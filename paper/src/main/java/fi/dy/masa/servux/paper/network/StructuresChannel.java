package fi.dy.masa.servux.paper.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

import fi.dy.masa.servux.paper.ServuxPaperConfig;
import fi.dy.masa.servux.paper.ServuxPaperPlugin;
import fi.dy.masa.servux.paper.ServuxPaperReference;
import fi.dy.masa.servux.paper.provider.StructureDataProvider;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;

/**
 * Registers and handles the {@code servux:structures} plugin channel via the plain Bukkit
 * {@link org.bukkit.plugin.messaging.Messenger} API, drives the chunk-watch trigger via
 * Paper's {@link PlayerChunkLoadEvent} (the public-API replacement for Fabric's
 * {@code MixinServerChunkLoadingManager} mixin), and handles the structures handshake.
 * <p>
 * The handshake is request/response: MiniHUD sends {@code PACKET_C2S_STRUCTURES_REGISTER} when it
 * is ready, and the server replies with metadata after a short delay. Proactive sends on login are
 * avoided because the client's payload receiver may not be fully initialized yet, which causes the
 * metadata to be dropped and the server to incorrectly think the handshake completed.
 */
public class StructuresChannel implements PluginMessageListener, Listener
{
    public static final String CHANNEL = StructureDataProvider.CHANNEL_ID;
    private static final String PERMISSION = "servux.structures";
    // Give the client a few ticks to finish registering its payload receiver before sending metadata.
    private static final long HANDSHAKE_DELAY_TICKS = 8L;

    // Throttle metadata replies so two triggers in quick succession (channel registration +
    // register packet) do not spam the client, while still responding to each of MiniHUD's
    // once-per-second retries if the handshake has not yet succeeded.
    private static final long HANDSHAKE_THROTTLE_TICKS = 15L;

    private final ServuxPaperPlugin plugin;
    private BukkitTask tickTask;
    private final Map<UUID, BukkitTask> pendingHandshakes = new HashMap<>();
    private final Map<UUID, Long> lastHandshakeTick = new HashMap<>();

    public StructuresChannel(ServuxPaperPlugin plugin)
    {
        this.plugin = plugin;
    }

    public void register()
    {
        StructureDataProvider.INSTANCE.init(this.plugin);

        Bukkit.getMessenger().registerOutgoingPluginChannel(this.plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this.plugin, CHANNEL, this);
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        // NOTE: the interval is read once at (re)registration time - changing `structures.update_interval`
        // in config.yml requires a plugin/server restart to take effect for this scheduled task.
        long interval = ServuxPaperConfig.structuresUpdateInterval();
        this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, StructureDataProvider.INSTANCE::tick, interval, interval);
    }

    public void unregister()
    {
        if (this.tickTask != null)
        {
            this.tickTask.cancel();
            this.tickTask = null;
        }

        for (BukkitTask task : this.pendingHandshakes.values())
        {
            task.cancel();
        }
        this.pendingHandshakes.clear();
        this.lastHandshakeTick.clear();

        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this.plugin, CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this.plugin, CHANNEL, this);
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event)
    {
        StructureDataProvider.INSTANCE.onStartedWatchingChunk(event.getPlayer(), event.getChunk());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        ServuxPaperReference.debugLog("structures: player {} joined", event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        this.cancelHandshake(uuid);
        this.lastHandshakeTick.remove(uuid);
        StructureDataProvider.INSTANCE.unregister(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event)
    {
        if (!CHANNEL.equals(event.getChannel()))
        {
            return;
        }

        ServuxPaperReference.debugLog("structures: client registered channel for player {}", event.getPlayer().getName());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message)
    {
        if (!channel.equals(CHANNEL))
        {
            return;
        }

        ServuxStructuresPacket packet = ServuxStructuresPacket.fromBytes(message);

        if (packet == null)
        {
            ServuxPaperReference.debugLog("structures: received null/invalid packet from player {}", player.getName());
            return;
        }

        if (!player.hasPermission(PERMISSION))
        {
            ServuxPaperReference.debugLog("structures: denying access for player {}, insufficient permissions", player.getName());
            return;
        }

        ServuxPaperReference.debugLog("structures: received packet type '{}' from player {}", packet.getType(), player.getName());

        switch (packet.getType())
        {
            case PACKET_C2S_STRUCTURES_REGISTER ->
            {
                // Always reply to the client's register request. Relying on server-side state is
                // not enough: an earlier proactive metadata send may have been dropped before the
                // client payload receiver was ready, leaving the client still retrying.
                this.scheduleHandshake(player);
            }
            case PACKET_C2S_STRUCTURES_UNREGISTER ->
            {
                UUID uuid = player.getUniqueId();
                this.cancelHandshake(uuid);
                this.lastHandshakeTick.remove(uuid);
                StructureDataProvider.INSTANCE.unregister(player);
            }
            default -> ServuxPaperReference.logger().warn("StructuresChannel#onPluginMessageReceived: unexpected packet type '{}' from player {}", packet.getType(), player.getName());
        }
    }

    private void scheduleHandshake(Player player)
    {
        if (!player.isOnline())
        {
            return;
        }

        UUID uuid = player.getUniqueId();
        long now = StructureDataProvider.currentTick();
        Long last = this.lastHandshakeTick.get(uuid);

        if (last != null && now - last < HANDSHAKE_THROTTLE_TICKS)
        {
            ServuxPaperReference.debugLog("structures: throttling handshake for player {}", player.getName());
            return;
        }

        this.cancelHandshake(uuid);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(this.plugin, () ->
        {
            this.pendingHandshakes.remove(uuid);

            if (!player.isOnline())
            {
                return;
            }

            ServuxPaperReference.debugLog("structures: sending metadata handshake to player {}", player.getName());
            StructureDataProvider.INSTANCE.registerFresh(player);
        }, HANDSHAKE_DELAY_TICKS);

        this.pendingHandshakes.put(uuid, task);
        this.lastHandshakeTick.put(uuid, now);
    }

    private void cancelHandshake(UUID uuid)
    {
        BukkitTask task = this.pendingHandshakes.remove(uuid);

        if (task != null)
        {
            task.cancel();
        }
    }
}
