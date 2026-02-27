package net.imprex.lightxray;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LightXRayPlugin extends JavaPlugin {

  private LightXRayService service;

  @Override
  public void onEnable() {
    this.saveDefaultConfig();

    LightXRayConfig config = LightXRayConfig.from(this.getConfig());
    this.service = new LightXRayService(this, config);
    this.service.start();

    PluginCommand command = Objects.requireNonNull(this.getCommand("lightxray"), "Missing command lightxray");
    command.setExecutor(new LightXRayCommand(this));

    this.getServer().getPluginManager().registerEvents(new LightXRayListener(this.service), this);

    this.getLogger().info("LightXRay enabled. Boundary-only masking is active.");
  }

  @Override
  public void onDisable() {
    if (this.service != null) {
      this.service.stop();
      this.service = null;
    }
  }

  void reloadPluginConfig() {
    this.reloadConfig();
    LightXRayConfig config = LightXRayConfig.from(this.getConfig());
    this.service.updateConfig(config);
  }
}
