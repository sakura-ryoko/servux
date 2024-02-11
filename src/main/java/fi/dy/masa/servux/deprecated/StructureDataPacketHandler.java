package fi.dy.masa.servux.deprecated;

//import fi.dy.masa.servux.network.payload.ServuxPayload;
//import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//import net.minecraft.network.packet.CustomPayload;
//import net.minecraft.util.Identifier;
//import fi.dy.masa.servux.dataproviders.StructureDataProvider;

/**
 * Original Handler
 */
@Deprecated
public class StructureDataPacketHandler implements IPluginChannelHandler
{
    // Splitting Identifier into two parameters
    // --> It should result in the same "servux:structures" output
    /*
    public static final Identifier CHANNEL = new Identifier("servux", "structures");
    public static final StructureDataPacketHandler INSTANCE = new StructureDataPacketHandler();

    public static final int PROTOCOL_VERSION = 1;
    public static final int PACKET_S2C_METADATA = 1;
    public static final int PACKET_S2C_STRUCTURE_DATA = 2;

    @Override
    public Identifier getChannel()
    {
        return CHANNEL;
    }

    @Override
    public boolean isSubscribable()
    {
        return true;
    }

    @Override
    public boolean subscribe(ServerPlayNetworkHandler netHandler)
    {
        return StructureDataProvider.INSTANCE.register(netHandler.getPlayer());
    }

    @Override
    public boolean unsubscribe(ServerPlayNetworkHandler netHandler)
    {
        return StructureDataProvider.INSTANCE.unregister(netHandler.getPlayer());
    }
     */
}
