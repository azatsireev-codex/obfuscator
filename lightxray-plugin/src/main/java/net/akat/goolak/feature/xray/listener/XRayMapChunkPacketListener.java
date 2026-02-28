package net.akat.goolak.feature.xray.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.akat.goolak.core.GOOLakPlugin;
import net.akat.goolak.core.concurrent.ServerTaskDispatcher;
import net.akat.goolak.feature.xray.service.XRayProtectionService;
import org.bukkit.entity.Player;

public final class XRayMapChunkPacketListener extends PacketAdapter {

  private final GOOLakPlugin plugin;
  private final ServerTaskDispatcher taskDispatcher;
  private final XRayProtectionService xRayProtectionService;

  public XRayMapChunkPacketListener(GOOLakPlugin plugin, ServerTaskDispatcher taskDispatcher,
      XRayProtectionService xRayProtectionService) {
    super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK);
    this.plugin = plugin;
    this.taskDispatcher = taskDispatcher;
    this.xRayProtectionService = xRayProtectionService;
  }

  public void register() {
    ProtocolLibrary.getProtocolManager().addPacketListener(this);
  }

  public void unregister() {
    ProtocolLibrary.getProtocolManager().removePacketListener(this);
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    if (!this.xRayProtectionService.config().enabled()) {
      return;
    }

    Player player = event.getPlayer();

    int chunkX = event.getPacket().getIntegers().read(0);
    int chunkZ = event.getPacket().getIntegers().read(1);

    if (event.getPacket().getByteArrays().size() > 0) {
      byte[] original = event.getPacket().getByteArrays().readSafely(0);
      if (original != null) {
        event.getPacket().getByteArrays().writeSafely(0, original.clone());
      }
    }

    this.taskDispatcher.executeAtChunk(player.getWorld(), chunkX, chunkZ, () -> {
      try {
        this.xRayProtectionService.handleChunkPacket(player, chunkX, chunkZ);
      } catch (Exception exception) {
        this.plugin.getLogger().warning("XRay processing failed for chunk " + chunkX + "," + chunkZ
            + " player=" + player.getName() + " reason=" + exception.getMessage());
      }
    });
  }
}
