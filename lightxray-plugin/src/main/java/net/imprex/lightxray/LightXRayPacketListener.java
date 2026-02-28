package net.imprex.lightxray;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;

final class LightXRayPacketListener extends PacketAdapter {

  private final LightXRayService service;

  LightXRayPacketListener(LightXRayPlugin plugin, LightXRayService service) {
    super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK);
    this.service = service;
  }

  void register() {
    ProtocolLibrary.getProtocolManager().addPacketListener(this);
  }

  void unregister() {
    ProtocolLibrary.getProtocolManager().removePacketListener(this);
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    if (!this.service.config().enabled()) {
      return;
    }

    Player player = event.getPlayer();

    int chunkX = event.getPacket().getIntegers().read(0);
    int chunkZ = event.getPacket().getIntegers().read(1);

    // lightweight rewrite marker: replace data array with cloned content when available
    if (event.getPacket().getByteArrays().size() > 0) {
      byte[] original = event.getPacket().getByteArrays().readSafely(0);
      if (original != null) {
        event.getPacket().getByteArrays().writeSafely(0, original.clone());
      }
    }

    this.service.handleChunkPacket(player, chunkX, chunkZ);
  }
}
