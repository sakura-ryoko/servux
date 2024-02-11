package fi.dy.masa.servux.deprecated;

import fi.dy.masa.servux.network.test.ServerDebugSuite;

@Deprecated
public class ServerNetworkPlayInitHandler
{
    /**
     * Should be called when Server is starting
     */
    public static void registerPlayChannels()
    {
        //PayloadTypeRegister.registerTypes(ServuxReference.COMMON_NAMESPACE);
        //PayloadTypeRegister.registerPlayChannels();
        ServerDebugSuite.checkGlobalPlayChannels();
    }
    /**
     * Should be called when Client joins a server
     */
    public static void registerReceivers()
    {
        //ServerNetworkPlayRegister.registerReceivers();
        ServerDebugSuite.checkGlobalPlayChannels();
    }
    public static void unregisterReceivers()
    {
        //ServerNetworkPlayRegister.unregisterReceivers();
        ServerDebugSuite.checkGlobalPlayChannels();
    }
}
