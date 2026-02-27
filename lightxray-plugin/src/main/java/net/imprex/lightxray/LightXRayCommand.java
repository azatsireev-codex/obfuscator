package net.imprex.lightxray;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

final class LightXRayCommand implements CommandExecutor {

  private final LightXRayPlugin plugin;

  LightXRayCommand(LightXRayPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
      this.plugin.reloadPluginConfig();
      sender.sendMessage("§aLightXRay config reloaded.");
      return true;
    }

    sender.sendMessage("§eUsage: /" + label + " reload");
    return true;
  }
}
