package fi.dy.masa.servux.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import fi.dy.masa.servux.network.ServuxBuf;

public class PayloadUtils
{
    @Nullable
    public static PacketByteBuf toPacketByteBuf(@Nonnull ServuxBuf in)
    {
        if (in.isReadable())
        {
            return new PacketByteBuf(in.asByteBuf());
        }

        return null;
    }

    @Nullable
    public static RegistryByteBuf toRegistryByteBuf(@Nonnull ServuxBuf in,
                                                    @Nonnull DynamicRegistryManager registryManager)
    {
        if (in.isReadable() && registryManager.equals(DynamicRegistryManager.EMPTY) == false)
        {
            return new RegistryByteBuf(in.asByteBuf(), registryManager);
        }

        return null;
    }

    @Nullable
    public static ServuxBuf fromPacketByteBuf(@Nonnull PacketByteBuf in)
    {
        if (in.isReadable())
        {
            return new ServuxBuf(in.asByteBuf());
        }

        return null;
    }

    @Nullable
    public static ServuxBuf fromRegistryByteBuf(@Nonnull RegistryByteBuf in)
    {
        if (in.isReadable())
        {
            return new ServuxBuf(in.asByteBuf());
        }

        return null;
    }

    @Nullable
    public static NbtElement toNbtElement(@Nonnull ServuxBuf in)
    {
        if (in.isReadable())
        {
            return in.readNbt(NbtSizeTracker.of(in.readableBytes()));
        }

        return null;
    }

    @Nullable
    public static NbtCompound toNbtCompound(@Nonnull ServuxBuf in)
    {
        if (in.isReadable())
        {
            return in.readNbt();
        }

        return null;
    }

    @Nullable
    public static ServuxBuf fromNbtElement(@Nonnull NbtElement in)
    {
        if (in.getSizeInBytes() > 0)
        {
            ServuxBuf buf = new ServuxBuf(Unpooled.buffer());
            buf.writeNbt(in);

            return buf;
        }

        return null;
    }

    @Nullable
    public static ServuxBuf fromNbtCompound(@Nonnull NbtCompound in)
    {
        if (in.getSizeInBytes() > 0)
        {
            ServuxBuf buf = new ServuxBuf(Unpooled.buffer());
            buf.writeNbt(in);

            return buf;
        }

        return null;
    }
}
