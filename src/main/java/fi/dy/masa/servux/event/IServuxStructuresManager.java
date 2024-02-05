package fi.dy.masa.servux.event;

import fi.dy.masa.malilib.interfaces.IServuxStructuresListener;

public interface IServuxStructuresManager
{
    /**
     * Registers a handler for receiving Carpet Hello NBTCompound packets.
     */
    void registerServuxStructuresHandler(IServuxStructuresListener handler);

    /**
     * Un-Registers a handler for receiving Carpet Hello NBTCompound packets.
     */
    void unregisterServuxStructuresHandler(IServuxStructuresListener handler);
}
