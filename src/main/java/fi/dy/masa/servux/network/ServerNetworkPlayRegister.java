package fi.dy.masa.servux.network;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.*;
import fi.dy.masa.servux.network.handler.ServerNetworkPlayHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ServerNetworkPlayRegister
{
    static ServerPlayNetworking.PlayPayloadHandler<StringPayload> C2SStringHandler;
    static ServerPlayNetworking.PlayPayloadHandler<DataPayload> C2SDataHandler;
    
    public static void registerDefaultReceivers()
    {
        // Do when the server starts, not before
        if (Reference.isServer())
        {
            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): isServer() true.");
            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): registerStringHandler()");

            ServerPlayNetworking.registerGlobalReceiver(StringPayload.TYPE, C2SStringHandler);

            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): registerDataHandler()");
            ServerPlayNetworking.registerGlobalReceiver(DataPayload.TYPE, C2SDataHandler);
        }
    }

    public static void unregisterDefaultReceivers()
    {
        // Do when server stops
        if (Reference.isServer())
        {
            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): isServer() true.");
            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): registerStringHandler()");

            ServerPlayNetworking.unregisterGlobalReceiver(StringPayload.TYPE.id());

            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): registerDataHandler()");
            ServerPlayNetworking.unregisterGlobalReceiver(DataPayload.TYPE.id());
        }
    }
    static
    {
        C2SStringHandler = ServerNetworkPlayHandler::receive;
        C2SDataHandler = ServerNetworkPlayHandler::receive;
    }
}
