package fi.dy.masa.servux.deprecated;

import java.util.Objects;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.ByteBuf;

/**
 * This was never being used.  PayloadUtils basically is the Utility to put them in, if ever needed.
  */
@Deprecated(forRemoval = false)
public class PacketUtils
{
    /**
     * Wraps the newly created buf from {@code buf.slice()} in a PacketByteBuf.
     *
     * @param buf the original ByteBuf
     * @return a slice of the buffer
     * @see ByteBuf#slice()
     */
    public static PacketByteBuf slice(ByteBuf buf)
    {
        Objects.requireNonNull(buf, "ByteBuf cannot be null");
        return new PacketByteBuf(buf.slice());
    }

    /**
     * Wraps the newly created buf from {@code buf.retainedSlice()} in a PacketByteBuf.
     *
     * @param buf the original ByteBuf
     * @return a slice of the buffer
     * @see ByteBuf#retainedSlice()
     */
    public static PacketByteBuf retainedSlice(ByteBuf buf)
    {
        Objects.requireNonNull(buf, "ByteBuf cannot be null");
        return new PacketByteBuf(buf.retainedSlice());
    }
}
