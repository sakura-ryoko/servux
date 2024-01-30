package fi.dy.masa.servux.event;

import fi.dy.masa.servux.interfaces.ISyncmaticaPayloadServerListener;

public interface ISyncmaticaPayloadServerManager
{
    /**
     * Registers a handler for receiving Carpet Hello NBTCompound packets.
     * @param handler
     */
    void registerSyncmaticaServerHandler(ISyncmaticaPayloadServerListener handler);

    /**
     * Un-Registers a handler for receiving Carpet Hello NBTCompound packets.
     * @param handler
     */
    void unregisterSyncmaticaServerHandler(ISyncmaticaPayloadServerListener handler);
}
