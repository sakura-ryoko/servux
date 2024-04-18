package fi.dy.masa.servux.event;

import net.minecraft.server.MinecraftServer;
import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.servux.dataproviders.DataProviderManager;

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
        // NO-OP
    }

    @Override
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        DataProviderManager.INSTANCE.writeToConfig();
    }
}
