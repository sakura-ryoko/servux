package fi.dy.masa.servux.listeners;

import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.malilib.network.payload.PayloadTypeRegister;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.network.PacketListenerRegister;
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
            Servux.logger.info("[{}] Single Player mode detected", ServuxReference.MOD_ID);
        }
        else if (server.isDedicated())
        {
            ServuxReference.setOpenToLan(false);
            ServuxReference.setIntegrated(false);
            ServuxReference.setDedicated(true);
            Servux.logger.info("[{}] Dedicated Server Mode detected", ServuxReference.MOD_ID);
        }
        PacketListenerRegister.registerListeners();
    }
    @Override
    public void onServerStarted(MinecraftServer minecraftServer)
    {
        PayloadTypeRegister.getInstance().registerAllHandlers();
    }
    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        Servux.logger.info("[{}] integrated Server Mode detected", ServuxReference.MOD_ID);
        ServuxReference.setOpenToLan(false);
        ServuxReference.setDedicated(false);
        ServuxReference.setIntegrated(true);
    }
    @Override
    public void onServerOpenToLan(IntegratedServer server)
    {
        Servux.logger.info("[{}] OpenToLan Mode detected [Serving on localhost:{}]", ServuxReference.MOD_ID, server.getServerPort());
        ServuxReference.setOpenToLan(true);
        ServuxReference.setIntegrated(true);
        ServuxReference.setDedicated(false);
    }

    @Override
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        Servux.logger.info("[{}] server is stopping", ServuxReference.MOD_ID);
        PayloadTypeRegister.getInstance().resetPayloads();
    }
    @Override
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        Servux.logger.info("[{}] server has stopped", ServuxReference.MOD_ID);
        ServuxReference.setOpenToLan(false);
        ServuxReference.setIntegrated(false);
        ServuxReference.setDedicated(false);
    }
}
