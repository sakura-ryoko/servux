package fi.dy.masa.servux.deprecated;

//import net.minecraft.network.PacketByteBuf;
//import net.minecraft.server.MinecraftServer;
//import net.minecraft.server.network.ServerPlayNetworkHandler;
//import net.minecraft.util.Identifier;

//import java.util.Objects;

/**
 * Original Handler interface
 */
@Deprecated
public interface IPluginChannelHandler
{
    /*
    Identifier getChannel();

    default PlayChannelHandler getServerPacketHandler()
    {
        if (this.usePacketSplitter())
        {
            return (server, player, net, buf, responder) -> this.handleViaPacketSplitter(server, net, buf);
        }

        return (server, player, net, buf, responder) -> server.execute(() -> this.onPacketReceived(buf, net));
    }

    default void handleViaPacketSplitter(MinecraftServer server, ServerPlayNetworkHandler netHandler, PacketByteBuf buf)
    {
        PacketByteBuf fullBuf = PacketSplitter.receive(this.getChannel(), buf, netHandler);

        if (fullBuf != null)
        {
            server.execute(() -> this.onPacketReceived(fullBuf, netHandler));
        }
    }

    default void onPacketReceived(PacketByteBuf buf, ServerPlayNetworkHandler netHandler)
    {
    }

    default boolean usePacketSplitter()
    {
        return true;
    }

    default boolean isSubscribable()
    {
        return false;
    }

    default boolean subscribe(ServerPlayNetworkHandler netHandler)
    {
        return false;
    }

    default boolean unsubscribe(ServerPlayNetworkHandler netHandler)
    {
        return false;
    }
    */
}
