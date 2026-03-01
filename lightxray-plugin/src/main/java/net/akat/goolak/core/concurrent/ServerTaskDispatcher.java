package net.akat.goolak.core.concurrent;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerTaskDispatcher {

  private final JavaPlugin plugin;
  private final Method getRegionSchedulerMethod;
  private final Method regionSchedulerExecuteMethod;

  public ServerTaskDispatcher(JavaPlugin plugin) {
    this.plugin = plugin;

    Method regionSchedulerGetter = null;
    Method executeMethod = null;

    try {
      regionSchedulerGetter = Bukkit.getServer().getClass().getMethod("getRegionScheduler");
      Class<?> regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
      executeMethod = regionSchedulerClass.getMethod("execute",
          org.bukkit.plugin.Plugin.class,
          World.class,
          int.class,
          int.class,
          Runnable.class);
    } catch (Exception ignored) {
      regionSchedulerGetter = null;
      executeMethod = null;
    }

    this.getRegionSchedulerMethod = regionSchedulerGetter;
    this.regionSchedulerExecuteMethod = executeMethod;
  }

  public boolean isFoliaLikeSchedulerAvailable() {
    return this.getRegionSchedulerMethod != null && this.regionSchedulerExecuteMethod != null;
  }

  public void executeAtChunk(World world, int chunkX, int chunkZ, Runnable runnable) {
    if (this.isFoliaLikeSchedulerAvailable()) {
      try {
        Object regionScheduler = this.getRegionSchedulerMethod.invoke(Bukkit.getServer());
        this.regionSchedulerExecuteMethod.invoke(regionScheduler, this.plugin, world, chunkX, chunkZ, runnable);
        return;
      } catch (Exception ignored) {
        // fallback to bukkit scheduler
      }
    }

    Bukkit.getScheduler().runTask(this.plugin, runnable);
  }

  public void executeAtPlayerChunk(Player player, Runnable runnable) {
    int chunkX = player.getLocation().getBlockX() >> 4;
    int chunkZ = player.getLocation().getBlockZ() >> 4;
    this.executeAtChunk(player.getWorld(), chunkX, chunkZ, runnable);
  }
}
