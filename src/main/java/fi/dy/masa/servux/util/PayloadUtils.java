package fi.dy.masa.servux.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.server.ServuxBuf;

public class PayloadUtils
{
    public final static int FROM_SERVER = 1;
    public final static int TO_SERVER = 2;
    public final static int BOTH_SERVER = 3;
    public final static int TO_CLIENT = 4;
    public final static int FROM_CLIENT = 5;
    public final static int BOTH_CLIENT = 6;

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


    public static <T extends CustomPayload> void registerPlayPayload(int direction, @Nonnull CustomPayload.Id<T> id, @Nonnull PacketCodec<? super RegistryByteBuf,T> codec)
    {
        switch (direction)
        {
            case TO_SERVER, FROM_CLIENT -> PayloadTypeRegistry.playC2S().register(id, codec);
            case FROM_SERVER, TO_CLIENT -> PayloadTypeRegistry.playS2C().register(id, codec);
            default ->
            {
                PayloadTypeRegistry.playC2S().register(id, codec);
                PayloadTypeRegistry.playS2C().register(id, codec);
            }
        }
    }

    public static <T extends CustomPayload> boolean registerPlayReceiver(@Nonnull CustomPayload.Id<T> id, @Nonnull ServerPlayNetworking.PlayPayloadHandler<T> receiver)
    {
        try
        {
            return ServerPlayNetworking.registerGlobalReceiver(id, receiver);
        }
        catch (IllegalArgumentException e)
        {
            Servux.logger.error("registerPlayReceiver IllegalArgumentException: Payload not registered");
        }

        return false;
    }

    public static void unregisterPlayReceiver(@Nonnull Identifier id)
    {
        ServerPlayNetworking.unregisterGlobalReceiver(id);
    }
}
