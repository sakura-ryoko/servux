package fi.dy.masa.servux.event;

import fi.dy.masa.malilib.interfaces.IServuxLitematicsListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

public class ServuxLitematicsHandler implements IServuxLitematicsManager
{
    private static final ServuxLitematicsHandler INSTANCE = new ServuxLitematicsHandler();
    private final List<IServuxLitematicsListener> handlers = new ArrayList<>();
    public static IServuxLitematicsManager getInstance() { return INSTANCE; }
    @Override
    public void registerServuxLitematicsHandler(IServuxLitematicsListener handler)
    {
        if (!this.handlers.contains(handler))
        {
            this.handlers.add(handler);
        }
    }
    @Override
    public void unregisterServuxLitematicsHandler(IServuxLitematicsListener handler)
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
            for (IServuxLitematicsListener handler : this.handlers)
            {
                handler.reset();
            }
        }
    }
    public void receiveServuxLitematics(NbtCompound data, ClientPlayNetworking.Context ctx)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxLitematicsListener handler : this.handlers)
            {
                handler.receiveServuxLitematics(data, ctx);
            }
        }
    }

    public void sendServuxLitematics(NbtCompound data)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxLitematicsListener handler : this.handlers)
            {
                handler.sendServuxLitematics(data);
            }
        }
    }
    public void encodeServuxLitematics(NbtCompound data)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxLitematicsListener handler : this.handlers)
            {
                handler.encodeServuxLitematics(data);
            }
        }
    }
    public void decodeServuxLitematics(NbtCompound data)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxLitematicsListener handler : this.handlers)
            {
                handler.decodeServuxLitematics(data);
            }
        }
    }
}
