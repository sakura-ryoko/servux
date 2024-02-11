package fi.dy.masa.servux.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;

/**
 * This can be used as a replacement for your legacy "PacketByteBuf" type of CustomPayloads,
 * And can be used to try to (re)-implement some of your IPluginChannelHandler based protocols
 */
public class ServuxByteBuf extends PacketByteBuf
{
    public ServuxByteBuf(ByteBuf parent)
    {
        super(parent);
    }
}
