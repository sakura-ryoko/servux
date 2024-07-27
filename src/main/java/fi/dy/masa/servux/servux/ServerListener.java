package fi.dy.masa.servux.servux;

import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import fi.dy.masa.servux.util.i18nLang;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
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

        i18nLang.tryLoadLanguage(ServuxConfigProvider.INSTANCE.getDefaultLanguage());
    }

    @Override
    public void onServerStopping(MinecraftServer server)
    {
        DataProviderManager.INSTANCE.onServerTickEndPre();
    }

    @Override
    public void onServerStopped(MinecraftServer server)
    {
        DataProviderManager.INSTANCE.onServerTickEndPost();
    }
}
