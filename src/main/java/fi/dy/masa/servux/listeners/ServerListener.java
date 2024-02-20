package fi.dy.masa.servux.listeners;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.interfaces.IServerListener;
import fi.dy.masa.servux.network.packet.PacketUtils;
import fi.dy.masa.servux.network.payload.PayloadTypeRegister;
import fi.dy.masa.servux.network.test.ServerDebugSuite;
import net.minecraft.server.MinecraftServer;

public class ServerListener implements IServerListener
{
    public void onServerStarting(MinecraftServer minecraftServer)
    {
        if (minecraftServer.isSingleplayer())
        {
            ServuxReference.setOpenToLan(false);
            ServuxReference.setDedicated(false);
        }
        else if (minecraftServer.isDedicated())
        {
            ServuxReference.setOpenToLan(false);
            ServuxReference.setDedicated(true);
        }
        PacketUtils.registerPayloads();
        Servux.printDebug("MinecraftServerEvents#onServerStarting(): invoked.");
    }
    public void onServerStarted(MinecraftServer minecraftServer)
    {
        PayloadTypeRegister.getInstance().registerAllHandlers();

        ServerDebugSuite.checkGlobalPlayChannels();
        ServerDebugSuite.checkGlobalConfigChannels();

        Servux.printDebug("MinecraftServerEvents#onServerStarted(): invoked.");
    }
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        PayloadTypeRegister.getInstance().resetPayloads();

        ServerDebugSuite.checkGlobalPlayChannels();
        ServerDebugSuite.checkGlobalConfigChannels();
        Servux.printDebug("MinecraftServerEvents#onServerStopping(): invoked.");
    }
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        ServerDebugSuite.checkGlobalPlayChannels();
        Servux.printDebug("MinecraftServerEvents#onServerStopped(): invoked.");
    }
}
