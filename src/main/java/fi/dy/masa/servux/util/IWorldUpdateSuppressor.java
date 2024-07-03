package fi.dy.masa.servux.util;

public interface IWorldUpdateSuppressor
{
    boolean litematica_getShouldPreventBlockUpdates();

    void litematica_setShouldPreventBlockUpdates(boolean preventUpdates);
}
