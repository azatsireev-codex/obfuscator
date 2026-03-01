package net.akat.goolak.feature.xray.service;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import net.akat.goolak.feature.xray.config.XRayProtectionConfig;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class XRayProtectionService {

  private volatile XRayProtectionConfig config;

  public XRayProtectionService(XRayProtectionConfig config) {
    this.config = config;
  }

  public void updateConfig(XRayProtectionConfig config) {
    this.config = config;
  }

  public XRayProtectionConfig config() {
    return this.config;
  }

  public byte[] rewriteChunkPacket(Player player, int chunkX, int chunkZ, byte[] payload) {
    XRayProtectionConfig current = this.config;
    if (!current.enabled() || payload == null || payload.length == 0 || !player.isOnline() || player.isDead()) {
      return payload;
    }

    World world = player.getWorld();
    ChunkBufferReader reader = new ChunkBufferReader(payload);
    ByteArrayOutputStream output = new ByteArrayOutputStream(payload.length);

    boolean changed = false;
    int minY = Math.max(world.getMinHeight(), current.minY());
    int maxY = Math.min(world.getMaxHeight() - 1, current.maxY());

    int sectionIndex = 0;
    while (reader.remaining() > 0) {
      int sectionStart = reader.position();
      SectionData section = SectionData.read(reader);
      if (section == null) {
        return payload;
      }

      int sectionEnd = reader.position();
      int sectionMinY = world.getMinHeight() + (sectionIndex << 4);
      int sectionMaxY = sectionMinY + 15;
      sectionIndex++;

      boolean sectionChanged = false;
      boolean insideY = sectionMaxY >= minY && sectionMinY <= maxY;
      if (insideY) {
        int baseX = chunkX << 4;
        int baseY = sectionMinY;
        int baseZ = chunkZ << 4;

        int replacementStateId = section.findStateIdForMaterial(world, baseX, baseY, baseZ, current.replacementMaterial());
        if (replacementStateId != Integer.MIN_VALUE) {
          Set<Integer> hiddenStateIds = section.findStateIdsForMaterials(world, baseX, baseY, baseZ,
              current.hiddenMaterials(), current.boundaryOnly());
          if (!hiddenStateIds.isEmpty()) {
            sectionChanged = section.replaceStates(hiddenStateIds, replacementStateId);
            changed |= sectionChanged;
          }
        }
      }

      if (sectionChanged) {
        section.write(output);
      } else {
        output.write(payload, sectionStart, sectionEnd - sectionStart);
      }
    }

    return changed ? output.toByteArray() : payload;
  }

  private static final class SectionData {

    private static final int BLOCK_COUNT = 4096;
    private static final int BIOME_COUNT = 64;
    private static final int MAX_BLOCK_ARRAY_LENGTH = 4096;
    private static final int MAX_BIOME_ARRAY_LENGTH = 256;

    private final int blockCount;
    private final int bitsPerBlock;
    private final int[] palette;
    private final long[] blockStates;

    private final byte biomeBitsPerValue;
    private final int[] biomePalette;
    private final long[] biomeData;

    private SectionData(int blockCount, int bitsPerBlock, int[] palette, long[] blockStates,
        byte biomeBitsPerValue, int[] biomePalette, long[] biomeData) {
      this.blockCount = blockCount;
      this.bitsPerBlock = bitsPerBlock;
      this.palette = palette;
      this.blockStates = blockStates;
      this.biomeBitsPerValue = biomeBitsPerValue;
      this.biomePalette = biomePalette;
      this.biomeData = biomeData;
    }

    static SectionData read(ChunkBufferReader reader) {
      if (!reader.canRead(3)) {
        return null;
      }

      int blockCount = reader.readUnsignedShort();
      int bitsPerBlock = reader.readUnsignedByte();
      if (bitsPerBlock < 0 || bitsPerBlock > 15) {
        return null;
      }

      int[] palette;
      if (bitsPerBlock == 0) {
        Integer single = reader.readVarIntSafe();
        if (single == null) {
          return null;
        }
        palette = new int[] {single};
      } else if (bitsPerBlock <= 8) {
        Integer size = reader.readVarIntSafe();
        if (size == null || size < 1 || size > 1 << bitsPerBlock) {
          return null;
        }
        palette = new int[size];
        for (int i = 0; i < size; i++) {
          Integer value = reader.readVarIntSafe();
          if (value == null) {
            return null;
          }
          palette[i] = value;
        }
      } else {
        palette = null;
      }

      Integer blockArrayLength = reader.readVarIntSafe();
      if (blockArrayLength == null || blockArrayLength < 0 || blockArrayLength > MAX_BLOCK_ARRAY_LENGTH) {
        return null;
      }
      if (!reader.canRead(blockArrayLength * Long.BYTES)) {
        return null;
      }
      long[] blockStates = reader.readLongArray(blockArrayLength);

      if (!reader.canRead(1)) {
        return null;
      }
      byte biomeBitsPerValue = (byte) reader.readUnsignedByte();
      if (biomeBitsPerValue < 0 || biomeBitsPerValue > 6) {
        return null;
      }

      int[] biomePalette;
      if (biomeBitsPerValue == 0) {
        Integer singleBiome = reader.readVarIntSafe();
        if (singleBiome == null) {
          return null;
        }
        biomePalette = new int[] {singleBiome};
      } else if (biomeBitsPerValue <= 3) {
        Integer size = reader.readVarIntSafe();
        if (size == null || size < 1 || size > 1 << biomeBitsPerValue) {
          return null;
        }
        biomePalette = new int[size];
        for (int i = 0; i < size; i++) {
          Integer value = reader.readVarIntSafe();
          if (value == null) {
            return null;
          }
          biomePalette[i] = value;
        }
      } else {
        biomePalette = null;
      }

      Integer biomeArrayLength = reader.readVarIntSafe();
      if (biomeArrayLength == null || biomeArrayLength < 0 || biomeArrayLength > MAX_BIOME_ARRAY_LENGTH) {
        return null;
      }
      if (!reader.canRead(biomeArrayLength * Long.BYTES)) {
        return null;
      }
      long[] biomeData = reader.readLongArray(biomeArrayLength);

      return new SectionData(blockCount, bitsPerBlock, palette, blockStates, biomeBitsPerValue, biomePalette, biomeData);
    }

    int findStateIdForMaterial(World world, int baseX, int baseY, int baseZ, Material target) {
      for (int index = 0; index < BLOCK_COUNT; index++) {
        int localX = index & 15;
        int localY = (index >> 8) & 15;
        int localZ = (index >> 4) & 15;
        if (world.getBlockAt(baseX + localX, baseY + localY, baseZ + localZ).getType() == target) {
          return this.getStateId(index);
        }
      }
      return Integer.MIN_VALUE;
    }

    Set<Integer> findStateIdsForMaterials(World world, int baseX, int baseY, int baseZ,
        Set<Material> hiddenMaterials, boolean boundaryOnly) {
      Set<Integer> hiddenStateIds = new HashSet<>();
      for (int index = 0; index < BLOCK_COUNT; index++) {
        int localX = index & 15;
        int localY = (index >> 8) & 15;
        int localZ = (index >> 4) & 15;

        if (boundaryOnly && localX > 0 && localX < 15 && localZ > 0 && localZ < 15) {
          continue;
        }

        Material material = world.getBlockAt(baseX + localX, baseY + localY, baseZ + localZ).getType();
        if (hiddenMaterials.contains(material)) {
          hiddenStateIds.add(this.getStateId(index));
        }
      }
      return hiddenStateIds;
    }

    boolean replaceStates(Set<Integer> hiddenStateIds, int replacementStateId) {
      boolean changed = false;
      if (this.palette != null) {
        for (int i = 0; i < this.palette.length; i++) {
          if (hiddenStateIds.contains(this.palette[i])) {
            this.palette[i] = replacementStateId;
            changed = true;
          }
        }
      } else {
        for (int index = 0; index < BLOCK_COUNT; index++) {
          int value = getValue(this.blockStates, this.bitsPerBlock, index);
          if (hiddenStateIds.contains(value)) {
            setValue(this.blockStates, this.bitsPerBlock, index, replacementStateId);
            changed = true;
          }
        }
      }
      return changed;
    }

    int getStateId(int blockIndex) {
      if (this.palette != null) {
        int paletteIndex = this.bitsPerBlock == 0 ? 0 : getValue(this.blockStates, this.bitsPerBlock, blockIndex);
        if (paletteIndex < 0 || paletteIndex >= this.palette.length) {
          return 0;
        }
        return this.palette[paletteIndex];
      }

      return getValue(this.blockStates, this.bitsPerBlock, blockIndex);
    }

    void write(ByteArrayOutputStream output) {
      output.write((this.blockCount >>> 8) & 0xFF);
      output.write(this.blockCount & 0xFF);

      output.write(this.bitsPerBlock & 0xFF);
      if (this.palette != null) {
        if (this.bitsPerBlock == 0) {
          writeVarInt(output, this.palette[0]);
        } else {
          writeVarInt(output, this.palette.length);
          for (int value : this.palette) {
            writeVarInt(output, value);
          }
        }
      }

      writeVarInt(output, this.blockStates.length);
      writeLongArray(output, this.blockStates);

      output.write(this.biomeBitsPerValue & 0xFF);
      if (this.biomePalette != null) {
        if (this.biomeBitsPerValue == 0) {
          writeVarInt(output, this.biomePalette[0]);
        } else {
          writeVarInt(output, this.biomePalette.length);
          for (int value : this.biomePalette) {
            writeVarInt(output, value);
          }
        }
      }

      writeVarInt(output, this.biomeData.length);
      writeLongArray(output, this.biomeData);
    }

    private static int getValue(long[] data, int bits, int index) {
      if (bits == 0 || data.length == 0) {
        return 0;
      }
      int bitIndex = index * bits;
      int longIndex = bitIndex >>> 6;
      if (longIndex >= data.length) {
        return 0;
      }
      int startBit = bitIndex & 63;
      long mask = (1L << bits) - 1L;
      long value = (data[longIndex] >>> startBit) & mask;
      int endBit = startBit + bits;
      if (endBit > 64 && longIndex + 1 < data.length) {
        int spill = endBit - 64;
        value |= (data[longIndex + 1] & ((1L << spill) - 1L)) << (bits - spill);
      }
      return (int) value;
    }

    private static void setValue(long[] data, int bits, int index, int value) {
      if (bits == 0 || data.length == 0) {
        return;
      }
      int bitIndex = index * bits;
      int longIndex = bitIndex >>> 6;
      if (longIndex >= data.length) {
        return;
      }
      int startBit = bitIndex & 63;
      long mask = ((1L << bits) - 1L) << startBit;
      data[longIndex] = (data[longIndex] & ~mask) | (((long) value << startBit) & mask);

      int endBit = startBit + bits;
      if (endBit > 64 && longIndex + 1 < data.length) {
        int spill = endBit - 64;
        long spillMask = (1L << spill) - 1L;
        data[longIndex + 1] = (data[longIndex + 1] & ~spillMask) | (((long) value >>> (bits - spill)) & spillMask);
      }
    }

    private static void writeVarInt(ByteArrayOutputStream output, int value) {
      int current = value;
      while ((current & -128) != 0) {
        output.write((current & 127) | 128);
        current >>>= 7;
      }
      output.write(current);
    }

    private static void writeLongArray(ByteArrayOutputStream output, long[] values) {
      for (long value : values) {
        output.write((int) ((value >>> 56) & 0xFF));
        output.write((int) ((value >>> 48) & 0xFF));
        output.write((int) ((value >>> 40) & 0xFF));
        output.write((int) ((value >>> 32) & 0xFF));
        output.write((int) ((value >>> 24) & 0xFF));
        output.write((int) ((value >>> 16) & 0xFF));
        output.write((int) ((value >>> 8) & 0xFF));
        output.write((int) (value & 0xFF));
      }
    }
  }

  private static final class ChunkBufferReader {

    private final byte[] payload;
    private int position;

    ChunkBufferReader(byte[] payload) {
      this.payload = payload;
    }

    int position() {
      return this.position;
    }

    int remaining() {
      return this.payload.length - this.position;
    }

    boolean canRead(int bytes) {
      return bytes >= 0 && this.position + bytes <= this.payload.length;
    }

    int readUnsignedByte() {
      return this.payload[this.position++] & 0xFF;
    }

    int readUnsignedShort() {
      int high = readUnsignedByte();
      int low = readUnsignedByte();
      return (high << 8) | low;
    }

    Integer readVarIntSafe() {
      int value = 0;
      int shift = 0;
      for (int i = 0; i < 5; i++) {
        if (!canRead(1)) {
          return null;
        }
        int current = readUnsignedByte();
        value |= (current & 0x7F) << shift;
        if ((current & 0x80) == 0) {
          return value;
        }
        shift += 7;
      }
      return null;
    }

    long[] readLongArray(int length) {
      long[] data = new long[length];
      for (int i = 0; i < length; i++) {
        long value = 0;
        value |= ((long) readUnsignedByte()) << 56;
        value |= ((long) readUnsignedByte()) << 48;
        value |= ((long) readUnsignedByte()) << 40;
        value |= ((long) readUnsignedByte()) << 32;
        value |= ((long) readUnsignedByte()) << 24;
        value |= ((long) readUnsignedByte()) << 16;
        value |= ((long) readUnsignedByte()) << 8;
        value |= readUnsignedByte();
        data[i] = value;
      }
      return data;
    }
  }
}
