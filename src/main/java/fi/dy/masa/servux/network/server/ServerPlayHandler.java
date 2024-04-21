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
        boolean isRegistered = this.isServerPlayChannelRegistered(handler);
        Identifier channel = handler.getPayloadChannel();

        if (this.handlers.containsEntry(channel, handler) == false)
        {
            this.handlers.put(channel, (IPluginServerPlayHandler<T>) handler);

            if (handler.isPlayRegistered(channel) == false && isRegistered == false)
            {
                handler.registerPlayPayload(channel);
            }

            handler.setPlayRegistered(channel);
        }
    }

    @Override
    public <P extends CustomPayload> boolean isServerPlayChannelRegistered(IPluginServerPlayHandler<P> handler)
    {
        Identifier channel = handler.getPayloadChannel();
        boolean isRegistered = false;

        for (IPluginServerPlayHandler<T> handlerEnt : this.handlers.get(channel))
        {
            if (isRegistered == false)
            {
                isRegistered = handlerEnt.isPlayRegistered(channel);
            }
        }

        return isRegistered;
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
    public void decodeObjects(Identifier channel, ServerPlayerEntity player, Object... args)
    {
        if (this.handlers.isEmpty() == false)
        {
            for (IPluginServerPlayHandler<T> handler : this.handlers.get(channel))
            {
                handler.decodeObjects(channel, player, args);
            }
        }
    }
}
