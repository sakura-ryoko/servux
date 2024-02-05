package fi.dy.masa.servux.event;

import fi.dy.masa.malilib.interfaces.IServuxLitematicsListener;

public interface IServuxLitematicsManager
{
    /**
     * Registers a handler for receiving Carpet Hello NBTCompound packets.
     */
    void registerServuxLitematicsHandler(IServuxLitematicsListener handler);

    /**
     * Un-Registers a handler for receiving Carpet Hello NBTCompound packets.
     */
    void unregisterServuxLitematicsHandler(IServuxLitematicsListener handler);
}
