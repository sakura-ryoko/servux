package fi.dy.masa.servux.event;

import fi.dy.masa.servux.interfaces.IServerListener;

public interface IServerManager
{
    void registerServerHandler(IServerListener handler);
    void unregisterServerHandler(IServerListener handler);
}
