package fi.dy.masa.servux.network.packet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IServerPayloadData;

public class ServuxEntitiesPacket implements IServerPayloadData
{
    private Type packetType;
    private int transactionId;
    private int entityId;
    private BlockPos pos;
    private NbtCompound nbt;
    private PacketByteBuf buffer;
    public static final int PROTOCOL_VERSION = 1;

    private ServuxEntitiesPacket(Type type) {
        this.packetType = type;
        this.clearPacket();
    }

    public static ServuxEntitiesPacket MetadataRequest(NbtCompound nbt)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_METADATA_REQUEST);
        packet.nbt = nbt.copy();
        return packet;
    }

    public static ServuxEntitiesPacket MetadataResponse(NbtCompound nbt)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_METADATA);
        packet.nbt = nbt.copy();
        return packet;
    }

    // Entity simple response
    public static ServuxEntitiesPacket SimpleEntityResponse(int entityId, NbtCompound nbt)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE);
        packet.nbt = nbt.copy();
        packet.entityId = entityId;
        return packet;
    }

    public static ServuxEntitiesPacket SimpleBlockResponse(BlockPos pos, NbtCompound nbt)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE);
        packet.nbt = nbt.copy();
        packet.pos = pos.toImmutable();
        return packet;
    }

    public static ServuxEntitiesPacket BlockEntityRequest(BlockPos pos)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_BLOCK_ENTITY_REQUEST);
        packet.pos = pos.toImmutable();
        return packet;
    }

    public static ServuxEntitiesPacket EntityRequest(int entityId)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_ENTITY_REQUEST);
        packet.entityId = entityId;
        return packet;
    }

    // Response Nbt Packet, set splitter to true to use Packet Splitter
    public ServuxEntitiesPacket(int transactionId, NbtCompound nbt, boolean splitter)
    {
        if (splitter)
        {
            this.packetType = Type.PACKET_S2C_NBT_RESPONSE_START;
        }
        else
        {
            this.packetType = Type.PACKET_S2C_NBT_RESPONSE_SIMPLE;
        }
        this.transactionId = transactionId;
        this.nbt = new NbtCompound();
        this.nbt.copyFrom(nbt);
        this.clearPacket();
    }

    // Response Packet Slice (Packet Splitter)
    public ServuxEntitiesPacket(@Nonnull PacketByteBuf packet)
    {
        this.packetType = Type.PACKET_S2C_NBT_RESPONSE_DATA;
        this.nbt = new NbtCompound();
        this.buffer = packet;
    }

    private void clearPacket()
    {
        if (this.buffer != null)
        {
            this.buffer.clear();
            this.buffer = new PacketByteBuf(Unpooled.buffer());
        }
    }

    @Override
    public int getVersion()
    {
        return PROTOCOL_VERSION;
    }

    @Override
    public int getPacketType()
    {
        return this.packetType.get();
    }

    @Override
    public int getTotalSize()
    {
        int total = 2;

        if (this.nbt != null && this.nbt.isEmpty() == false)
        {
            total += this.nbt.getSizeInBytes();
        }
        if (this.buffer != null)
        {
            total += this.buffer.readableBytes();
        }

        return total;
    }

    public Type getType()
    {
        return this.packetType;
    }

    public int getTransactionId() { return this.transactionId; }

    public int getEntityId() { return this.entityId; }

    public BlockPos getPos() { return this.pos; }

    public NbtCompound getCompound()
    {
        return this.nbt;
    }

    public PacketByteBuf getBuffer()
    {
        return this.buffer;
    }

    public boolean hasBuffer() { return this.buffer != null && this.buffer.isReadable(); }

    public boolean hasNbt() { return this.nbt != null && !this.nbt.isEmpty(); }

    @Override
    public boolean isEmpty()
    {
        return !this.hasBuffer() && !this.hasNbt();
    }

    @Override
    public void toPacket(PacketByteBuf output)
    {
        output.writeVarInt(this.packetType.get());

        switch (this.packetType)
        {
            case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
            {
                // Write BE Request
                try
                {
                    output.writeVarInt(this.transactionId);
                    output.writeBlockPos(this.pos);
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#toPacket: error writing Block Entity Request to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_ENTITY_REQUEST ->
            {
                // Write Entity Request
                try
                {
                    output.writeVarInt(this.transactionId);
                    output.writeVarInt(this.entityId);
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#toPacket: error writing Entity Request to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_NBT_RESPONSE_SIMPLE ->
            {
                // Write Response (Without Packet Splitter)
                try
                {
                    output.writeVarInt(this.transactionId);
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#toPacket: error writing response data to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                // Write Packet Buffer (Slice)
                try
                {
                    output.writeBytes(this.buffer.readBytes(this.buffer.readableBytes()));
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#toPacket: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
            {
                try
                {
                    output.writeBlockPos(this.pos);
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#toPacket: error writing Block Entity Response to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
            {
                try
                {
                    output.writeVarInt(this.entityId);
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#toPacket: error writing Entity Response to packet: [{}]", e.getLocalizedMessage());
                }
            }
            default ->
            {
                // Write NBT
                try
                {
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
                }
            }
        }
    }

    @Nullable
    public static ServuxEntitiesPacket fromPacket(PacketByteBuf input)
    {
        int i = input.readVarInt();
        Type type = getType(i);

        if (type == null)
        {
            // Invalid Type
            Servux.logger.warn("ServuxEntitiesPacket#fromPacket: invalid packet type received");
            return null;
        }
        switch (type)
        {
            case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
            {
                // Read Packet Buffer
                try
                {
                    input.readVarInt(); // todo: old code compat
                    return ServuxEntitiesPacket.BlockEntityRequest(input.readBlockPos());
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#fromPacket: error reading Block Entity Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_ENTITY_REQUEST ->
            {
                // Read Packet Buffer
                try
                {
                    input.readVarInt(); // todo: old code compat
                    return ServuxEntitiesPacket.EntityRequest(input.readVarInt());
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#fromPacket: error reading Entity Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_NBT_RESPONSE_SIMPLE ->
            {
                // Read Nbt Response (Without Packet Splitter)
                try
                {
                    return new ServuxEntitiesPacket(input.readVarInt(), input.readNbt(), false);
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#fromPacket: error reading Response from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                // Read Packet Buffer Slice
                try
                {
                    return new ServuxEntitiesPacket(new PacketByteBuf(input.readBytes(input.readableBytes())));
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#fromPacket: error reading Buffer from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST ->
            {
                // Read Nbt
                try
                {
                    return ServuxEntitiesPacket.MetadataRequest(input.readNbt());
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxEntitiesPacket#fromPacket: error reading Metadata Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            default ->
            {
                Servux.logger.error("ServuxEntitiesPacket#fromPacket: Unknown packet type!");
            }
        }

        return null;
    }

    @Override
    public void clear()
    {
        if (this.nbt != null && this.nbt.isEmpty() == false)
        {
            this.nbt = new NbtCompound();
        }
        this.clearPacket();
        this.transactionId = -1;
        this.entityId = -1;
        this.pos = BlockPos.ORIGIN;
        this.packetType = null;
    }

    @Nullable
    public static Type getType(int input)
    {
        for (Type type : Type.values())
        {
            if (type.get() == input)
            {
                return type;
            }
        }

        return null;
    }

    public enum Type
    {
        PACKET_S2C_METADATA(1),
        PACKET_C2S_METADATA_REQUEST(2),
        PACKET_C2S_BLOCK_ENTITY_REQUEST(3),
        PACKET_C2S_ENTITY_REQUEST(4),
        PACKET_S2C_NBT_RESPONSE_START(5),
        PACKET_S2C_NBT_RESPONSE_SIMPLE(6),
        PACKET_S2C_NBT_RESPONSE_DATA(7),
        PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE(8),
        PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE(9);

        private final int type;

        Type(int type)
        {
            this.type = type;
        }

        int get() { return this.type; }
    }

    public record Payload(ServuxEntitiesPacket data) implements CustomPayload
    {
        public static final Id<Payload> ID = new Id<>(ServuxEntitiesHandler.CHANNEL_ID);
        public static final PacketCodec<PacketByteBuf, Payload> CODEC = CustomPayload.codecOf(Payload::write, Payload::new);

        public Payload(PacketByteBuf input)
        {
            this(fromPacket(input));
        }

        private void write(PacketByteBuf output)
        {
            data.toPacket(output);
        }

        @Override
        public Id<? extends CustomPayload> getId()
        {
            return ID;
        }
    }
}
