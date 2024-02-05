package fi.dy.masa.servux.event;

import fi.dy.masa.malilib.interfaces.IServuxMetadataListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

public class ServuxMetadataHandler implements IServuxMetadataManager
{
    private static final ServuxMetadataHandler INSTANCE = new ServuxMetadataHandler();
    private final List<IServuxMetadataListener> handlers = new ArrayList<>();
    public static IServuxMetadataManager getInstance() { return INSTANCE; }
    @Override
    public void registerServuxMetadataHandler(IServuxMetadataListener handler)
    {
        if (!this.handlers.contains(handler))
        {
            this.handlers.add(handler);
        }
    }
    @Override
    public void unregisterServuxMetadataHandler(IServuxMetadataListener handler)
    {
        this.handlers.remove(handler);
    }

    /**
     * NOT PUBLIC API - DO NOT CALL
     */
    public void reset()
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxMetadataListener handler : this.handlers)
            {
                handler.reset();
            }
        }
    }
    public void receiveServuxMetadata(NbtCompound data, ClientPlayNetworking.Context ctx)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxMetadataListener handler : this.handlers)
            {
                handler.receiveServuxMetadata(data, ctx);
            }
        }
    }

    public void sendServuxMetadata(NbtCompound data)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxMetadataListener handler : this.handlers)
            {
                handler.sendServuxMetadata(data);
            }
        }
    }
    public void encodeServuxMetadata(NbtCompound data)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxMetadataListener handler : this.handlers)
            {
                handler.encodeServuxMetadata(data);
            }
        }
    }
    public void decodeServuxMetadata(NbtCompound data)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxMetadataListener handler : this.handlers)
            {
                handler.decodeServuxMetadata(data);
            }
        }
    }
}
