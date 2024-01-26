package fi.dy.masa.servux.network.legacy;

import fi.dy.masa.servux.network.payload.ServuxPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

public class StructureDataPacketHandler<T extends CustomPayload> implements IPluginChannelHandler<T>
{
    // Splitting Identifier into two parameters
    // --> It should result in the same "servux:structures" output
    //public static final Identifier CHANNEL = new Identifier("servux", "structures");
    public static final StructureDataPacketHandler<ServuxPayload> INSTANCE = new StructureDataPacketHandler<>();

    public static final int PROTOCOL_VERSION = 1;
    public static final int PACKET_S2C_METADATA = 1;
    public static final int PACKET_S2C_STRUCTURE_DATA = 2;

    @Override
    public Identifier getChannel()
    {
        return null;
    }

    @Override
    public boolean isSubscribable()
    {
        return true;
    }

    @Override
    public boolean subscribe(ServerPlayNetworking.Context ctx)
    {
        return StructureDataProvider.INSTANCE.register(ctx.player());
    }

    @Override
    public boolean unsubscribe(ServerPlayNetworking.Context ctx)
    {
        return StructureDataProvider.INSTANCE.unregister(ctx.player());
    }
}
