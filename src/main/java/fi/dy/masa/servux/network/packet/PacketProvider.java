package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.event.ServuxLitematicsHandler;
import fi.dy.masa.servux.event.ServuxMetadataHandler;
import fi.dy.masa.servux.event.ServuxStructuresHandler;

public class PacketProvider
{
    static ServuxStructuresListener servuxStructuresListener = new ServuxStructuresListener();
    static ServuxMetadataListener servuxMetadataListener = new ServuxMetadataListener();
    static ServuxLitematicsListener servuxLitematicsListener = new ServuxLitematicsListener();
    private static boolean payloadsRegistered = false;

    public static void registerPayloads()
    {
        if (payloadsRegistered)
            return;
        Servux.printDebug("PacketProvider#registerPayloads(): invoked.");
        ServuxStructuresHandler.getInstance().registerServuxStructuresHandler(servuxStructuresListener);
        ServuxMetadataHandler.getInstance().registerServuxMetadataHandler(servuxMetadataListener);
        ServuxLitematicsHandler.getInstance().registerServuxLitematicsHandler(servuxLitematicsListener);

        payloadsRegistered = true;
    }
    public static void unregisterPayloads()
    {
        Servux.printDebug("PacketProvider#unregisterPayloads(): invoked.");
        ServuxStructuresHandler.getInstance().unregisterServuxStructuresHandler(servuxStructuresListener);
        ServuxMetadataHandler.getInstance().unregisterServuxMetadataHandler(servuxMetadataListener);
        ServuxLitematicsHandler.getInstance().unregisterServuxLitematicsHandler(servuxLitematicsListener);

        payloadsRegistered = false;
    }
}
