package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.event.ServuxPayloadHandler;

public class PacketProvider
{
    static ServuxPayloadListener servuxListener = new ServuxPayloadListener();
    public static void registerPayloads()
    {
        ServuxPayloadHandler.getInstance().registerServuxHandler(servuxListener);
    }
    public static void unregisterPayloads()
    {
        ServuxPayloadHandler.getInstance().unregisterServuxHandler(servuxListener);
    }
}
