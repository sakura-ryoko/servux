package fi.dy.masa.servux.paper.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

import fi.dy.masa.servux.paper.ServuxPaperConfig;
import fi.dy.masa.servux.paper.ServuxPaperPlugin;
import fi.dy.masa.servux.paper.ServuxPaperReference;
import fi.dy.masa.servux.paper.provider.StructureDataProvider;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;

/**
 * Registers and handles the {@code servux:structures} plugin channel via the plain Bukkit
 * {@link org.bukkit.plugin.messaging.Messenger} API, and drives the chunk-watch trigger via
 * Paper's {@link PlayerChunkLoadEvent} - the public-API replacement for Fabric's
 * {@code MixinServerChunkLoadingManager} mixin. No PacketEvents/Netty interception is needed for
 * this channel.
 */
public class StructuresChannel implements PluginMessageListener, Listener
{
    public static final String CHANNEL = StructureDataProvider.CHANNEL_ID;
    private static final String PERMISSION = "servux.structures";

    private final ServuxPaperPlugin plugin;
    private BukkitTask tickTask;

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
    }

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event)
    {
        StructureDataProvider.INSTANCE.onStartedWatchingChunk(event.getPlayer(), event.getChunk());
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
            return;
        }

        if (!player.hasPermission(PERMISSION))
        {
            ServuxPaperReference.debugLog("structures: denying access for player {}, insufficient permissions", player.getName());
            return;
        }

        switch (packet.getType())
        {
            case PACKET_C2S_STRUCTURES_REGISTER -> StructureDataProvider.INSTANCE.registerFresh(player);
            case PACKET_C2S_STRUCTURES_UNREGISTER -> StructureDataProvider.INSTANCE.unregister(player);
            default -> ServuxPaperReference.logger().warn("StructuresChannel#onPluginMessageReceived: unexpected packet type '{}' from player {}", packet.getType(), player.getName());
        }
    }
}
