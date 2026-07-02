package fi.dy.masa.servux.paper.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import fi.dy.masa.servux.paper.ServuxPaperPlugin;
import fi.dy.masa.servux.paper.ServuxPaperReference;
import fi.dy.masa.servux.paper.provider.EntitiesDataProvider;

/**
 * Registers and handles the {@code servux:entity_data} plugin channel via the plain Bukkit
 * {@link org.bukkit.plugin.messaging.Messenger} API - no PacketEvents needed for this channel.
 * Unlike {@code structures}, this channel is purely request/response - no periodic tick loop or
 * chunk-watch trigger needed.
 */
public class EntitiesChannel implements PluginMessageListener
{
    public static final String CHANNEL = EntitiesDataProvider.CHANNEL_ID;
    private static final String PERMISSION = "servux.entity_data";

    private final ServuxPaperPlugin plugin;

    public EntitiesChannel(ServuxPaperPlugin plugin)
    {
        this.plugin = plugin;
    }

    public void register()
    {
        EntitiesDataProvider.INSTANCE.init(this.plugin);

        Bukkit.getMessenger().registerOutgoingPluginChannel(this.plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this.plugin, CHANNEL, this);
    }

    public void unregister()
    {
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

        ServuxEntitiesPacket packet = ServuxEntitiesPacket.fromBytes(message);

        if (packet == null)
        {
            return;
        }

        if (!player.hasPermission(PERMISSION))
        {
            ServuxPaperReference.debugLog("entity_data: denying access for player {}, insufficient permissions", player.getName());
            return;
        }

        switch (packet.getType())
        {
            case PACKET_C2S_METADATA_REQUEST -> EntitiesDataProvider.INSTANCE.sendMetadata(player);
            case PACKET_C2S_BLOCK_ENTITY_REQUEST -> EntitiesDataProvider.INSTANCE.onBlockEntityRequest(player, packet.getPos());
            case PACKET_C2S_ENTITY_REQUEST -> EntitiesDataProvider.INSTANCE.onEntityRequest(player, packet.getEntityId());
            default -> ServuxPaperReference.logger().warn("EntitiesChannel#onPluginMessageReceived: unexpected packet type '{}' from player {}", packet.getType(), player.getName());
        }
    }
}
