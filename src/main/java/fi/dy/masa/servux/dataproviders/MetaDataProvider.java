package fi.dy.masa.servux.dataproviders;

import fi.dy.masa.servux.network.packet.PacketType;
import fi.dy.masa.servux.network.payload.channel.ServuxMetadataPayload;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * This provides the basic foundations for a Metadata Interface for various things that's not structure related
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

    //@Override
    //public boolean shouldTick() { return true; }

    //@Override
    //public void tick(MinecraftServer server, int tickCounter)
    //{
    //}
}
