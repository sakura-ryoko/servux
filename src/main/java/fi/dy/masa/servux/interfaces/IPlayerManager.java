package fi.dy.masa.servux.interfaces;

public interface IPlayerManager
{
    void registerPlayerHandler(IPlayerListener handler);
    void unregisterPlayerHandler(IPlayerListener handler);
}
