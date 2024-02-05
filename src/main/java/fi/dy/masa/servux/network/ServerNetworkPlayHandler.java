package fi.dy.masa.servux.network;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.event.ServuxPayloadHandler;
import fi.dy.masa.servux.network.payload.*;
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
}
