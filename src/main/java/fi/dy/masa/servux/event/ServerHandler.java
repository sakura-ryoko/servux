package fi.dy.masa.servux.event;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.servux.interfaces.IServerListener;

/**
 * Interface Handler for Server loading / unloading events --> similar to WorldLoadHandler,
 * but it only executes once at the proper time to register packet receivers, etc.
 */
public class ServerHandler implements IServerManager
{
    private static final ServerHandler INSTANCE = new ServerHandler();
    private final List<IServerListener> handlers = new ArrayList<>();
    public static IServerManager getInstance() { return INSTANCE; }

    @Override
    public void registerServerHandler(IServerListener handler)
    {
        if (!this.handlers.contains(handler))
        {
            this.handlers.add(handler);
        }
    }

    @Override
    public void unregisterServerHandler(IServerListener handler)
    {
        this.handlers.remove(handler);
    }

    /**
     * NOT PUBLIC API - DO NOT CALL
     */
    public void onServerStarting(MinecraftServer server)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServerListener handler : this.handlers)
            {
                handler.onServerStarting(server);
            }
        }
    }

    /**
     * NOT PUBLIC API - DO NOT CALL
     */
    public void onServerStarted(MinecraftServer server)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServerListener handler : this.handlers)
            {
                handler.onServerStarted(server);
            }
        }
    }

    /**
     * NOT PUBLIC API - DO NOT CALL
     */
    public void onServerStopping(MinecraftServer server)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServerListener handler : this.handlers)
            {
                handler.onServerStopping(server);
            }
        }
    }

    /**
     * NOT PUBLIC API - DO NOT CALL
     */
    public void onServerStopped(MinecraftServer server)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServerListener handler : this.handlers)
            {
                handler.onServerStopped(server);
            }
        }
    }
}
