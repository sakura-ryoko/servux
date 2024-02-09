package fi.dy.masa.servux.dataproviders;

import fi.dy.masa.servux.network.packet.PacketType;
import fi.dy.masa.servux.network.payload.channel.ServuxLitematicsPayload;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * This provides the basic foundations for a future "Syncmatica-like" storage server
 */
public class LitematicsDataProvider extends DataProviderBase
{
    public static final LitematicsDataProvider INSTANCE = new LitematicsDataProvider();
    private static Identifier getChannel() { return ServuxLitematicsPayload.TYPE.id(); }
    protected final NbtCompound metadata = new NbtCompound();
    protected static String LitematicDirectory = "litematics";
    protected LitematicsDataProvider()
    {
        super(
                "litematic_data_provider",
                PacketType.Litematics.PROTOCOL_VERSION,
                "Alpha interface for providing a Server-side backend for Litematic storage.");
        this.metadata.putString("id", this.getNetworkChannel());
        this.metadata.putInt("version", PacketType.Litematics.PROTOCOL_VERSION);
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
