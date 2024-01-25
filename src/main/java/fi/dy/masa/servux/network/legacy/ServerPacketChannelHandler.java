package fi.dy.masa.servux.network.legacy;

import java.util.HashMap;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.util.Identifier;

public class ServerPacketChannelHandler
{
    public static final ServerPacketChannelHandler INSTANCE = new ServerPacketChannelHandler();

    private final HashMap<Identifier, IPluginChannelHandler> handlers = new HashMap<>();

    private ServerPacketChannelHandler()
    {
    }

    public void registerServerChannelHandler(IPluginChannelHandler handler)
    {
        synchronized (this.handlers)
        {
            Identifier channel = handler.getChannel();

            if (this.handlers.containsKey(channel) == false)
            {
                this.handlers.put(channel, handler);

                if (handler.isSubscribable())
                {
                    S2CPlayChannelEvents.REGISTER.register((net, server, sender, channels) -> {
                        if (channels.contains(channel))
                        {
                            handler.subscribe(net);
                        }
                    });
                    S2CPlayChannelEvents.UNREGISTER.register((net, server, sender, channels) -> {
                        if (channels.contains(channel))
                        {
                            handler.unsubscribe(net);
                        }
                    });
                    //ServerPlayNetworking.registerGlobalReceiver(channel, handler.getServerPacketHandler());
                }
            }
        }
    }

    public void unregisterServerChannelHandler(IPluginChannelHandler handler)
    {
        synchronized (this.handlers)
        {
            Identifier channel = handler.getChannel();

            if (this.handlers.remove(channel, handler))
            {
                ServerPlayNetworking.unregisterGlobalReceiver(channel);
            }
        }
    }
}
