package fi.dy.masa.servux.util;

public interface IWorldUpdateSuppressor
{
    boolean servux_getShouldPreventBlockUpdates();

    void servux_setShouldPreventBlockUpdates(boolean preventUpdates);
}
