package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.handlers.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.listeners.ServuxStructuresPlayListener;
import fi.dy.masa.servux.network.payload.channel.ServuxS2CStructuresPayload;

public class PacketUtils
{
    static ServuxStructuresPlayListener<ServuxS2CStructuresPayload> servuxStructuresListener = ServuxStructuresPlayListener.INSTANCE;
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
