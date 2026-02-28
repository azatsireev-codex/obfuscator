package net.akat.goolak.feature.xray.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.akat.goolak.core.model.BlockPosition;
import net.akat.goolak.feature.xray.config.XRayProtectionConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class XRayProtectionService {

  private final Map<UUID, Set<BlockPosition>> maskedByPlayer = new ConcurrentHashMap<>();

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

  public void handleChunkPacket(Player player, int chunkX, int chunkZ) {
    XRayProtectionConfig current = this.config;
    if (!current.enabled() || !player.isOnline() || player.isDead()) {
      return;
    }

    World world = player.getWorld();
    int worldMinY = world.getMinHeight();
    int worldMaxY = world.getMaxHeight() - 1;
    int minY = Math.max(worldMinY, current.minY());
    int maxY = Math.min(worldMaxY, current.maxY());

    int replaced = 0;
    int baseX = chunkX << 4;
    int baseZ = chunkZ << 4;

    Set<BlockPosition> playerMask = this.maskedByPlayer.computeIfAbsent(
        player.getUniqueId(), key -> ConcurrentHashMap.newKeySet());
    List<BlockPosition> newMasked = new ArrayList<>();

    for (int y = minY; y <= maxY && replaced < current.maxReplacementsPerChunk(); y++) {
      for (int local = 0; local < 16 && replaced < current.maxReplacementsPerChunk(); local++) {
        replaced += this.tryMask(player, world, baseX + local, y, baseZ, current, newMasked);
        replaced += this.tryMask(player, world, baseX + local, y, baseZ + 15, current, newMasked);
        if (local > 0 && local < 15) {
          replaced += this.tryMask(player, world, baseX, y, baseZ + local, current, newMasked);
          replaced += this.tryMask(player, world, baseX + 15, y, baseZ + local, current, newMasked);
        }
      }
    }

    playerMask.addAll(newMasked);
  }

  private int tryMask(Player player, World world, int x, int y, int z,
      XRayProtectionConfig current, List<BlockPosition> newMasked) {

    Block block = world.getBlockAt(x, y, z);
    Material material = block.getType();
    if (!current.hiddenMaterials().contains(material)) {
      return 0;
    }

    if (!isEnclosed(block)) {
      return 0;
    }

    if (isInsideViewCone(player, x + 0.5, y + 0.5, z + 0.5, current.viewConeDegrees())) {
      return 0;
    }

    player.sendBlockChange(new Location(world, x, y, z), current.replacementMaterial().createBlockData());
    newMasked.add(new BlockPosition(x, y, z));
    return 1;
  }

  public void clearPlayer(Player player) {
    Set<BlockPosition> oldMask = this.maskedByPlayer.remove(player.getUniqueId());
    if (oldMask == null || oldMask.isEmpty()) {
      return;
    }

    World world = player.getWorld();
    for (BlockPosition pos : oldMask) {
      player.sendBlockChange(new Location(world, pos.x(), pos.y(), pos.z()),
          world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData());
    }
  }

  private static boolean isEnclosed(Block block) {
    return isOccluding(block.getRelative(1, 0, 0))
        && isOccluding(block.getRelative(-1, 0, 0))
        && isOccluding(block.getRelative(0, 1, 0))
        && isOccluding(block.getRelative(0, -1, 0))
        && isOccluding(block.getRelative(0, 0, 1))
        && isOccluding(block.getRelative(0, 0, -1));
  }

  private static boolean isOccluding(Block block) {
    Material material = block.getType();
    return material.isOccluding() && material.isSolid();
  }

  private static boolean isInsideViewCone(Player player, double x, double y, double z, double viewConeDegrees) {
    Location eye = player.getEyeLocation();
    Vector toBlock = new Vector(x - eye.getX(), y - eye.getY(), z - eye.getZ());
    if (toBlock.lengthSquared() < 0.01) {
      return true;
    }

    Vector direction = eye.getDirection();
    double dot = direction.normalize().dot(toBlock.normalize());
    double cosHalfFov = Math.cos(Math.toRadians(viewConeDegrees / 2d));
    return dot >= cosHalfFov;
  }
}
