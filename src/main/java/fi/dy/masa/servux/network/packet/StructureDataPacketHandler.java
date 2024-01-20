package fi.dy.masa.servux.network.packet;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.IPluginChannelHandler;

public class StructureDataPacketHandler implements IPluginChannelHandler
{
    // Splitting Identifier into two parameters
    // --> It should result in the same "servux:structures" output
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
}
