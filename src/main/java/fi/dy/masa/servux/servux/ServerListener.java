package fi.dy.masa.servux.servux;

import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.HudDataProvider;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import fi.dy.masa.servux.interfaces.IServerListener;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerStarting(MinecraftServer server)
    {
        DataProviderManager.INSTANCE.readFromConfig();
    }

    @Override
    public void onServerStarted(MinecraftServer server)
    {
        DataProviderManager.INSTANCE.writeToConfig();
        DataProviderManager.INSTANCE.onCaptureImmutable(server.getRegistryManager());

        if (HudDataProvider.INSTANCE.isEnabled())
        {
            HudDataProvider.INSTANCE.checkWorldSeed(server);
        }
    }

    @Override
    public void onServerResourceReloadPre(MinecraftServer server, ResourceManager resourceManager)
    {
        DataProviderManager.INSTANCE.readFromConfig();
    }

    @Override
    public void onServerResourceReloadPost(MinecraftServer server, ResourceManager resourceManager, boolean success)
    {
        DataProviderManager.INSTANCE.writeToConfig();
        DataProviderManager.INSTANCE.onCaptureImmutable(server.getRegistryManager());
//        i18nLang.tryLoadLanguage(ServuxConfigProvider.INSTANCE.getDefaultLanguage());
        ServuxConfigProvider.INSTANCE.registerHandler();
    }

    @Override
    public void onServerStopping(MinecraftServer server)
    {
        DataProviderManager.INSTANCE.onServerTickEndPre();
        DataProviderManager.INSTANCE.writeToConfig();
    }

    @Override
    public void onServerStopped(MinecraftServer server)
    {
        DataProviderManager.INSTANCE.onServerTickEndPost();
    }
}
