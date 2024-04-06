package fi.dy.masa.servux.dataproviders.data;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.dataproviders.DataProviderBase;
import fi.dy.masa.servux.dataproviders.client.MetadataClient;
import fi.dy.masa.servux.network.PacketType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This provides the basic foundations for a Metadata Interface for various things that's not structure related,
 * such as spawn_chunk_radius and spawn_pos
 */
public class MetaDataProvider extends DataProviderBase
{
    public static final MetaDataProvider INSTANCE = new MetaDataProvider();
    protected final Map<UUID, MetadataClient> CLIENTS = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected final int protocolVersion = PacketType.Metadata.PROTOCOL_VERSION;

    protected MetaDataProvider()
    {
        super(
                "meta_data_provider",
                PacketType.Metadata.PROTOCOL_VERSION,
                "Alpha interface for providing a Server Metadata backend for various mods.");

        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.protocolVersion);
        this.metadata.putString("servux", ServuxReference.MOD_STRING);
    }

    @Override
    public PayloadType getNetworkChannel() { return PayloadType.SERVUX_METADATA; }

    @Override
    public int getProtocolVersion() { return this.protocolVersion; }

    public void register(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        MetadataClient newClient = new MetadataClient(player.getName().getLiteralString(), uuid, null);
        newClient.registerClient(addr, profile, player);
        newClient.metadataEnableClient();
        CLIENTS.put(uuid, newClient);
        Servux.printDebug("MetaDataProvider#register(): new MetadataClient register() for {}", player.getName().getLiteralString());
    }
    public void unregister(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        MetadataClient oldClient = CLIENTS.get(uuid);
        oldClient.metadataDisableClient();
        oldClient.unregisterClient();
        CLIENTS.remove(uuid);
        Servux.printDebug("MetaDataProvider#register(): new MetadataClient unregister() for {}", player.getName().getLiteralString());
    }
    public void splitPacketType(int packetType, NbtCompound data, ServerPlayerEntity player)
    {
        // NO-OP
    }
}
