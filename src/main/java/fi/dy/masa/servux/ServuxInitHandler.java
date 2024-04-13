package fi.dy.masa.servux;

import fi.dy.masa.malilib.event.PlayerHandler;
import fi.dy.masa.malilib.event.ServerHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.network.handler.server.ServerPlayHandler;
import fi.dy.masa.malilib.network.payload.PayloadManager;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.channel.ServuxStructuresPayload;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.event.PlayerListener;
import fi.dy.masa.servux.event.ServerListener;
import fi.dy.masa.servux.network.ServuxStructuresPlayListener;

/**
 * This file assumes that ServuX is best served being Multi-Environment with MaLiLib,
 * Might as well use InitHandler.
 */
public class ServuxInitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        DataProviderManager.INSTANCE.registerDataProvider(StructureDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.readFromConfig();

        ServerListener serverListener = new ServerListener();
        ServerHandler.getInstance().registerServerHandler(serverListener);
        PlayerListener playerListener = new PlayerListener();
        PlayerHandler.getInstance().registerPlayerHandler(playerListener);

        PayloadManager.getInstance().register(PayloadType.SERVUX_STRUCTURES, "servux", "structures");
        ServuxStructuresPlayListener<ServuxStructuresPayload> servuxStructuresListener = ServuxStructuresPlayListener.getInstance();
        ServerPlayHandler.getInstance().registerServerPlayHandler(servuxStructuresListener);
    }
}
