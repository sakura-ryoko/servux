package fi.dy.masa.servux.deprecated;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class ServuxStructuresHandler
        //implements IServuxStructuresManager
{
    private static final ServuxStructuresHandler INSTANCE = new ServuxStructuresHandler();
    private final List<IServuxStructuresListener> handlers = new ArrayList<>();
    //public static IServuxStructuresManager getInstance() { return INSTANCE; }
    //@Override
    public void registerServuxStructuresHandler(IServuxStructuresListener handler)
    {
        if (!this.handlers.contains(handler))
        {
            this.handlers.add(handler);
        }
    }
    //@Override
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
    public void receiveServuxStructures(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxStructuresListener handler : this.handlers)
            {
                handler.receiveServuxStructures(data, ctx, id);
            }
        }
    }

    public void sendServuxStructures(NbtCompound data, ServerPlayerEntity player)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxStructuresListener handler : this.handlers)
            {
                handler.sendServuxStructures(data, player);
            }
        }
    }
    public void encodeServuxStructures(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxStructuresListener handler : this.handlers)
            {
                handler.encodeServuxStructures(data, player, id);
            }
        }
    }
    public void decodeServuxStructures(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxStructuresListener handler : this.handlers)
            {
                handler.decodeServuxStructures(data, player, id);
            }
        }
    }
}
