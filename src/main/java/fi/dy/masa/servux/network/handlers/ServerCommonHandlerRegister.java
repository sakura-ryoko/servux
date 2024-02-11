package fi.dy.masa.servux.network.handlers;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.channel.*;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * This probably cannot be made too abstract, because it references items in the static context directly (ie. Specific Payload types)
 */
public class ServerCommonHandlerRegister
{
    public static final ServerCommonHandlerRegister INSTANCE = new ServerCommonHandlerRegister();
    public static ServerCommonHandlerRegister getInstance() { return INSTANCE; }

    public <T extends CustomPayload> void registerPlayHandler(CustomPayload.Id<T> type, ServerPlayNetworking.PlayPayloadHandler<T> handler)
    {
        Servux.printDebug("ServerCommonHandlerRegister#registerPlayHandler(): for type {}", type.id().toString());
        ServerPlayNetworking.registerGlobalReceiver(type, handler);
    }
    public <T extends CustomPayload> void unregisterPlayHandler(CustomPayload.Id<T> type)
    {
        Servux.printDebug("ServerCommonHandlerRegister#unregisterPlayHandler(): for type {}", type.id().toString());
        ServerPlayNetworking.unregisterGlobalReceiver(type.id());
    }
    public <T extends CustomPayload> void registerConfigHandler(CustomPayload.Id<T> type, ServerConfigurationNetworking.ConfigurationPacketHandler<T> handler)
    {
        Servux.printDebug("ServerCommonHandlerRegister#registerConfigHandler(): for type {}", type.id().toString());
        ServerConfigurationNetworking.registerGlobalReceiver(type, handler);
    }
    public <T extends CustomPayload> void unregisterConfigHandler(CustomPayload.Id<T> type)
    {
        Servux.printDebug("ServerCommonHandlerRegister#unregisterConfigHandler(): for type {}", type.id().toString());
        ServerConfigurationNetworking.unregisterGlobalReceiver(type.id());
    }
    @SuppressWarnings("unchecked")
    public <T extends CustomPayload> CustomPayload.Id<T> getPayloadType(PayloadType type)
    {
        //Servux.printDebug("ServerCommonHandlerRegister#getPayload(): type {}", type.toString());
        if (type == PayloadType.SERVUX_BYTEBUF)
        {
            return (CustomPayload.Id<T>) ServuxBufPayload.TYPE;
        }
        else if (type == PayloadType.SERVUX_STRUCTURES)
        {
            return (CustomPayload.Id<T>) ServuxStructuresPayload.TYPE;
        }
        else if (type == PayloadType.SERVUX_METADATA)
        {
            return (CustomPayload.Id<T>) ServuxMetadataPayload.TYPE;
        }
        else if (type == PayloadType.SERVUX_LITEMATICS)
        {
            return (CustomPayload.Id<T>) ServuxMetadataPayload.TYPE;
        }
        else
        {
            return null;
        }
    }
    @SuppressWarnings("unchecked")
    public <B extends ByteBuf, T extends CustomPayload> PacketCodec<B, T> getPacketCodec(PayloadType type)
    {
        //Servux.printDebug("ServerCommonHandlerRegister#getPacketCodec(): type {}", type.toString());
        if (type == PayloadType.SERVUX_BYTEBUF)
        {
            return (PacketCodec<B, T>) ServuxBufPayload.CODEC;
        }
        else if (type == PayloadType.SERVUX_STRUCTURES)
        {
            return (PacketCodec<B, T>) ServuxStructuresPayload.CODEC;
        }
        else if (type == PayloadType.SERVUX_METADATA)
        {
            return (PacketCodec<B, T>) ServuxMetadataPayload.CODEC;
        }
        else if (type == PayloadType.SERVUX_LITEMATICS)
        {
            return (PacketCodec<B, T>) ServuxLitematicsPayload.CODEC;
        }
        else
        {
            return null;
        }
    }
}
