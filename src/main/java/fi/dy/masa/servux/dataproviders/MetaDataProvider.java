package fi.dy.masa.servux.dataproviders;

import fi.dy.masa.servux.network.packet.PacketType;
import fi.dy.masa.servux.network.payload.channel.ServuxMetadataPayload;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * This provides the basic foundations for a Metadata Interface for various things that's not structure related,
 * such as spawn_chunk_radius and spawn_pos
 */
public class MetaDataProvider extends DataProviderBase
{
    public static final MetaDataProvider INSTANCE = new MetaDataProvider();
    private static Identifier getChannel() { return ServuxMetadataPayload.TYPE.id(); }
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

    // FIXME
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
