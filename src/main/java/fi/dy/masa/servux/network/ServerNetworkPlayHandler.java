package fi.dy.masa.servux.network;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.event.ServuxLitematicsHandler;
import fi.dy.masa.servux.event.ServuxMetadataHandler;
import fi.dy.masa.servux.event.ServuxStructuresHandler;
import fi.dy.masa.servux.network.payload.channel.ServuxLitematicsPayload;
import fi.dy.masa.servux.network.payload.channel.ServuxMetadataPayload;
import fi.dy.masa.servux.network.payload.channel.ServuxStructuresPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * canSend()
 * Wraps: canSend(player.networkHandler, payload.getId().id());
 * --> Wraps Internally as:
 * `--> ServerNetworkingImpl.getAddon(player.networkHandler).getSendableChannels().contains(payload.getId().id());
 * send()
 * Wraps internally as:
 * --> player.networkHandler.sendPacket(ServerPlayNetworking.createS2CPacket(payload));
 */
public abstract class ServerNetworkPlayHandler
{
    public static void sendServuxStructures(ServuxStructuresPayload payload, ServerPlayerEntity player)
    {
        // Client-bound packet sent from the Server
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
            Servux.printDebug("ServerNetworkPlayHandler#sendServuxStructures(): sending payload id: {}", payload.getId());
        }
    }
    public static void receiveServuxStructures(ServuxStructuresPayload payload, ServerPlayNetworking.Context ctx)
    {
        // Server-bound packet received from the Client
        Servux.printDebug("ServerNetworkPlayHandler#receiveServuxStructures(): id: {} received ServUX Payload (size in bytes): {}", payload.getId(), payload.data().getSizeInBytes());
        ((ServuxStructuresHandler) ServuxStructuresHandler.getInstance()).receiveServuxStructures(payload.data(), ctx, payload.getId().id());
    }
    public static void sendServuxMetadata(ServuxMetadataPayload payload, ServerPlayerEntity player)
    {
        // Client-bound packet sent from the Server
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
            Servux.printDebug("ServerNetworkPlayHandler#sendServuxMetadata(): sending payload id: {}", payload.getId());
        }
    }
    public static void receiveServuxMetadata(ServuxMetadataPayload payload, ServerPlayNetworking.Context ctx)
    {
        // Server-bound packet received from the Client
        Servux.printDebug("ServerNetworkPlayHandler#receiveServuxMetadata(): id: {} received ServUX Payload (size in bytes): {}", payload.getId(), payload.data().getSizeInBytes());
        ((ServuxMetadataHandler) ServuxMetadataHandler.getInstance()).receiveServuxMetadata(payload.data(), ctx, payload.getId().id());
    }
    public static void sendServuxLitematics(ServuxLitematicsPayload payload, ServerPlayerEntity player)
    {
        // Client-bound packet sent from the Server
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
            Servux.printDebug("ServerNetworkPlayHandler#sendServuxLitematics(): sending payload id: {}", payload.getId());
        }
    }
    public static void receiveServuxLitematics(ServuxLitematicsPayload payload, ServerPlayNetworking.Context ctx)
    {
        // Server-bound packet received from the Client
        Servux.printDebug("ServerNetworkPlayHandler#receiveServuxLitematics(): id: {} received ServUX Payload (size in bytes): {}", payload.getId(), payload.data().getSizeInBytes());
        ((ServuxLitematicsHandler) ServuxLitematicsHandler.getInstance()).receiveServuxLitematics(payload.data(), ctx, payload.getId().id());
    }
}
