package fi.dy.masa.servux;

import fi.dy.masa.servux.event.PlayerHandler;
import fi.dy.masa.servux.event.ServerHandler;
import fi.dy.masa.servux.listeners.PlayerListener;
import fi.dy.masa.servux.listeners.ServerListener;
import fi.dy.masa.servux.network.ServerNetworkPlayInitHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import net.fabricmc.api.ModInitializer;

public class Servux implements ModInitializer
{
    public static final Logger logger = LogManager.getLogger(ServuxReference.MOD_ID);

    @Override
    public void onInitialize()
    {
        ServerNetworkPlayInitHandler.registerPlayChannels();
        DataProviderManager.INSTANCE.registerDataProvider(StructureDataProvider.INSTANCE);
        //DataProviderManager.INSTANCE.registerDataProvider(MetaDataProvider.INSTANCE);
        //DataProviderManager.INSTANCE.registerDataProvider(LitematicsDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.readFromConfig();

        // Init Handler's
        ServerListener serverListener = new ServerListener();
        ServerHandler.getInstance().registerServerHandler(serverListener);
        PlayerListener playerListener = new PlayerListener();
        PlayerHandler.getInstance().registerPlayerHandler(playerListener);
    }

    public static String getModVersionString(String modId)
    {
        for (net.fabricmc.loader.api.ModContainer container : net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods())
        {
            if (container.getMetadata().getId().equals(modId))
            {
                return container.getMetadata().getVersion().getFriendlyString();
            }
        }

        return "?";
    }
    public static void printDebug(String key, Object... args)
    {
        if (ServuxReference.MOD_DEBUG)
        {
            logger.info(key, args);
        }
    }
}
