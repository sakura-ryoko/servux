package fi.dy.masa.servux.event;

import fi.dy.masa.servux.interfaces.IServuxMetadataListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

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
    public void receiveServuxMetadata(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxMetadataListener handler : this.handlers)
            {
                handler.receiveServuxMetadata(data, ctx, id);
            }
        }
    }

    public void sendServuxMetadata(NbtCompound data, ServerPlayerEntity player)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxMetadataListener handler : this.handlers)
            {
                handler.sendServuxMetadata(data, player);
            }
        }
    }
    public void encodeServuxMetadata(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxMetadataListener handler : this.handlers)
            {
                handler.encodeServuxMetadata(data, player, id);
            }
        }
    }
    public void decodeServuxMetadata(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxMetadataListener handler : this.handlers)
            {
                handler.decodeServuxMetadata(data, player, id);
            }
        }
    }
}
