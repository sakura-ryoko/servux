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

            if (handler.isPlayRegistered(channel) == false)
            {
                handler.registerPlayPayload(channel);
            }
            handler.registerPlayHandler(channel);
        }
    }

    @Override
    public <P extends CustomPayload> void unregisterServerPlayHandler(IPluginServerPlayHandler<P> handler)
    {
        Identifier channel = handler.getPayloadChannel();

        if (this.handlers.remove(channel, handler))
        {
            handler.unregisterPlayHandler(channel);
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
    public void registerPlayPayload(Identifier channel)
    {
       if (this.handlers.isEmpty() == false)
       {
           for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
           {
               handler.registerPlayPayload(channel);
           }
       }
    }

    /**
     * API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public void registerPlayHandler(Identifier channel)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.registerPlayHandler(channel);
            }
        }
    }

    /**
     * API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public void unregisterPlayHandler(Identifier channel)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.unregisterPlayHandler(channel);
            }
        }
    }

    /**
     * API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public void decodeC2SNbtCompound(Identifier channel, NbtCompound data, ServerPlayerEntity player)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.decodeNbtCompound(channel, data, player);
            }
        }
    }

    /**
     * API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public void decodeC2SByteBuf(Identifier channel, ServuxBuf data, ServerPlayerEntity player)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.decodeByteBuf(channel, data, player);
            }
        }
    }

    /**
     * API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public void decodeC2SObject(Identifier channel, Object data, ServerPlayerEntity player)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.decodeObject(channel, data, player);
            }
        }
    }
}
