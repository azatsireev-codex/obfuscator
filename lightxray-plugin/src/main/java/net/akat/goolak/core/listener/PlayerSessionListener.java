package net.akat.goolak.core.listener;

import net.akat.goolak.core.concurrent.ServerTaskDispatcher;
import net.akat.goolak.feature.xray.service.XRayProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerSessionListener implements Listener {

  private final ServerTaskDispatcher taskDispatcher;
  private final XRayProtectionService xRayProtectionService;

  public PlayerSessionListener(ServerTaskDispatcher taskDispatcher, XRayProtectionService xRayProtectionService) {
    this.taskDispatcher = taskDispatcher;
    this.xRayProtectionService = xRayProtectionService;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.taskDispatcher.executeAtPlayerChunk(event.getPlayer(),
        () -> this.xRayProtectionService.clearPlayer(event.getPlayer()));
  }
}
