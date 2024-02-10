package fi.dy.masa.servux.dataproviders;

import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.litematics.placement.LitematicPlacement;
import fi.dy.masa.servux.litematics.players.PlayerIdentityProvider;
import fi.dy.masa.servux.network.packet.PacketType;
import fi.dy.masa.servux.network.payload.channel.ServuxLitematicsPayload;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.io.File;

/**
 * This provides the basic foundations for a future "Syncmatica-like" storage server, and
 * I am trying to make it work nearly exactly the same using a different channel ID, and this
 * might even be directly compatible with Syncmaica or Litematica itself soon.
 */
public class LitematicsDataProvider extends DataProviderBase
{
    public static final LitematicsDataProvider INSTANCE = new LitematicsDataProvider();
    private static Identifier getChannel() { return ServuxLitematicsPayload.TYPE.id(); }
    protected final NbtCompound metadata = new NbtCompound();
    protected String LitematicDirectory = "litematics";
    protected File worldFolder;
    protected File litematicFolder = new File("."+File.separator+ LitematicDirectory);
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
    public File getLitematicFolder()
    {
        if (this.litematicFolder != null)
            return this.litematicFolder;
        else return null;
    }
    public File getConfigFolder()
    {
        if (this.worldFolder != null)
            return new File(this.worldFolder, ServuxReference.MOD_ID);
        else return null;
    }
    public boolean getDownloadState(LitematicPlacement placement)
    {
        return false;
    }

    //@Override
    //public boolean shouldTick() { return true; }

    //@Override
    //public void tick(MinecraftServer server, int tickCounter)
    //{
    //}
}