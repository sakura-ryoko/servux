package fi.dy.masa.servux.listeners;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.interfaces.IServerListener;
import fi.dy.masa.servux.network.ServerNetworkPlayInitHandler;
import fi.dy.masa.servux.network.packet.PacketProvider;
import fi.dy.masa.servux.network.test.ServerDebugSuite;
import net.minecraft.server.MinecraftServer;

public class ServerListener implements IServerListener
{
    public void onServerStarting(MinecraftServer minecraftServer)
    {
        ServerNetworkPlayInitHandler.registerPlayChannels();
        PacketProvider.registerPayloads();
        ServerDebugSuite.checkGlobalChannels();
        Servux.printDebug("MinecraftServerEvents#onServerStarting(): invoked.");
    }
    public void onServerStarted(MinecraftServer minecraftServer)
    {
        ServerNetworkPlayInitHandler.registerReceivers();
        ServerDebugSuite.checkGlobalChannels();
        Servux.printDebug("MinecraftServerEvents#onServerStarted(): invoked.");
    }
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        ServerDebugSuite.checkGlobalChannels();
        Servux.printDebug("MinecraftServerEvents#onServerStopping(): invoked.");
    }
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        ServerNetworkPlayInitHandler.unregisterReceivers();
        PacketProvider.unregisterPayloads();
        ServerDebugSuite.checkGlobalChannels();
        Servux.printDebug("MinecraftServerEvents#onServerStopped(): invoked.");
    }
}
