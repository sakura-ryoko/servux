package fi.dy.masa.servux.network;

import com.google.common.collect.ArrayListMultimap;
import org.jetbrains.annotations.ApiStatus;
import net.minecraft.network.packet.CustomPayload;
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

    @ApiStatus.Internal
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
}
