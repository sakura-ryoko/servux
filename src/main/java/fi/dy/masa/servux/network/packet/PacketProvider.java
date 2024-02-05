package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.event.ServuxPayloadHandler;

public class PacketProvider
{
    static ServuxPayloadListener servuxListener = new ServuxPayloadListener();
    private static boolean payloadsRegistered = false;

    public static void registerPayloads()
    {
        if (payloadsRegistered)
            return;
        Servux.printDebug("PacketProvider#registerPayloads(): invoked.");
        ServuxPayloadHandler.getInstance().registerServuxHandler(servuxListener);

        payloadsRegistered = true;
    }
    public static void unregisterPayloads()
    {
        Servux.printDebug("PacketProvider#unregisterPayloads(): invoked.");
        ServuxPayloadHandler.getInstance().unregisterServuxHandler(servuxListener);

        payloadsRegistered = false;
    }
}
