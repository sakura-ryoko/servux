package fi.dy.masa.servux.dataproviders.data;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.dataproviders.DataProviderBase;
import fi.dy.masa.servux.dataproviders.client.LitematicClient;
import fi.dy.masa.servux.litematics.placement.LitematicPlacement;
import fi.dy.masa.servux.litematics.players.PlayerIdentityProvider;
import fi.dy.masa.servux.network.packet.PacketType;
import fi.dy.masa.servux.network.payload.channel.ServuxS2CLitematicsPayload;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This provides the basic foundations for a future "Syncmatica-like" storage server, and
 * I am trying to make it work nearly exactly the same using a different channel ID, and this
 * might even be directly compatible with Syncmaica or Litematica itself in the future.
 */
public class LitematicsDataProvider extends DataProviderBase
{
    public static final LitematicsDataProvider INSTANCE = new LitematicsDataProvider();
    private static Identifier getChannel() { return ServuxS2CLitematicsPayload.TYPE.id(); }
    protected final Map<UUID, LitematicClient> CLIENTS = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    // Where to store the litematics
    protected final String LitematicDirectory = "litematics";
    protected File worldFolder;
    protected File litematicFolder = new File("."+File.separator+ LitematicDirectory);
    // PlayerIdentityProvider is to store a list of names / UUID for use with linking to litematics,
    // not for active client information.
    protected PlayerIdentityProvider players = new PlayerIdentityProvider();
    protected LitematicsDataProvider()
    {
        super(
                "litematic_data_provider",
                PacketType.Litematics.PROTOCOL_VERSION,
                "Alpha interface for providing a Server-side backend for Litematic storage.");
        this.metadata.putString("id", this.getNetworkChannel());
        this.metadata.putInt("version", PacketType.Litematics.PROTOCOL_VERSION);
    }

    public PlayerIdentityProvider getPlayerIdentityProvider() { return this.players; }

    @Override
    public String getNetworkChannel() { return getChannel().toString(); }

    // Returns the File object to the Litematics Storage
    public File getLitematicFolder()
    {
        if (this.litematicFolder != null)
            return this.litematicFolder;
        else return null;
    }

    // Returns the File object for the "configs" folder under the world folder for storing things like a list of listematics and PlayerIdentity data.
    public File getConfigFolder()
    {
        if (this.worldFolder != null)
            return new File(this.worldFolder, ServuxReference.MOD_ID);
        else return null;
    }

    public void register(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        LitematicClient newClient = new LitematicClient(player.getName().getLiteralString(), uuid, null);
        newClient.registerClient(player);
        newClient.litematicsEnableClient();
        CLIENTS.put(uuid, newClient);
        Servux.printDebug("LitematicsDataProvider#register(): new LitematicClient register() for {}", player.getName().getLiteralString());
    }
    public void unregister(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        LitematicClient oldClient = CLIENTS.get(uuid);
        oldClient.litematicsDisableClient();
        oldClient.unregisterClient();
        CLIENTS.remove(uuid);
        Servux.printDebug("LitematicsDataProvider#register(): new LitematicClient unregister() for {}", player.getName().getLiteralString());
    }
    // Returns true if Litematic Placement is still downloading.
    public boolean getDownloadState(LitematicPlacement placement)
    {
        return false;
    }

    /**
     * Splits incoming packets based on packetType.
     * @param packetType (Packet Type)
     * @param data (Actual data)
     * @param player (From Player)
     */
    public void splitPacketType(int packetType, NbtCompound data, ServerPlayerEntity player)
    {
        if (packetType == PacketType.Litematics.PACKET_REGISTER_VERSION)
        {
            String version = data.getString("version");
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_REGISTER_VERSION: {}", version);
        }
        else if (packetType == PacketType.Litematics.PACKET_FEATURE_REQUEST)
        {
            // Send "Features" packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_FEATURE_REQUEST");
        }
        else if (packetType == PacketType.Litematics.PACKET_SEND_FEATURES)
        {
            // Receive "Features" packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_SEND_FEATURES");
        }
        else if (packetType == PacketType.Litematics.PACKET_CONFIRM_PARTNER)
        {
            // Confirm "partner/user" packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_CONFIRM_PARTNER");
        }
        else if (packetType == PacketType.Litematics.PACKET_REGISTER_METADATA)
        {
            // Register Metadata packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_REGISTER_METADATA");
        }
        else if (packetType == PacketType.Litematics.PACKET_REQUEST_METADATA)
        {
            // Request Metadata packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_REQUEST_METADATA");
        }
        else if (packetType == PacketType.Litematics.PACKET_REQUEST_LITEMATIC)
        {
            // Request Litematic packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_REQUEST_LITEMATIC");
        }
        else if (packetType == PacketType.Litematics.PACKET_SEND_LITEMATIC)
        {
            // Send Litematic packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_SEND_LITEMATIC");
        }
        else if (packetType == PacketType.Litematics.PACKET_RECEIVE_LITEMATIC)
        {
            // Receive Litematic packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_RECEIVE_LITEMATIC");
        }
        else if (packetType == PacketType.Litematics.PACKET_FINISHED_LITEMATIC)
        {
            // Finished Litematic packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_FINISHED_LITEMATIC");
        }
        else if (packetType == PacketType.Litematics.PACKET_CANCEL_LITEMATIC)
        {
            // Cancel Litematic packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_CANCEL_LITEMATIC");
        }
        else if (packetType == PacketType.Litematics.PACKET_REMOVE_LITEMATIC)
        {
            // Remove Litematic packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_REMOVE_LITEMATIC");
        }
        else if (packetType == PacketType.Litematics.PACKET_CANCEL_SHARE)
        {
            // Cancel Share packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_CANCEL_SHARE");
        }
        else if (packetType == PacketType.Litematics.PACKET_SEND_MODIFY)
        {
            // Send Modify packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_SEND_MODIFY");
        }
        else if (packetType == PacketType.Litematics.PACKET_REQUEST_MODIFY)
        {
            // Request Modify packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_REQUEST_MODIFY");
        }
        else if (packetType == PacketType.Litematics.PACKET_REQUEST_MODIFY_ACCEPT)
        {
            // Request Modify Accept packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_REQUEST_MODIFY_ACCEPT");
        }
        else if (packetType == PacketType.Litematics.PACKET_REQUEST_MODIFY_DENY)
        {
            // Request Modify Deny packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_REQUEST_MODIFY_DENY");
        }
        else if (packetType == PacketType.Litematics.PACKET_FINISH_MODIFY)
        {
            // Finish Modify packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_FINISH_MODIFY");
        }
        else if (packetType == PacketType.Litematics.PACKET_MESSAGE)
        {
            // "Message" packet.
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): PACKET_MESSAGE");
        }
        else
        {
            Servux.printDebug("LitematicsDataProvider#splitPacketType(): Received invalid packetType {} from player {}", packetType, player.getName().getLiteralString());
        }
    }
}
