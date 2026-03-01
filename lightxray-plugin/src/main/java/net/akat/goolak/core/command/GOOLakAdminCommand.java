package net.akat.goolak.core.command;

import net.akat.goolak.core.GOOLakPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class GOOLakAdminCommand implements CommandExecutor {

  private final GOOLakPlugin plugin;

  public GOOLakAdminCommand(GOOLakPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
      this.plugin.reloadGOOLakConfig();
      sender.sendMessage("§aGOOLak config reloaded.");
      return true;
    }

    sender.sendMessage("§eUsage: /" + label + " reload");
    return true;
  }
}
