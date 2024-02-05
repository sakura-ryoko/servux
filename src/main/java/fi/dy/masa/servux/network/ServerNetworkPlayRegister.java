package fi.dy.masa.servux.network;

import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ServerNetworkPlayRegister
{
    static ServerPlayNetworking.PlayPayloadHandler<ServuxPayload> C2SSevUXHandler;

    public static void registerReceivers() {
        // Do when the server starts, not before
        if (ServuxReference.isServer()) {
            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): isServer() true.");

            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): registerServuxHandler()");
            ServerPlayNetworking.registerGlobalReceiver(ServuxPayload.TYPE, C2SSevUXHandler);
        }
    }

    public static void unregisterReceivers()
    {
        // Do when server stops
        if (ServuxReference.isServer())
        {
            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): isServer() true.");

            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): unregisterServuxHandler()");
            ServerPlayNetworking.unregisterGlobalReceiver(ServuxPayload.TYPE.id());
        }
    }
    static
    {
        C2SSevUXHandler = ServerNetworkPlayHandler::receiveServUX;
    }
}
