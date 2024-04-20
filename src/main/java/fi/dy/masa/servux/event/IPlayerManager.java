package fi.dy.masa.servux.event;

import fi.dy.masa.servux.interfaces.IPlayerListener;

public interface IPlayerManager
{
    void registerPlayerHandler(IPlayerListener handler);
    void unregisterPlayerHandler(IPlayerListener handler);
}
