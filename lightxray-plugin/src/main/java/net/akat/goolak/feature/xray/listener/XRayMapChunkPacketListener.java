package net.akat.goolak.feature.xray.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.akat.goolak.core.GOOLakPlugin;
import net.akat.goolak.core.concurrent.ServerTaskDispatcher;
import net.akat.goolak.feature.xray.service.XRayProtectionService;
import org.bukkit.entity.Player;

public final class XRayMapChunkPacketListener extends PacketAdapter {

  private static final long REWRITE_TIMEOUT_MS = 25L;

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

    PacketChunkDataAccessor chunkData = new PacketChunkDataAccessor(event.getPacket());
    byte[] original = chunkData.getData();
    if (original == null || original.length == 0) {
      return;
    }

    AtomicReference<byte[]> rewrittenRef = new AtomicReference<>(original);
    AtomicReference<Throwable> errorRef = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    this.taskDispatcher.executeAtChunk(player.getWorld(), chunkX, chunkZ, () -> {
      try {
        rewrittenRef.set(this.xRayProtectionService.rewriteChunkPacket(player, chunkX, chunkZ, original));
      } catch (Throwable throwable) {
        errorRef.set(throwable);
      } finally {
        latch.countDown();
      }
    });

    try {
      if (!latch.await(REWRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
        return;
      }
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      return;
    }

    Throwable throwable = errorRef.get();
    if (throwable != null) {
      this.plugin.getLogger().warning("XRay chunk rewrite failed for chunk " + chunkX + "," + chunkZ
          + " player=" + player.getName() + " reason=" + throwable.getMessage());
      return;
    }

    byte[] rewritten = rewrittenRef.get();
    if (rewritten != null && rewritten != original) {
      chunkData.setData(rewritten);
    }
  }
}
