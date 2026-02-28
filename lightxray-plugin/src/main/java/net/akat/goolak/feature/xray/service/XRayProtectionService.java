package net.akat.goolak.feature.xray.service;

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

    int baseX = chunkX << 4;
    int baseZ = chunkZ << 4;

    Set<BlockPosition> playerMask = this.maskedByPlayer.computeIfAbsent(
        player.getUniqueId(), key -> ConcurrentHashMap.newKeySet());

    for (int y = minY; y <= maxY; y++) {
      for (int localZ = 0; localZ < 16; localZ++) {
        for (int localX = 0; localX < 16; localX++) {
          if (current.boundaryOnly() && localX > 0 && localX < 15 && localZ > 0 && localZ < 15) {
            continue;
          }

          int x = baseX + localX;
          int z = baseZ + localZ;

          if (this.shouldMask(player, world, x, y, z, current)) {
            player.sendBlockChange(new Location(world, x, y, z), current.replacementMaterial().createBlockData());
            playerMask.add(new BlockPosition(x, y, z));
          }
        }
      }
    }
  }

  private boolean shouldMask(Player player, World world, int x, int y, int z, XRayProtectionConfig current) {
    Block block = world.getBlockAt(x, y, z);
    Material material = block.getType();
    if (!current.hiddenMaterials().contains(material)) {
      return false;
    }

    if (current.requireEnclosed() && !isEnclosed(block)) {
      return false;
    }

    if (current.checkViewCone() && isInsideViewCone(player, x + 0.5, y + 0.5, z + 0.5, current.viewConeDegrees())) {
      return false;
    }

    return true;
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
