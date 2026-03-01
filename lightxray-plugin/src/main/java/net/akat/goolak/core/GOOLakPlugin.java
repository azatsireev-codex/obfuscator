package net.akat.goolak.core;

import java.util.Objects;
import net.akat.goolak.core.command.GOOLakAdminCommand;
import net.akat.goolak.core.concurrent.ServerTaskDispatcher;
import net.akat.goolak.feature.xray.config.XRayProtectionConfig;
import net.akat.goolak.feature.xray.listener.XRayMapChunkPacketListener;
import net.akat.goolak.feature.xray.service.XRayProtectionService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class GOOLakPlugin extends JavaPlugin {

  private ServerTaskDispatcher taskDispatcher;
  private XRayProtectionService xRayProtectionService;
  private XRayMapChunkPacketListener xRayPacketListener;

  @Override
  public void onEnable() {
    Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
    if (protocolLib == null || !protocolLib.isEnabled()) {
      this.getLogger().severe("ProtocolLib is required for GOOLak XRay protection. Disabling GOOLak.");
      Bukkit.getPluginManager().disablePlugin(this);
      return;
    }

    this.saveDefaultConfig();

    this.taskDispatcher = new ServerTaskDispatcher(this);

    XRayProtectionConfig xRayConfig = XRayProtectionConfig.from(this.getConfig());
    this.xRayProtectionService = new XRayProtectionService(xRayConfig);

    this.xRayPacketListener = new XRayMapChunkPacketListener(this, this.taskDispatcher, this.xRayProtectionService);
    this.xRayPacketListener.register();

    PluginCommand command = Objects.requireNonNull(this.getCommand("goolak"), "Missing command goolak");
    command.setExecutor(new GOOLakAdminCommand(this));

    this.getLogger().info("GOOLak enabled. XRay MAP_CHUNK payload rewrite is active. Folia scheduler mode="
        + this.taskDispatcher.isFoliaLikeSchedulerAvailable());
  }

  @Override
  public void onDisable() {
    if (this.xRayPacketListener != null) {
      this.xRayPacketListener.unregister();
      this.xRayPacketListener = null;
    }

    this.xRayProtectionService = null;
    this.taskDispatcher = null;
  }

  public void reloadGOOLakConfig() {
    this.reloadConfig();
    XRayProtectionConfig xRayConfig = XRayProtectionConfig.from(this.getConfig());
    this.xRayProtectionService.updateConfig(xRayConfig);
  }
}
