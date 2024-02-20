package fi.dy.masa.servux.deprecated;

import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.channel.ServuxS2CLitematicsPayload;
import fi.dy.masa.servux.network.payload.channel.ServuxS2CMetadataPayload;
import fi.dy.masa.servux.network.payload.channel.ServuxS2CStructuresPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Version 1 Network API
 */
@Deprecated(forRemoval = true)
public class ServerNetworkPlayRegister
{
    static ServerPlayNetworking.PlayPayloadHandler<ServuxS2CStructuresPayload> C2SSevuxStructuresHandler;
    static ServerPlayNetworking.PlayPayloadHandler<ServuxS2CMetadataPayload> C2SSevuxMetadataHandler;
    static ServerPlayNetworking.PlayPayloadHandler<ServuxS2CLitematicsPayload> C2SSevuxLitematicsHandler;

    public static void registerReceivers() {
        // Do when the server starts, not before
        if (ServuxReference.isServer()) {
            Servux.printDebug("ServerHandlerManager#registerDefaultReceivers(): isServer() true --> registerServuxHandlers()");

            ServerPlayNetworking.registerGlobalReceiver(ServuxS2CStructuresPayload.TYPE, C2SSevuxStructuresHandler);
            ServerPlayNetworking.registerGlobalReceiver(ServuxS2CMetadataPayload.TYPE, C2SSevuxMetadataHandler);
            ServerPlayNetworking.registerGlobalReceiver(ServuxS2CLitematicsPayload.TYPE, C2SSevuxLitematicsHandler);
        }
    }

    public static void unregisterReceivers()
    {
        // Do when server stops
        if (ServuxReference.isServer())
        {
            Servux.printDebug("ServerHandlerManager#unregisterDefaultReceivers(): isServer() true --> unregisterServuxHandlers()");

            ServerPlayNetworking.unregisterGlobalReceiver(ServuxS2CStructuresPayload.TYPE.id());
            ServerPlayNetworking.unregisterGlobalReceiver(ServuxS2CMetadataPayload.TYPE.id());
            ServerPlayNetworking.unregisterGlobalReceiver(ServuxS2CLitematicsPayload.TYPE.id());
        }
    }
    static
    {
        //C2SSevuxStructuresHandler = ServerNetworkPlayHandler::receiveServuxStructures;
        //C2SSevuxMetadataHandler = ServerNetworkPlayHandler::receiveServuxMetadata;
        //C2SSevuxLitematicsHandler = ServerNetworkPlayHandler::receiveServuxLitematics;
    }
}
