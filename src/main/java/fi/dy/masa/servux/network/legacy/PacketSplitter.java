package fi.dy.masa.servux.network.legacy;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
//import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Network packet splitter code from QuickCarpet by skyrising
 * @author skyrising
 */
@Deprecated
public class PacketSplitter
{
    public static final int MAX_TOTAL_PER_PACKET_S2C = 1048576;
    public static final int MAX_PAYLOAD_PER_PACKET_S2C = MAX_TOTAL_PER_PACKET_S2C - 5;
    public static final int DEFAULT_MAX_RECEIVE_SIZE_C2S = 1048576;

    private static final Map<Pair<PacketListener, Identifier>, ReadingSession> READING_SESSIONS = new HashMap<>();

    public static void send(Identifier channel, PacketByteBuf packet, ServerPlayerEntity player)
    {
        send(channel, packet, MAX_PAYLOAD_PER_PACKET_S2C, player);
    }

    private static void send(Identifier channel, PacketByteBuf packet, int payloadLimit, ServerPlayerEntity player)
    {
        int len = packet.writerIndex();

        packet.resetReaderIndex();

        for (int offset = 0; offset < len; offset += payloadLimit)
        {
            int thisLen = Math.min(len - offset, payloadLimit);
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(thisLen));

            if (offset == 0)
            {
                buf.writeVarInt(len);
            }

            buf.writeBytes(packet, thisLen);

            //ServerPlayNetworking.send(player, channel, buf);
        }

        packet.release();
    }

    @Nullable
    public static PacketByteBuf receive(Identifier channel,
                                        PacketByteBuf buf,
                                        ServerPlayNetworkHandler networkHandler)
    {
        return receive(channel, buf, DEFAULT_MAX_RECEIVE_SIZE_C2S, networkHandler);
    }

    @Nullable
    private static PacketByteBuf receive(Identifier channel,
                                         PacketByteBuf data,
                                         int maxLength,
                                         ServerPlayNetworkHandler networkHandler)
    {
        Pair<PacketListener, Identifier> key = Pair.of(networkHandler, channel);
        return READING_SESSIONS.computeIfAbsent(key, ReadingSession::new).receive(data, maxLength);
    }

    /**
     * Sends a packet type ID as a VarInt, and then the given Compound tag.
     */
    public static void sendPacketTypeAndCompound(Identifier channel, int packetType, NbtCompound data, ServerPlayerEntity player)
    {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(packetType);
        buf.writeNbt(data);

        send(channel, buf, player);
    }

    private static class ReadingSession
    {
        private final Pair<PacketListener, Identifier> key;
        private int expectedSize = -1;
        private PacketByteBuf received;

        private ReadingSession(Pair<PacketListener, Identifier> key)
        {
            this.key = key;
        }

        @Nullable
        private PacketByteBuf receive(PacketByteBuf data, int maxLength)
        {
            data.readerIndex(0);
            //data = PacketUtils.slice(data);

            if (this.expectedSize < 0)
            {
                this.expectedSize = data.readVarInt();

                if (this.expectedSize > maxLength)
                {
                    throw new IllegalArgumentException("Payload too large");
                }

                this.received = new PacketByteBuf(Unpooled.buffer(this.expectedSize));
            }

            this.received.writeBytes(data.readBytes(data.readableBytes()));

            if (this.received.writerIndex() >= this.expectedSize)
            {
                READING_SESSIONS.remove(this.key);
                return this.received;
            }

            return null;
        }
    }
}
