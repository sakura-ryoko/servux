package fi.dy.masa.servux.dataproviders;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.client.MetadataClient;
import fi.dy.masa.servux.network.packet.PacketType;
import fi.dy.masa.servux.network.payload.channel.ServuxMetadataPayload;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

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
    private static Identifier getChannel() { return ServuxMetadataPayload.TYPE.id(); }
    protected final Map<UUID, MetadataClient> CLIENTS = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected MetaDataProvider()
    {
        super(
                "meta_data_provider",
                PacketType.Metadata.PROTOCOL_VERSION,
                "Alpha interface for providing a Server Metadata backend for various mods.");
        this.metadata.putString("id", this.getNetworkChannel());
        this.metadata.putInt("version", PacketType.Metadata.PROTOCOL_VERSION);
    }

    //@Override
    public String getNetworkChannel() { return getChannel().toString(); }

    public void register(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        MetadataClient newClient = new MetadataClient(player.getName().getLiteralString(), uuid, null, null);
        newClient.registerClient(player);
        newClient.metadataEnableClient();
        CLIENTS.put(uuid, newClient);
        Servux.printDebug("MetaDataProvider#register(): new MetadataClient register() for {}", player.getName().getLiteralString());
    }
    public void unregister(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        MetadataClient oldClient = CLIENTS.get(uuid);
        oldClient.unregisterClient();
        CLIENTS.remove(uuid);
        Servux.printDebug("MetaDataProvider#register(): new MetadataClient unregister() for {}", player.getName().getLiteralString());
    }
    public void splitPacketType(int packetType, NbtCompound data, ServerPlayerEntity player)
    {
        // NO-OP
    }
    @Override
    public BlockPos getSpawnPos() { return null; }

    @Override
    public void setSpawnPos(BlockPos spawnPos) { }

    @Override
    public int getSpawnChunkRadius() { return 0; }

    @Override
    public void setSpawnChunkRadius(int radius) { }

    @Override
    public boolean refreshSpawnMetadata() { return false; }

    @Override
    public void setRefreshSpawnMetadataComplete() { }
}
