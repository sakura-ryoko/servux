package fi.dy.masa.servux.event;

import net.minecraft.server.MinecraftServer;
import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.network.TestServerHandler;

// TODO --> Might not be required if we are using MaLiLib to do this for us.
public class ServerListener implements IServerListener
{
    @Override
    public void onServerStarting(MinecraftServer server)
    {
        DataProviderManager.INSTANCE.readFromConfig();
        TestServerHandler.getInstance().registerPlayHandler(PayloadType.MALILIB_TEST);
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
