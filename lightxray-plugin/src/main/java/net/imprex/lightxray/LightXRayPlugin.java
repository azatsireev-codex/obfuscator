package net.imprex.lightxray;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class LightXRayPlugin extends JavaPlugin {

  private LightXRayService service;
  private LightXRayPacketListener packetListener;

  @Override
  public void onEnable() {
    Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
    if (protocolLib == null || !protocolLib.isEnabled()) {
      this.getLogger().severe("ProtocolLib is required for packet-level obfuscation. Disabling LightXRay.");
      Bukkit.getPluginManager().disablePlugin(this);
      return;
    }

    this.saveDefaultConfig();

    LightXRayConfig config = LightXRayConfig.from(this.getConfig());
    this.service = new LightXRayService(config);
    this.packetListener = new LightXRayPacketListener(this, this.service);
    this.packetListener.register();

    PluginCommand command = Objects.requireNonNull(this.getCommand("lightxray"), "Missing command lightxray");
    command.setExecutor(new LightXRayCommand(this));

    this.getServer().getPluginManager().registerEvents(new LightXRayListener(this.service), this);

    this.getLogger().info("LightXRay enabled. MAP_CHUNK interception is active.");
  }

  @Override
  public void onDisable() {
    if (this.packetListener != null) {
      this.packetListener.unregister();
      this.packetListener = null;
    }

    if (this.service != null) {
      this.service = null;
    }
  }

  void reloadPluginConfig() {
    this.reloadConfig();
    LightXRayConfig config = LightXRayConfig.from(this.getConfig());
    this.service.updateConfig(config);
  }
}
