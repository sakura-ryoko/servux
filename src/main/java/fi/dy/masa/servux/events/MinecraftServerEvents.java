package fi.dy.masa.servux.events;

import fi.dy.masa.servux.network.ServerNetworkPlayRegister;
import fi.dy.masa.servux.network.test.ServerDebugSuite;
import net.minecraft.server.MinecraftServer;

public class MinecraftServerEvents
{
    public static void onServerStarting(MinecraftServer minecraftServer)
    {
        ServerDebugSuite.checkGlobalChannels();
    }
    public static void onServerStarted(MinecraftServer minecraftServer)
    {
        ServerNetworkPlayRegister.registerDefaultReceivers();
        ServerDebugSuite.checkGlobalChannels();
    }
    public static void onServerStopping(MinecraftServer minecraftServer)
    {
        ServerNetworkPlayRegister.unregisterDefaultReceivers();
        ServerDebugSuite.checkGlobalChannels();
    }
    public static void onServerStopped(MinecraftServer minecraftServer)
    {
        ServerDebugSuite.checkGlobalChannels();
    }
}
