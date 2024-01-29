package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.event.ServuxPayloadHandler;
import fi.dy.masa.servux.event.SyncmaticaPayloadHandler;

public class PacketProvider
{
    static ServuxPayloadListener servuxListener = new ServuxPayloadListener();
    static SyncmaticaPayloadListener syncmaticaListener = new SyncmaticaPayloadListener();
    public static void registerPayloads()
    {
        ServuxPayloadHandler.getInstance().registerServuxHandler(servuxListener);
        SyncmaticaPayloadHandler.getInstance().registerSyncmaticaHandler(syncmaticaListener);
    }
    public static void unregisterPayloads()
    {
        ServuxPayloadHandler.getInstance().unregisterServuxHandler(servuxListener);
        SyncmaticaPayloadHandler.getInstance().unregisterSyncmaticaHandler(syncmaticaListener);
    }
}
