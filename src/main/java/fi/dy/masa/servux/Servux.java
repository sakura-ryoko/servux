package fi.dy.masa.servux;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fi.dy.masa.servux.dataproviders.*;
import fi.dy.masa.servux.event.PlayerHandler;
import fi.dy.masa.servux.event.PlayerListener;
import fi.dy.masa.servux.event.ServerHandler;
import fi.dy.masa.servux.event.ServerListener;

public class Servux implements ModInitializer
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitialize()
    {
        DataProviderManager.INSTANCE.registerDataProvider(LitematicsDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.registerDataProvider(EntitiesDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.registerDataProvider(StructureDataProvider.INSTANCE);
        //DataProviderManager.INSTANCE.registerDataProvider(TweaksDataProvider.INSTANCE);

        ServerListener serverListener = new ServerListener();
        ServerHandler.getInstance().registerServerHandler(serverListener);

        PlayerListener playerListener = new PlayerListener();
        PlayerHandler.getInstance().registerPlayerHandler(playerListener);
    }
}
