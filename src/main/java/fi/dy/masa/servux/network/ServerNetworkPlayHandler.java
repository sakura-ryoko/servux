package fi.dy.masa.servux.network;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.event.ServuxPayloadHandler;
import fi.dy.masa.servux.network.payload.*;
import fi.dy.masa.servux.util.PayloadUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class ServerNetworkPlayHandler
{
    // String Payloads
    public static void sendString(StringPayload payload, ServerPlayerEntity player)
    {
        // Client-Bound packet sent from the Server
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
            Servux.printDebug("ServerNetworkPlayHandler#sendString(): sending payload id: {}", payload.getId());
        }
    }
    public static void receiveString(StringPayload payload, ServerPlayNetworking.Context ctx)
    {
        // Server-bound packet received from the Client
        String response = payload.toString();
        Servux.printDebug("ServerNetworkPlayHandler#receiveString(): id: {} received C2SString Payload: {}", payload.getId(), response);
        //ctx.player().sendMessage(Text.of("Your message has been received by the server:"));
        //ctx.player().sendMessage(Text.of("You sent (STRING) me: "+response));
    }
    // Data Payloads
    public static void sendData(DataPayload payload, ServerPlayerEntity player)
    {
        // Client-bound packet sent from the Server
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
            Servux.printDebug("ServerNetworkPlayHandler#sendData(): sending payload id: {}", payload.getId());
        }
    }

    public static void receiveData(DataPayload payload, ServerPlayNetworking.Context ctx)
    {
        // Server-bound packet received from the Client
        Servux.printDebug("ServerNetworkPlayHandler#receive(): received C2SData Payload (size in bytes): {}", payload.data().getSizeInBytes());
        PacketByteBuf buf = PayloadUtils.fromNbt(payload.data(), DataPayload.KEY);
        assert buf != null;
        Servux.printDebug("ServerNetworkPlayHandler#receive(): buf size in bytes: {}", buf.readableBytes());
        // --> To write a PacketByteBuf from NbtCompound
//        String response = payload.data().getString(DataPayload.NBT);
        String response = buf.readString();
        Servux.printDebug("ServerNetworkPlayHandler#receiveData(): id: {}, String: {}", payload.getId(), response);
    }

    public static void sendServUX(ServuxPayload payload, ServerPlayerEntity player)
    {
        // Client-bound packet sent from the Server
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
            Servux.printDebug("ServerNetworkPlayHandler#sendServUX(): sending payload id: {}", payload.getId());
        }
    }
    public static void receiveServUX(ServuxPayload payload, ServerPlayNetworking.Context ctx)
    {
        // Server-bound packet received from the Client
        Servux.printDebug("ServerNetworkPlayHandler#receiveServUX(): id: {} received ServUX Payload (size in bytes): {}", payload.getId(), payload.data().getSizeInBytes());
        ((ServuxPayloadHandler) ServuxPayloadHandler.getInstance()).receiveServuxPayload(payload.data(), ctx, payload.getId().id());
    }

    public static void sendSyncmaticaUX(SyncmaticaPayload payload, ServerPlayerEntity player)
    {
        // Client-bound packet sent from the Server
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
            Servux.printDebug("ServerNetworkPlayHandler#sendSyncmaticaUX(): sending payload id: {}", payload.getId());
        }

    }
    public static void receiveSyncmaticaUX(SyncmaticaPayload payload, ServerPlayNetworking.Context ctx)
    {
        Servux.printDebug("ServerNetworkPlayHandler#receiveSyncmaticaUX(): id: {} received ServUX Payload (size in bytes): {}", payload.getId(), payload.data().getSizeInBytes());
        //((ServuxPayloadHandler) ServuxPayloadHandler.getInstance()).receiveServuxPayload(payload.data(), ctx, payload.getId().id());
    }
}
