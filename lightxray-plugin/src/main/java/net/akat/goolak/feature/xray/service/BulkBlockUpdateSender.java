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

  boolean isSupported() {
    return this.sendMultiBlockChangeMethod != null;
  }

  void send(Player player, Map<Location, BlockData> blockUpdates) {
    if (blockUpdates.isEmpty() || this.sendMultiBlockChangeMethod == null) {
      return;
    }

    try {
      this.sendMultiBlockChangeMethod.invoke(player, blockUpdates);
    } catch (Exception ignored) {
      // intentionally ignore; packet flow should not crash game thread
    }
  }
}
