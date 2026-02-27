package net.imprex.lightxray;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

final class LightXRayListener implements Listener {

  private final LightXRayService service;

  LightXRayListener(LightXRayService service) {
    this.service = service;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.service.clearPlayer(event.getPlayer());
  }
}
