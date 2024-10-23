package fi.dy.masa.servux.servux;

import fi.dy.masa.servux.dataproviders.*;
import fi.dy.masa.servux.event.PlayerHandler;
import fi.dy.masa.servux.event.ServerHandler;
import fi.dy.masa.servux.interfaces.IServerInitHandler;

public class ServuxInitHandler implements IServerInitHandler
{
    @Override
    public void onServerInit()
    {
        DataProviderManager.INSTANCE.registerDataProvider(ServuxConfigProvider.INSTANCE);
        DataProviderManager.INSTANCE.registerDataProvider(StructureDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.registerDataProvider(HudDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.registerDataProvider(LitematicsDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.registerDataProvider(EntitiesDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.registerDataProvider(TweaksDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.registerDataProvider(DebugDataProvider.INSTANCE);

        ServerHandler.getInstance().registerServerHandler(new ServerListener());
        PlayerHandler.getInstance().registerPlayerHandler(new PlayerListener());
    }
}
