package fi.dy.masa.servux.dataproviders;

import fi.dy.masa.servux.network.payload.SyncmaticaPayload;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * This provides the basic foundations for a future "Syncmatica-lite" storage server
 */
public class SyncmaticDataProvider extends DataProviderBase
{
    public static final SyncmaticDataProvider INSTANCE = new SyncmaticDataProvider();
    private static Identifier getChannel() { return SyncmaticaPayload.TYPE.id(); }

    protected final NbtCompound metadata = new NbtCompound();
    protected static String SyncmaticDirectory = "syncmatics";
    protected SyncmaticDataProvider()
    {
        super(
                "syncmatic_data_provider",
                0,
                "Alpha interface for providing a Syncmatica backend for Litematic storage.");
        this.metadata.putString("id", this.getNetworkChannel());
        this.metadata.putInt("version", 0);
    }

    @Override
    public String getNetworkChannel() { return getChannel().toString(); }

    //@Override
    //public boolean shouldTick() { return true; }

    //@Override
    //public void tick(MinecraftServer server, int tickCounter)
    //{
    //}
}
