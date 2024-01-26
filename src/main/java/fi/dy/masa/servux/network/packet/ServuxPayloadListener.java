package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.interfaces.IServuxPayloadListener;
import fi.dy.masa.servux.network.ServerNetworkPlayHandler;
import fi.dy.masa.servux.network.payload.ServuxPayload;
import fi.dy.masa.servux.util.PayloadUtils;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ServuxPayloadListener implements IServuxPayloadListener
{
    /**
     * PacketSplitter/IPluginChannelHandler translated to new Networking API using various Encoding/Decoding using NbtCompound at the payload
     */
    public static final int MAX_TOTAL_PER_PACKET_S2C = 1048576;
    public static final int MAX_PAYLOAD_PER_PACKET_S2C = MAX_TOTAL_PER_PACKET_S2C - 5;
    public static final int DEFAULT_MAX_RECEIVE_SIZE_C2S = 1048576;

    private static final Map<Pair<PacketListener, Identifier>, ServuxPayloadListener.ReadingSession> READING_SESSIONS = new HashMap<>();

    @Override
    public void sendServuxPayload(NbtCompound data, ServerPlayerEntity player)
    {
        ServuxPayload payload = new ServuxPayload(data);
        Servux.printDebug("ServuxPayloadListener#sendServuxPayload(): sending payload of size {} bytes to player: {}.", data.getSizeInBytes(), player.getName().getLiteralString());
        ServerNetworkPlayHandler.sendServUX(payload, player);
    }
    @Override
    public void receiveServuxPayload(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        // Split From NbtCompound Packet using PayloadUtils
        PacketByteBuf buf = PayloadUtils.fromNbt(data, "data");
        //decodeServuxPayload(id, buf, ctx.player().networkHandler);
    }

    // *****************************************************************************************************************************************
    // HandleViaPacketSplitter().receive()
    // *****************************************************************************************************************************************
    public static PacketByteBuf splitServuxPayload(Identifier id, ServuxPayload payload, ServerPlayNetworking.Context ctx)
    {
        PacketByteBuf buf = PayloadUtils.fromNbt(payload.data(), ServuxPayload.KEY);
        return spliceServuxPayload(id, buf, DEFAULT_MAX_RECEIVE_SIZE_C2S, ctx.player().networkHandler);
    }
    private static PacketByteBuf spliceServuxPayload(Identifier id, PacketByteBuf buf, int maxLength, ServerPlayNetworkHandler netHandler)
    {
        Pair<PacketListener, Identifier> key = Pair.of(netHandler, id);
        return READING_SESSIONS.computeIfAbsent(key, ServuxPayloadListener.ReadingSession::new).receiveServuxSlice(buf, maxLength);
    }
    // *****************************************************************************************************************************************
    public void createServuxPayload(ServuxPayload payload, ServerPlayerEntity player)
    {
        // Get Identifier & data from payload, and attempt to follow the original code.
        Identifier id = payload.getId().id();
        PacketByteBuf buf = PayloadUtils.fromNbt(payload.data(), ServuxPayload.KEY);
        assert buf != null;
        sliceServuxPayload(id, buf, MAX_PAYLOAD_PER_PACKET_S2C, player);
    }
    private void sliceServuxPayload(Identifier channel, PacketByteBuf packet, int payloadLimit, ServerPlayerEntity player)
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

            encodeServuxPayload(buf, player);
        }

        packet.release();
    }
    private void encodeServuxPayload(PacketByteBuf packet, ServerPlayerEntity player)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        nbt.putByteArray("data", packet.readByteArray());
        Servux.printDebug("ServuxPayloadListener#encodeServuxPayload(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxPayload(nbt, player);
    }

    public void encodeServuxPayloadWithType(int packetType, NbtCompound data, ServerPlayerEntity player)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeNbt(data);
        Servux.printDebug("ServuxPayloadListener#encodeServuxPayloadWithType(): buf.writeNbt() size in bytes: {}", buf.readableBytes());
        nbt.putInt("packetType", packetType);
        Servux.printDebug("ServuxPayloadListener#encodeServuxPayloadWithType(): nbt.putInt() size in bytes: {}", nbt.getSizeInBytes());
        nbt.putByteArray("data", buf.readByteArray());
        Servux.printDebug("ServuxPayloadListener#encodeServuxPayloadWithType(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxPayload(nbt, player);
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
        private PacketByteBuf receiveServuxSlice(PacketByteBuf data, int maxLength)
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
