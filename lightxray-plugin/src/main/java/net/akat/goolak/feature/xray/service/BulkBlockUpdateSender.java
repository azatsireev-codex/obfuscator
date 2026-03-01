package net.akat.goolak.feature.xray.service;

import java.lang.reflect.Method;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

final class BulkBlockUpdateSender {

  private final Method sendMultiBlockChangeMethod;

  BulkBlockUpdateSender() {
    Method method;
    try {
      method = Player.class.getMethod("sendMultiBlockChange", Map.class);
    } catch (NoSuchMethodException exception) {
      method = null;
    }
    this.sendMultiBlockChangeMethod = method;
  }

  void send(Player player, Map<Location, BlockData> blockUpdates) {
    if (blockUpdates.isEmpty()) {
      return;
    }

    if (this.sendMultiBlockChangeMethod != null) {
      try {
        this.sendMultiBlockChangeMethod.invoke(player, blockUpdates);
        return;
      } catch (Exception ignored) {
        // fallback below
      }
    }

    for (Map.Entry<Location, BlockData> entry : blockUpdates.entrySet()) {
      player.sendBlockChange(entry.getKey(), entry.getValue());
    }
  }
}
