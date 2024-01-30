package fi.dy.masa.servux.network;

import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ServerNetworkPlayRegister
{
    //static ServerPlayNetworking.PlayPayloadHandler<StringPayload> C2SStringHandler;
    //static ServerPlayNetworking.PlayPayloadHandler<DataPayload> C2SDataHandler;
    static ServerPlayNetworking.PlayPayloadHandler<ServuxPayload> C2SSevUXHandler;
    static ServerPlayNetworking.PlayPayloadHandler<SyncmaticaPayload> C2SSyncmatiaHandler;

    public static void registerReceivers() {
        // Do when the server starts, not before
        if (ServuxReference.isServer()) {
            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): isServer() true.");

            //Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): registerStringHandler()");
            //ServerPlayNetworking.registerGlobalReceiver(StringPayload.TYPE, C2SStringHandler);
            //Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): registerDataHandler()");
            //ServerPlayNetworking.registerGlobalReceiver(DataPayload.TYPE, C2SDataHandler);
            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): registerServuxHandler()");
            ServerPlayNetworking.registerGlobalReceiver(ServuxPayload.TYPE, C2SSevUXHandler);
            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): registerSyncmaticaHandler()");
            ServerPlayNetworking.registerGlobalReceiver(SyncmaticaPayload.TYPE, C2SSyncmatiaHandler);
        }
    }

    public static void unregisterReceivers()
    {
        // Do when server stops
        if (ServuxReference.isServer())
        {
            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): isServer() true.");

            //Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): unregisterStringHandler()");
            //ServerPlayNetworking.unregisterGlobalReceiver(StringPayload.TYPE.id());
            //Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): unregisterDataHandler()");
            //ServerPlayNetworking.unregisterGlobalReceiver(DataPayload.TYPE.id());
            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): unregisterServuxHandler()");
            ServerPlayNetworking.unregisterGlobalReceiver(ServuxPayload.TYPE.id());
            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): unregisterSyncmaticaHandler()");
            ServerPlayNetworking.unregisterGlobalReceiver(SyncmaticaPayload.TYPE.id());
        }
    }
    static
    {
        //C2SStringHandler = ServerNetworkPlayHandler::receiveString;
        //C2SDataHandler = ServerNetworkPlayHandler::receiveData;
        C2SSevUXHandler = ServerNetworkPlayHandler::receiveServUX;
        C2SSyncmatiaHandler = ServerNetworkPlayHandler::receiveSyncmaticaServerPayload;
    }
}
