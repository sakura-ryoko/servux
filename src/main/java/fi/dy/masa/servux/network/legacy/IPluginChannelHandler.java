package fi.dy.masa.servux.network.legacy;

import fi.dy.masa.servux.network.payload.ServuxPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.*;

import fi.dy.masa.servux.network.packet.ServuxPayloadListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Objects;

public interface IPluginChannelHandler<T extends CustomPayload>
{
    Identifier getChannel();

    default PlayPayloadHandler<T> getServerPacketHandler()
    {
        if (this.usePacketSplitter())
        {
            return (T payload, Context ctx) -> this.handleViaPacketSplitter((ServuxPayload) payload, ctx);
        }

        return (T payload, Context ctx) -> this.onPacketReceived(payload.getId().id(), payload, ctx);
    }
    //@Deprecated
    default void handleViaPacketSplitter(ServuxPayload payload, ServerPlayNetworking.Context ctx)
    {
        PacketByteBuf fullBuf = ServuxPayloadListener.splitServuxPayload(this.getChannel(), payload, ctx);

        if (fullBuf != null)
        {
            Objects.requireNonNull(ctx.player().getServer()).execute(() -> this.onPacketReceived(this.getChannel(), fullBuf, ctx));
        }
    }

    //@Deprecated
    default void onPacketReceived(Identifier id, T payload, ServerPlayNetworking.Context ctx)
    {
    }
    default void onPacketReceived(Identifier id, PacketByteBuf buf, ServerPlayNetworking.Context ctx)
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

    default boolean subscribe(ServerPlayNetworking.Context ctx)
    {
        return false;
    }

    default boolean unsubscribe(ServerPlayNetworking.Context ctx)
    {
        return false;
    }
}
