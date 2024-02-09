package fi.dy.masa.servux.event;

import fi.dy.masa.servux.interfaces.IServuxLitematicsListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

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
    public void receiveServuxLitematics(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxLitematicsListener handler : this.handlers)
            {
                handler.receiveServuxLitematics(data, ctx, id);
            }
        }
    }

    public void sendServuxLitematics(NbtCompound data, ServerPlayerEntity player)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxLitematicsListener handler : this.handlers)
            {
                handler.sendServuxLitematics(data, player);
            }
        }
    }
    public void encodeServuxLitematics(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxLitematicsListener handler : this.handlers)
            {
                handler.encodeServuxLitematics(data, player, id);
            }
        }
    }
    public void decodeServuxLitematics(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxLitematicsListener handler : this.handlers)
            {
                handler.decodeServuxLitematics(data, player, id);
            }
        }
    }
}
