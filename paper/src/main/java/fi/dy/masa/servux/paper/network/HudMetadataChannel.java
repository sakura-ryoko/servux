package fi.dy.masa.servux.paper.network;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

import fi.dy.masa.servux.paper.ServuxPaperConfig;
import fi.dy.masa.servux.paper.ServuxPaperPlugin;
import fi.dy.masa.servux.paper.ServuxPaperReference;
import fi.dy.masa.servux.paper.provider.HudDataProvider;
import net.minecraft.nbt.CompoundTag;

/**
 * Registers and handles the {@code servux:hud_metadata} plugin channel via the plain Bukkit
 * {@link org.bukkit.plugin.messaging.Messenger} API (no PacketEvents needed for this channel).
 * Also schedules the periodic weather/data-logger tick broadcast.
 */
public class HudMetadataChannel implements PluginMessageListener
{
    public static final String CHANNEL = HudDataProvider.CHANNEL_ID;
    private static final String PERMISSION = "servux.hud_data";

    private final ServuxPaperPlugin plugin;
    private BukkitTask tickTask;

    public HudMetadataChannel(ServuxPaperPlugin plugin)
    {
        this.plugin = plugin;
    }

    public void register()
    {
        HudDataProvider.INSTANCE.init(this.plugin);

        Bukkit.getMessenger().registerOutgoingPluginChannel(this.plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this.plugin, CHANNEL, this);

        // NOTE: the interval is read once at (re)registration time - changing `hud_data.update_interval`
        // in config.yml requires a plugin/server restart to take effect for this scheduled task.
        long interval = ServuxPaperConfig.hudDataUpdateInterval();
        this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, HudDataProvider.INSTANCE::tick, interval, interval);
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

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message)
    {
        if (!channel.equals(CHANNEL))
        {
            return;
        }

        ServuxHudPacket packet = ServuxHudPacket.fromBytes(message);

        if (packet == null)
        {
            return;
        }

        if (!player.hasPermission(PERMISSION))
        {
            ServuxPaperReference.debugLog("hud_data: denying access for player {}, insufficient permissions", player.getName());
            return;
        }

        switch (packet.getType())
        {
            case PACKET_C2S_METADATA_REQUEST -> this.sendMetadata(player);
            case PACKET_C2S_SPAWN_DATA_REQUEST -> this.sendSpawnData(player);
            case PACKET_C2S_RECIPE_MANAGER_REQUEST -> HudDataProvider.INSTANCE.sendRecipeManager(player);
            case PACKET_C2S_DATA_LOGGER_REQUEST -> HudDataProvider.INSTANCE.updateLoggerSubscription(player, packet.getCompound());
            default -> ServuxPaperReference.logger().warn("HudMetadataChannel#onPluginMessageReceived: unexpected packet type '{}' from player {}", packet.getType(), player.getName());
        }
    }

    private void sendMetadata(Player player)
    {
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        CompoundTag nbt = HudDataProvider.INSTANCE.buildMetadataNbt(player, spawn);

        this.send(player, ServuxHudPacket.MetadataResponse(nbt));
    }

    private void sendSpawnData(Player player)
    {
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        CompoundTag nbt = HudDataProvider.INSTANCE.buildSpawnNbt(player, spawn);

        this.send(player, ServuxHudPacket.SpawnResponse(nbt));
    }

    private void send(Player player, ServuxHudPacket packet)
    {
        player.sendPluginMessage(this.plugin, CHANNEL, packet.toBytes());
    }
}
