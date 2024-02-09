package fi.dy.masa.servux.event;


import fi.dy.masa.servux.interfaces.IServuxMetadataListener;

public interface IServuxMetadataManager
{
    /**
     * Registers a handler for receiving Carpet Hello NBTCompound packets.
     */
    void registerServuxMetadataHandler(IServuxMetadataListener handler);

    /**
     * Un-Registers a handler for receiving Carpet Hello NBTCompound packets.
     */
    void unregisterServuxMetadataHandler(IServuxMetadataListener handler);
}
