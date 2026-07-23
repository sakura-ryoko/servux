package fi.dy.masa.servux.paper.network;

import java.util.HashSet;
import java.util.Set;
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
import org.bukkit.scheduler.BukkitRunnable;
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
 * {@code MixinServerChunkLoadingManager} mixin), and mirrors Fabric's proactive player-join
 * registration so the structures handshake completes immediately on login instead of only after
 * a dimension change. No PacketEvents/Netty interception is needed for this channel.
 */
public class StructuresChannel implements PluginMessageListener, Listener
{
    public static final String CHANNEL = StructureDataProvider.CHANNEL_ID;
    private static final String PERMISSION = "servux.structures";
    // Delay the join handshake slightly: the client may not have finished registering
    // plugin channels when PlayerJoinEvent fires, so an immediate send can be dropped.
    private static final long JOIN_HANDSHAKE_DELAY_TICKS = 5L;
    // Retry a few times if the client hasn't sent its own register packet yet.
    private static final long JOIN_HANDSHAKE_PERIOD_TICKS = 10L;
    private static final int JOIN_HANDSHAKE_REPEATS = 3;

    private final ServuxPaperPlugin plugin;
    private BukkitTask tickTask;
    private final Set<UUID> handshakeInProgress = new HashSet<>();

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

        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this.plugin, CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this.plugin, CHANNEL, this);
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event)
    {
        StructureDataProvider.INSTANCE.onStartedWatchingChunk(event.getPlayer(), event.getChunk());
    }

    /**
     * Mirrors Fabric's {@code PlayerListener.onPlayerJoin()} behaviour: send the structures
     * metadata handshake when a player joins. On Paper the client may not have finished its
     * plugin-channel registration handshake when PlayerJoinEvent fires, so the send is delayed
     * and retried a few times unless the client registers the channel or sends its own register
     * packet first.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        if (!player.hasPermission(PERMISSION))
        {
            return;
        }

        UUID uuid = player.getUniqueId();
        this.handshakeInProgress.add(uuid);

        new BukkitRunnable()
        {
            private int count = 0;

            @Override
            public void run()
            {
                if (!player.isOnline())
                {
                    StructuresChannel.this.handshakeInProgress.remove(uuid);
                    this.cancel();
                    return;
                }

                if (!StructuresChannel.this.handshakeInProgress.contains(uuid))
                {
                    this.cancel();
                    return;
                }

                StructuresChannel.this.sendHandshake(player);
                this.count++;

                if (this.count >= JOIN_HANDSHAKE_REPEATS)
                {
                    StructuresChannel.this.handshakeInProgress.remove(uuid);
                    this.cancel();
                }
            }
        }.runTaskTimer(this.plugin, JOIN_HANDSHAKE_DELAY_TICKS, JOIN_HANDSHAKE_PERIOD_TICKS);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        this.handshakeInProgress.remove(event.getPlayer().getUniqueId());
        StructureDataProvider.INSTANCE.unregister(event.getPlayer());
    }

    /**
     * Send the metadata handshake as soon as the client tells the server it can receive on the
     * structures channel. This is the most reliable trigger because it guarantees the channel is
     * registered on the client side (Bukkit plugin messages are ignored for unregistered channels).
     */
    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event)
    {
        if (!CHANNEL.equals(event.getChannel()))
        {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission(PERMISSION))
        {
            return;
        }

        ServuxPaperReference.debugLog("structures: client registered channel for player {}", player.getName());
        this.sendHandshake(player);
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
                this.handshakeInProgress.remove(player.getUniqueId());
                StructureDataProvider.INSTANCE.registerFresh(player);
            }
            case PACKET_C2S_STRUCTURES_UNREGISTER ->
            {
                this.handshakeInProgress.remove(player.getUniqueId());
                StructureDataProvider.INSTANCE.unregister(player);
            }
            default -> ServuxPaperReference.logger().warn("StructuresChannel#onPluginMessageReceived: unexpected packet type '{}' from player {}", packet.getType(), player.getName());
        }
    }

    private void sendHandshake(Player player)
    {
        if (StructureDataProvider.INSTANCE.isRegistered(player))
        {
            return;
        }

        ServuxPaperReference.debugLog("structures: sending metadata handshake to player {}", player.getName());
        StructureDataProvider.INSTANCE.registerFresh(player);
    }
}
