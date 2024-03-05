package fi.dy.masa.servux.network;

import fi.dy.masa.malilib.network.handler.server.ServerPlayHandler;
import fi.dy.masa.malilib.network.payload.channel.*;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.listeners.ServuxStructuresPlayListener;

public class PacketListenerRegister
{
    static ServuxStructuresPlayListener<ServuxStructuresPayload> servuxStructuresListener = ServuxStructuresPlayListener.INSTANCE;
    private static boolean payloadsRegistered = false;

    public static void registerListeners()
    {
        if (payloadsRegistered)
            return;
        Servux.printDebug("PacketListenerRegister#registerPayloads(): invoked.");
        ServerPlayHandler.getInstance().registerServerPlayHandler(servuxStructuresListener);

        payloadsRegistered = true;
    }
    public static void unregisterListeners()
    {
        Servux.printDebug("PacketListenerRegister#unregisterPayloads(): invoked.");
        ServerPlayHandler.getInstance().unregisterServerPlayHandler(servuxStructuresListener);

        payloadsRegistered = false;
    }
}
