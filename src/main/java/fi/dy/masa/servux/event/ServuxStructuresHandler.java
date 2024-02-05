package fi.dy.masa.servux.event;

import fi.dy.masa.malilib.interfaces.IServuxStructuresListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

public class ServuxStructuresHandler implements IServuxStructuresManager
{
    private static final ServuxStructuresHandler INSTANCE = new ServuxStructuresHandler();
    private final List<IServuxStructuresListener> handlers = new ArrayList<>();
    public static IServuxStructuresManager getInstance() { return INSTANCE; }
    @Override
    public void registerServuxStructuresHandler(IServuxStructuresListener handler)
    {
        if (!this.handlers.contains(handler))
        {
            this.handlers.add(handler);
        }
    }
    @Override
    public void unregisterServuxStructuresHandler(IServuxStructuresListener handler)
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
            for (IServuxStructuresListener handler : this.handlers)
            {
                handler.reset();
            }
        }
    }
    public void receiveServuxStructures(NbtCompound data, ClientPlayNetworking.Context ctx)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxStructuresListener handler : this.handlers)
            {
                handler.receiveServuxStructures(data, ctx);
            }
        }
    }

    public void sendServuxStructures(NbtCompound data)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxStructuresListener handler : this.handlers)
            {
                handler.sendServuxStructures(data);
            }
        }
    }
    public void encodeServuxStructures(NbtCompound data)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxStructuresListener handler : this.handlers)
            {
                handler.encodeServuxStructures(data);
            }
        }
    }
    public void decodeServuxStructures(NbtCompound data)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxStructuresListener handler : this.handlers)
            {
                handler.decodeServuxStructures(data);
            }
        }
    }
}
