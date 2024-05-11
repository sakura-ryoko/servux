package fi.dy.masa.servux.network.server;

import com.google.common.collect.ArrayListMultimap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * The Server Network Play handler
 * @param <T> (Payload)
 */
public class ServerPlayHandler<T extends CustomPayload> implements IServerPlayHandler
{
    private static final ServerPlayHandler<CustomPayload> INSTANCE = new ServerPlayHandler<>();
    private final ArrayListMultimap<Identifier, IPluginServerPlayHandler<T>> handlers = ArrayListMultimap.create();
    public static IServerPlayHandler getInstance()
    {
        return INSTANCE;
    }

    private ServerPlayHandler() {}

    @Override
    @SuppressWarnings("unchecked")
    public <P extends CustomPayload> void registerServerPlayHandler(IPluginServerPlayHandler<P> handler)
    {
        Identifier channel = handler.getPayloadChannel();

        if (this.handlers.containsEntry(channel, handler) == false)
        {
            this.handlers.put(channel, (IPluginServerPlayHandler<T>) handler);
        }
    }

    @Override
    public <P extends CustomPayload> void unregisterServerPlayHandler(IPluginServerPlayHandler<P> handler)
    {
        Identifier channel = handler.getPayloadChannel();

        if (this.handlers.remove(channel, handler))
        {
            handler.reset(channel);
            handler.unregisterPlayReceiver();
        }
    }

    /**
     * API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public void reset(Identifier channel)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.reset(channel);
            }
        }
    }

    /**
     * API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public void decodeNbtCompound(Identifier channel, ServerPlayerEntity player, NbtCompound data)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.decodeNbtCompound(channel, player, data);
            }
        }
    }

    /**
     * API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public void decodeByteBuf(Identifier channel, ServerPlayerEntity player, ServuxBuf data)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.decodeByteBuf(channel, player, data);
            }
        }
    }

    /**
     * API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public <D> void decodeObject(Identifier channel, ServerPlayerEntity player, D data1)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.decodeObject(channel, player, data1);
            }
        }
    }
}
