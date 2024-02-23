package fi.dy.masa.servux.network;

import fi.dy.masa.malilib.network.handler.play.ServerPlayHandler;
import fi.dy.masa.malilib.network.payload.channel.*;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.listeners.ServuxStructuresPlayListener;

public class PacketUtils
{
    static ServuxStructuresPlayListener<ServuxStructuresPayload> servuxStructuresListener = ServuxStructuresPlayListener.INSTANCE;
    private static boolean payloadsRegistered = false;

    public static void registerPayloads()
    {
        if (payloadsRegistered)
            return;
        Servux.printDebug("PacketUtils#registerPayloads(): invoked.");
        ServerPlayHandler.getInstance().registerServerPlayHandler(servuxStructuresListener);

        payloadsRegistered = true;
    }
    public static void unregisterPayloads()
    {
        Servux.printDebug("PacketUtils#unregisterPayloads(): invoked.");
        ServerPlayHandler.getInstance().unregisterServerPlayHandler(servuxStructuresListener);

        payloadsRegistered = false;
    }
}
