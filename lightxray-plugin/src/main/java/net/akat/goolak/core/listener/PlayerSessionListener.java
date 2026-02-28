package net.akat.goolak.core.listener;

import net.akat.goolak.feature.xray.service.XRayProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerSessionListener implements Listener {

  private final XRayProtectionService xRayProtectionService;

  public PlayerSessionListener(XRayProtectionService xRayProtectionService) {
    this.xRayProtectionService = xRayProtectionService;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.xRayProtectionService.clearPlayer(event.getPlayer());
  }
}
