package fi.dy.masa.servux.event;

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
    public void onServerStopping(MinecraftServer server)
    {
        DataProviderManager.INSTANCE.writeToConfig();
    }
}
