package fi.dy.masa.servux.event;

import fi.dy.masa.servux.interfaces.IServuxPayloadListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ServuxPayloadHandler implements IServuxPayloadManager
{
    private static final ServuxPayloadHandler INSTANCE = new ServuxPayloadHandler();
    private final List<IServuxPayloadListener> handlers = new ArrayList<>();
    public static IServuxPayloadManager getInstance() { return INSTANCE; }
    @Override
    public void registerServuxHandler(IServuxPayloadListener handler)
    {
        if (!this.handlers.contains(handler))
        {
            this.handlers.add(handler);
        }
    }
    @Override
    public void unregisterServuxHandler(IServuxPayloadListener handler)
    {
        this.handlers.remove(handler);
    }

    /**
     * NOT PUBLIC API - DO NOT CALL
     */
    public void receiveServuxPayload(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxPayloadListener handler : this.handlers)
            {
                handler.receiveServuxPayload(data, ctx, id);
            }
        }
    }

    public void encodeServuxPayload(PacketByteBuf packet, ServerPlayerEntity player, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxPayloadListener handler : this.handlers)
            {
                handler.encodeServuxPayload(packet, player, id);
            }
        }
    }
    public void encodeServuxPayloadWithType(int packetType, NbtCompound tag, ServerPlayerEntity player)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxPayloadListener handler : this.handlers)
            {
                handler.encodeServuxPayloadWithType(packetType, tag, player);
            }
        }
    }
    public void decodeServuxPayload(PacketByteBuf packet, ServerPlayerEntity player, Identifier id)
    {
        if (!this.handlers.isEmpty())
        {
            for (IServuxPayloadListener handler : this.handlers)
            {
                handler.decodeServuxPayload(packet, player, id);
            }
        }
    }
}
