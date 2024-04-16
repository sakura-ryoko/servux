package fi.dy.masa.servux.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.malilib.network.NetworkReference;
import fi.dy.masa.malilib.network.payload.PayloadManager;
import fi.dy.masa.servux.dataproviders.DataProviderManager;

// TODO --> Might not be required if we are using MaLiLib to do this for us.
public class ServerListener implements IServerListener
{
    @Override
    public void onServerStarting(MinecraftServer server)
    {
        if (server.isSingleplayer())
        {
            NetworkReference.getInstance().setOpenToLan(false);
            NetworkReference.getInstance().setDedicated(false);
        }
        else if (server.isDedicated())
        {
            NetworkReference.getInstance().setOpenToLan(false);
            NetworkReference.getInstance().setDedicated(true);
        }
        DataProviderManager.INSTANCE.readFromConfig();
    }

    @Override
    public void onServerStarted(MinecraftServer server)
    {
        PayloadManager.getInstance().registerHandlers();
    }

    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        NetworkReference.getInstance().setOpenToLan(false);
        NetworkReference.getInstance().setDedicated(false);
    }
    @Override
    public void onServerOpenToLan(IntegratedServer server)
    {
        NetworkReference.getInstance().setOpenToLan(true);
        NetworkReference.getInstance().setDedicated(false);
    }

    @Override
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        PayloadManager.getInstance().resetPayloads();
        DataProviderManager.INSTANCE.writeToConfig();
    }

    @Override
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        NetworkReference.getInstance().setOpenToLan(false);
        NetworkReference.getInstance().setDedicated(false);
    }
}
