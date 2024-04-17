package fi.dy.masa.servux;

import fi.dy.masa.malilib.event.PlayerHandler;
import fi.dy.masa.malilib.event.ServerHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.event.PlayerListener;
import fi.dy.masa.servux.event.ServerListener;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        DataProviderManager.INSTANCE.registerDataProvider(StructureDataProvider.INSTANCE);

        ServerListener serverListener = new ServerListener();
        ServerHandler.getInstance().registerServerHandler(serverListener);

        PlayerListener playerListener = new PlayerListener();
        PlayerHandler.getInstance().registerPlayerHandler(playerListener);
    }
}
