package net.akat.goolak.feature.xray.listener;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;

final class PacketChunkDataAccessor {

  private static final Class<?> CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA = MinecraftReflection.getMinecraftClass(
      "network.protocol.game.ClientboundLevelChunkPacketData");
  private static final FieldAccessor BUFFER = Accessors.getFieldAccessor(CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA,
      byte[].class, true);

  private final PacketContainer packet;
  private final Object packetDataHandle;

  PacketChunkDataAccessor(PacketContainer packet) {
    this.packet = packet;

    Object handle;
    try {
      handle = packet.getSpecificModifier(CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA).read(0);
    } catch (Throwable ignored) {
      handle = null;
    }
    this.packetDataHandle = handle;
  }

  byte[] getData() {
    if (this.packetDataHandle != null) {
      return (byte[]) BUFFER.get(this.packetDataHandle);
    }

    if (this.packet.getByteArrays().size() > 0) {
      return this.packet.getByteArrays().readSafely(0);
    }

    return null;
  }

  void setData(byte[] data) {
    if (this.packetDataHandle != null) {
      BUFFER.set(this.packetDataHandle, data);
      return;
    }

    if (this.packet.getByteArrays().size() > 0) {
      this.packet.getByteArrays().writeSafely(0, data);
    }
  }
}
