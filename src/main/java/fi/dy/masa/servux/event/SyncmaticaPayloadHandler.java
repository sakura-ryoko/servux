package fi.dy.masa.servux.event;

import fi.dy.masa.servux.interfaces.ISyncmaticaPayloadListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class SyncmaticaPayloadHandler implements ISyncmaticaPayloadManager
{
    private static final SyncmaticaPayloadHandler INSTANCE = new SyncmaticaPayloadHandler();
    private final List<ISyncmaticaPayloadListener> handlers = new ArrayList<>();
    public static ISyncmaticaPayloadManager getInstance() { return INSTANCE; }
    @Override
    public void registerSyncmaticaHandler(ISyncmaticaPayloadListener handler)
    {
        if (!this.handlers.contains(handler))
        {
            this.handlers.add(handler);
        }
    }
    @Override
    public void unregisterSyncmaticaHandler(ISyncmaticaPayloadListener handler)
    {
        this.handlers.remove(handler);
    }

    /**
     * NOT PUBLIC API - DO NOT CALL
     */
    public void receiveSyncmaticaPayload(NbtCompound data, ServerPlayNetworking.Context ctx)
    {
        if (!this.handlers.isEmpty())
        {
            for (ISyncmaticaPayloadListener handler : this.handlers)
            {
                handler.receiveSyncmaticaPayload(data, ctx);
            }
        }
    }
    public void sendSyncmaticaPayload(NbtCompound data, ServerPlayerEntity player)
    {
        if (!this.handlers.isEmpty())
        {
            for (ISyncmaticaPayloadListener handler : this.handlers)
            {
                handler.sendSyncmaticaPayload(data, player);
            }
        }
    }
    public void encodeSyncmaticaPayload(NbtCompound data, ServerPlayerEntity player)
    {
        if (!this.handlers.isEmpty())
        {
            for (ISyncmaticaPayloadListener handler : this.handlers)
            {
                handler.encodeSyncmaticaPayload(data, player);
            }
        }
    }
    public void decodeSyncmaticaPayload(NbtCompound data, ServerPlayerEntity player)
    {
        if (!this.handlers.isEmpty())
        {
            for (ISyncmaticaPayloadListener handler : this.handlers)
            {
                handler.decodeSyncmaticaPayload(data, player);
            }
        }
    }
}
