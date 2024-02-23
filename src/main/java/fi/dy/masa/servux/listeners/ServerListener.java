package fi.dy.masa.servux.listeners;

import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.malilib.network.payload.PayloadTypeRegister;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.network.PacketUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerStarting(MinecraftServer minecraftServer)
    {
        if (minecraftServer.isSingleplayer())
        {
            ServuxReference.setOpenToLan(false);
            ServuxReference.setDedicated(false);
            Servux.logger.info("Servux is running under Single Player mode.");
        }
        else if (minecraftServer.isDedicated())
        {
            ServuxReference.setOpenToLan(false);
            ServuxReference.setIntegrated(false);
            ServuxReference.setDedicated(true);
            Servux.logger.info("Servux is running under Dedicated Server mode.");
        }
        PacketUtils.registerPayloads();
        Servux.printDebug("MinecraftServerEvents#onServerStarting(): invoked.");
    }
    @Override
    public void onServerStarted(MinecraftServer minecraftServer)
    {
        PayloadTypeRegister.getInstance().registerAllHandlers();

        //ServerDebugSuite.checkGlobalPlayChannels();
        //ServerDebugSuite.checkGlobalConfigChannels();

        Servux.printDebug("MinecraftServerEvents#onServerStarted(): invoked.");
    }
    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        Servux.logger.info("Servux Integrated Server Mode detected.");
        ServuxReference.setOpenToLan(false);
        ServuxReference.setDedicated(false);
        ServuxReference.setIntegrated(true);
    }
    @Override
    public void onServerOpenToLan(IntegratedServer server)
    {
        Servux.logger.info("Servux OpenToLan Mode detected.");
        ServuxReference.setOpenToLan(true);
        ServuxReference.setIntegrated(true);
        ServuxReference.setDedicated(false);
    }

    @Override
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        PayloadTypeRegister.getInstance().resetPayloads();

        //ServerDebugSuite.checkGlobalPlayChannels();
        //ServerDebugSuite.checkGlobalConfigChannels();

        Servux.printDebug("MinecraftServerEvents#onServerStopping(): invoked.");
    }
    @Override
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        //ServerDebugSuite.checkGlobalPlayChannels();
        Servux.printDebug("MinecraftServerEvents#onServerStopped(): invoked.");
        ServuxReference.setOpenToLan(false);
        ServuxReference.setIntegrated(false);
        ServuxReference.setDedicated(false);
    }
}
