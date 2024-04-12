package fi.dy.masa.servux.event;

import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.malilib.network.payload.PayloadManager;
import fi.dy.masa.servux.ServuxReference;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerStarting(MinecraftServer server)
    {
        if (server.isSingleplayer())
        {
            ServuxReference.setOpenToLan(false);
            ServuxReference.setDedicated(false);
        }
        else if (server.isDedicated())
        {
            ServuxReference.setOpenToLan(false);
            ServuxReference.setDedicated(true);
        }
    }

    @Override
    public void onServerStarted(MinecraftServer server)
    {
        PayloadManager.getInstance().registerAllHandlers();
    }

    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        ServuxReference.setOpenToLan(false);
        ServuxReference.setDedicated(false);
    }
    @Override
    public void onServerOpenToLan(IntegratedServer server)
    {
        ServuxReference.setOpenToLan(true);
        ServuxReference.setDedicated(false);
    }

    @Override
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        PayloadManager.getInstance().resetPayloads();
    }

    @Override
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        ServuxReference.setOpenToLan(false);
        ServuxReference.setDedicated(false);
    }
}
