package fi.dy.masa.servux.network.handler;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public abstract class ServerNetworkPlayHandler
{
    // String Payloads
    public static void send(StringPayload payload, ServerPlayerEntity player)
    {
        // Client-Bound packet sent from the Server
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
            Servux.printDebug("ServerNetworkPlayHandler#send(): sending payload id: {}", payload.getId());
        }
    }
    public static void receive(StringPayload payload, ServerPlayNetworking.Context ctx)
    {
        // Server-bound packet received from the Client
        String response = payload.toString();
        Servux.printDebug("ServerNetworkPlayHandler#receive(): id: {} received C2SString Payload: {}", payload.getId(), response);
        ctx.player().sendMessage(Text.of("Your message has been received by the server:"));
        ctx.player().sendMessage(Text.of("You sent (STRING) me: "+response));
    }
    // Data Payloads
    public static void send(DataPayload payload, ServerPlayerEntity player)
    {
        // Client-bound packet sent from the Server
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
            Servux.printDebug("ServerNetworkPlayHandler#send(): sending payload id: {}", payload.getId());
        }
    }

    public static void receive(DataPayload payload, ServerPlayNetworking.Context ctx)
    {
        // Server-bound packet received from the Client
        Servux.printDebug("ServerNetworkPlayHandler#receive(): received C2SData Payload (size in bytes): {}", payload.data().getSizeInBytes());
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeByteArray((payload.data().getByteArray(DataPayload.NBT)));
        Servux.printDebug("ServerNetworkPlayHandler#receive(): buf size in bytes: {}", buf.readableBytes());
        // --> To write a PacketByteBuf from NbtCompound
//        String response = payload.data().getString(DataPayload.NBT);
        String response = buf.readString();
        Servux.printDebug("ServerNetworkPlayHandler#receive(): id: {}, String: {}", payload.getId(), response);
    }
}
