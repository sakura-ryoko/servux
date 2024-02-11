package fi.dy.masa.servux.deprecated;

import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.channel.ServuxLitematicsPayload;
import fi.dy.masa.servux.network.payload.channel.ServuxMetadataPayload;
import fi.dy.masa.servux.network.payload.channel.ServuxStructuresPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

@Deprecated
public class ServerNetworkPlayRegister
{
    static ServerPlayNetworking.PlayPayloadHandler<ServuxStructuresPayload> C2SSevuxStructuresHandler;
    static ServerPlayNetworking.PlayPayloadHandler<ServuxMetadataPayload> C2SSevuxMetadataHandler;
    static ServerPlayNetworking.PlayPayloadHandler<ServuxLitematicsPayload> C2SSevuxLitematicsHandler;

    public static void registerReceivers() {
        // Do when the server starts, not before
        if (ServuxReference.isServer()) {
            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): isServer() true --> registerServuxHandlers()");

            ServerPlayNetworking.registerGlobalReceiver(ServuxStructuresPayload.TYPE, C2SSevuxStructuresHandler);
            ServerPlayNetworking.registerGlobalReceiver(ServuxMetadataPayload.TYPE, C2SSevuxMetadataHandler);
            ServerPlayNetworking.registerGlobalReceiver(ServuxLitematicsPayload.TYPE, C2SSevuxLitematicsHandler);
        }
    }

    public static void unregisterReceivers()
    {
        // Do when server stops
        if (ServuxReference.isServer())
        {
            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): isServer() true --> unregisterServuxHandlers()");

            ServerPlayNetworking.unregisterGlobalReceiver(ServuxStructuresPayload.TYPE.id());
            ServerPlayNetworking.unregisterGlobalReceiver(ServuxMetadataPayload.TYPE.id());
            ServerPlayNetworking.unregisterGlobalReceiver(ServuxLitematicsPayload.TYPE.id());
        }
    }
    static
    {
        //C2SSevuxStructuresHandler = ServerNetworkPlayHandler::receiveServuxStructures;
        //C2SSevuxMetadataHandler = ServerNetworkPlayHandler::receiveServuxMetadata;
        //C2SSevuxLitematicsHandler = ServerNetworkPlayHandler::receiveServuxLitematics;
    }
}
