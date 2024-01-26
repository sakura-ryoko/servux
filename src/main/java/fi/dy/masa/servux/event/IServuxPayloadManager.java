package fi.dy.masa.servux.event;

import fi.dy.masa.servux.interfaces.IServuxPayloadListener;

public interface IServuxPayloadManager
{
    /**
     * Registers a handler for receiving Carpet Hello NBTCompound packets.
     * @param handler
     */
    void registerServuxHandler(IServuxPayloadListener handler);

    /**
     * Un-Registers a handler for receiving Carpet Hello NBTCompound packets.
     * @param handler
     */
    void unregisterServuxHandler(IServuxPayloadListener handler);
}
