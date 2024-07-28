package fi.dy.masa.servux.event;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import fi.dy.masa.servux.interfaces.IServerInitDispatcher;
import fi.dy.masa.servux.interfaces.IServerInitHandler;

public class ServerInitHandler implements IServerInitDispatcher
{
    private static final ServerInitHandler INSTANCE = new ServerInitHandler();
    public static IServerInitDispatcher getInstance() { return INSTANCE; }
    private final List<IServerInitHandler> handlers = new ArrayList<>();

    @Override
    public void registerServerInitHandler(IServerInitHandler handler)
    {
        if (this.handlers.contains(handler) == false)
        {
            this.handlers.add(handler);
        }
    }

    @ApiStatus.Internal
    public void onServerInit()
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IServerInitHandler handler : this.handlers)
            {
                handler.onServerInit();
            }
        }
    }
}
