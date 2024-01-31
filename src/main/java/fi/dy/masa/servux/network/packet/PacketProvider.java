package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.event.ServuxPayloadHandler;

public class PacketProvider
{
    static ServuxPayloadListener servuxListener = new ServuxPayloadListener();
    //static SyncmaticaPayloadServerListener syncmaticaServerListener = new SyncmaticaPayloadServerListener();
    private static boolean payloadsRegistered = false;

    public static void registerPayloads()
    {
        if (payloadsRegistered)
            return;
        Servux.printDebug("PacketProvider#registerPayloads(): invoked.");
        ServuxPayloadHandler.getInstance().registerServuxHandler(servuxListener);
        //SyncmaticaPayloadServerHandler.getInstance().registerSyncmaticaServerHandler(syncmaticaServerListener);

        payloadsRegistered = true;
    }
    public static void unregisterPayloads()
    {
        Servux.printDebug("PacketProvider#unregisterPayloads(): invoked.");
        ServuxPayloadHandler.getInstance().unregisterServuxHandler(servuxListener);
        //SyncmaticaPayloadServerHandler.getInstance().unregisterSyncmaticaServerHandler(syncmaticaServerListener);

        payloadsRegistered = false;
    }
}
