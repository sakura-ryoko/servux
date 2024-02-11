package fi.dy.masa.servux.network.handlers;

import com.google.common.collect.ArrayListMultimap;
import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.ServuxByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerConfigHandler<T extends CustomPayload> implements IServerConfigHandler
{
    private static final ServerConfigHandler<CustomPayload> INSTANCE = new ServerConfigHandler<>();
    private final ArrayListMultimap<PayloadType, IPluginServerConfigHandler<T>> handlers = ArrayListMultimap.create();
    public static IServerConfigHandler getInstance()
    {
        return INSTANCE;
    }

    private ServerConfigHandler() { }

    @Override
    @SuppressWarnings("unchecked")
    public <P extends CustomPayload> void registerServerConfigHandler(IPluginServerConfigHandler<P> handler)
    {
        PayloadType type = handler.getPayloadType();

        if (type.exists(type))
        {
            if (!this.handlers.containsEntry(type, handler))
            {
                this.handlers.put(type, (IPluginServerConfigHandler<T>) handler);
                handler.registerConfigPayload(type);
                // Don't register Receivers until Server/World fully joined.
            }
        }
    }

    @Override
    public <P extends CustomPayload> void unregisterServerConfigHandler(IPluginServerConfigHandler<P> handler)
    {
        PayloadType type = handler.getPayloadType();

        if (type.exists(type))
        {
            if (this.handlers.remove(type, handler))
            {
                handler.unregisterConfigHandler(type);
            }
        }
    }

    /**
     * LOCAL API CALLS DO NOT USE ANYWHERE ELSE (DANGEROUS!)
     */
    public void reset(PayloadType type)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPluginServerConfigHandler<T> handler : this.handlers.get(type))
            {
                handler.reset(type);
            }
        }
    }
    public void registerConfigPayload(PayloadType type)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPluginServerConfigHandler<T> handler : this.handlers.get(type))
            {
                handler.registerConfigPayload(type);
            }
        }
    }
    public void registerConfigHandler(PayloadType type)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPluginServerConfigHandler<T> handler : this.handlers.get(type))
            {
                handler.registerConfigHandler(type);
            }
        }
    }
    public void unregisterConfigHandler(PayloadType type)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPluginServerConfigHandler<T> handler : this.handlers.get(type))
            {
                handler.unregisterConfigHandler(type);
            }
        }
    }
    public <P extends CustomPayload> void receiveC2SConfigPayload(PayloadType type, P payload, ServerConfigurationNetworking.Context ctx)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPluginServerConfigHandler<T> handler : this.handlers.get(type))
            {
                handler.receiveC2SConfigPayload(type, payload, ctx);
            }
        }
    }
    public <P extends CustomPayload> void receiveC2SConfigPayload(PayloadType type, P payload, ServerConfigurationNetworkHandler networkHandler)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPluginServerConfigHandler<T> handler : this.handlers.get(type))
            {
                handler.receiveC2SConfigPayload(type, payload, networkHandler);
            }
        }
    }
   public void decodeC2SNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player)
   {
       if (!this.handlers.isEmpty())
       {
           for (IPluginServerConfigHandler<T> handler : this.handlers.get(type))
           {
               handler.decodeC2SNbtCompound(type, data, player);
           }
       }
   }
    public void decodeC2SByteBuf(PayloadType type, ServuxByteBuf data, ServerPlayerEntity player)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPluginServerConfigHandler<T> handler : this.handlers.get(type))
            {
                handler.decodeC2SByteBuf(type, data, player);
            }
        }
    }
}
