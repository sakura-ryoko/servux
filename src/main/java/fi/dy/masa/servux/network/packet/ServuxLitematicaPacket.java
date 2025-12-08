package fi.dy.masa.servux.network.packet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IServerPayloadData;

public class ServuxLitematicaPacket implements IServerPayloadData
{
    private Type packetType;
    private int transactionId;
    private int entityId;
    private BlockPos pos;
    private CompoundTag nbt;
    private ChunkPos chunkPos;
    private FriendlyByteBuf buffer;
    public static final int PROTOCOL_VERSION = 1;

    private ServuxLitematicaPacket(Type type)
    {
        this.packetType = type;
        this.transactionId = -1;
        this.entityId = -1;
        this.pos = BlockPos.ZERO;
        this.chunkPos = ChunkPos.ZERO;
        this.nbt = new CompoundTag();
        this.clearPacket();
    }

    public static ServuxLitematicaPacket MetadataRequest(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_METADATA_REQUEST);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    public static ServuxLitematicaPacket MetadataResponse(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_METADATA);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    // Entity simple response
    public static ServuxLitematicaPacket SimpleEntityResponse(int entityId, @Nullable CompoundTag nbt)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        packet.entityId = entityId;
        return packet;
    }

    public static ServuxLitematicaPacket SimpleBlockResponse(BlockPos pos, @Nullable CompoundTag nbt)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        packet.pos = pos.immutable();
        return packet;
    }

    public static ServuxLitematicaPacket BlockEntityRequest(BlockPos pos)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_BLOCK_ENTITY_REQUEST);
        packet.pos = pos.immutable();
        return packet;
    }

    public static ServuxLitematicaPacket EntityRequest(int entityId)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_ENTITY_REQUEST);
        packet.entityId = entityId;
        return packet;
    }

    public static ServuxLitematicaPacket BulkNbtRequest(ChunkPos chunkPos, @Nullable CompoundTag nbt)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_BULK_ENTITY_NBT_REQUEST);
        packet.chunkPos = chunkPos;
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    // Nbt Packet, using Packet Splitter
    public static ServuxLitematicaPacket ResponseS2CStart(@Nonnull CompoundTag nbt)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_NBT_RESPONSE_START);
        packet.nbt.merge(nbt);
        return packet;
    }

    public static ServuxLitematicaPacket ResponseS2CData(@Nonnull FriendlyByteBuf buffer)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_NBT_RESPONSE_DATA);
        packet.buffer = new FriendlyByteBuf(buffer.copy());
        packet.nbt = new CompoundTag();
        return packet;
    }

    public static ServuxLitematicaPacket ResponseC2SStart(@Nonnull CompoundTag nbt)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_NBT_RESPONSE_START);
        packet.nbt.merge(nbt);
        return packet;
    }

    public static ServuxLitematicaPacket ResponseC2SData(@Nonnull FriendlyByteBuf buffer)
    {
        var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_NBT_RESPONSE_DATA);
        packet.buffer = new FriendlyByteBuf(buffer.copy());
        packet.nbt = new CompoundTag();
        return packet;
    }

    private void clearPacket()
    {
        if (this.buffer != null)
        {
            this.buffer.clear();
            this.buffer = new FriendlyByteBuf(Unpooled.buffer());
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

        if (this.nbt != null && !this.nbt.isEmpty())
        {
            total += this.nbt.sizeInBytes();
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

    public void setTransactionId(int id)
    {
        this.transactionId = id;
    }

    public int getTransactionId()
    {
        return this.transactionId;
    }

    public int getEntityId() { return this.entityId; }

    public BlockPos getPos() { return this.pos; }

    public CompoundTag getCompound()
    {
        return this.nbt;
    }

    public ChunkPos getChunkPos() { return this.chunkPos; }

    public FriendlyByteBuf getBuffer()
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
    public void toPacket(FriendlyByteBuf output)
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
                    Servux.LOGGER.error("ServuxLitematicaPacket#toPacket: error writing Block Entity Request to packet: [{}]", e.getLocalizedMessage());
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
                    Servux.LOGGER.error("ServuxLitematicaPacket#toPacket: error writing Entity Request to packet: [{}]", e.getLocalizedMessage());
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
                    Servux.LOGGER.error("ServuxLitematicaPacket#toPacket: error writing Block Entity Response to packet: [{}]", e.getLocalizedMessage());
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
                    Servux.LOGGER.error("ServuxLitematicaPacket#toPacket: error writing Entity Response to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_BULK_ENTITY_NBT_REQUEST ->
            {
                try
                {
                    output.writeChunkPos(this.chunkPos);
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#toPacket: error writing Bulk Entity Request to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_NBT_RESPONSE_DATA, PACKET_C2S_NBT_RESPONSE_DATA ->
            {
                // Write Packet Buffer (Slice)
                try
                {
                    /*
                    PacketByteBuf serverReplay = new PacketByteBuf(this.buffer.copy());
                    output.writeBytes(serverReplay.readBytes(serverReplay.readableBytes()));
                     */

                    output.writeBytes(this.buffer.copy());
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#toPacket: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST, PACKET_S2C_METADATA ->
            {
                // Write NBT
                try
                {
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
                }
            }
            default -> Servux.LOGGER.error("ServuxLitematicaPacket#toPacket: Unknown packet type!");
        }
    }

    @Nullable
    public static ServuxLitematicaPacket fromPacket(FriendlyByteBuf input)
    {
        int i = input.readVarInt();
        Type type = getType(i);

        if (type == null)
        {
            // Invalid Type
            Servux.LOGGER.warn("ServuxLitematicaPacket#fromPacket: invalid packet type received");
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
                    return ServuxLitematicaPacket.BlockEntityRequest(input.readBlockPos());
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Block Entity Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_ENTITY_REQUEST ->
            {
                // Read Packet Buffer
                try
                {
                    input.readVarInt(); // todo: old code compat
                    return ServuxLitematicaPacket.EntityRequest(input.readVarInt());
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Entity Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
            {
                try
                {
                    return ServuxLitematicaPacket.SimpleBlockResponse(input.readBlockPos(), (CompoundTag) input.readNbt(NbtAccounter.unlimitedHeap()));
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Block Entity Response from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
            {
                try
                {
                    return ServuxLitematicaPacket.SimpleEntityResponse(input.readVarInt(), (CompoundTag) input.readNbt(NbtAccounter.unlimitedHeap()));
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Entity Response from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_BULK_ENTITY_NBT_REQUEST ->
            {
                try
                {
                    return ServuxLitematicaPacket.BulkNbtRequest(input.readChunkPos(), (CompoundTag) input.readNbt(NbtAccounter.unlimitedHeap()));
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Bulk Entity Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                // Read Packet Buffer Slice
                try
                {
                    return ServuxLitematicaPacket.ResponseS2CData(new FriendlyByteBuf(input.readBytes(input.readableBytes())));
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading S2C Bulk Response Buffer from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_NBT_RESPONSE_DATA ->
            {
                // Read Packet Buffer Slice
                try
                {
                    return ServuxLitematicaPacket.ResponseC2SData(new FriendlyByteBuf(input.readBytes(input.readableBytes())));
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading C2S Bulk Response Buffer from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST ->
            {
                // Read Nbt
                try
                {
                    return ServuxLitematicaPacket.MetadataRequest(input.readNbt());
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Metadata Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_METADATA ->
            {
                // Read Nbt
                try
                {
                    return ServuxLitematicaPacket.MetadataResponse(input.readNbt());
                }
                catch (Exception e)
                {
                    Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Metadata Response from packet: [{}]", e.getLocalizedMessage());
                }
            }
            default -> Servux.LOGGER.error("ServuxLitematicaPacket#fromPacket: Unknown packet type!");
        }

        return null;
    }

    @Override
    public void clear()
    {
        if (this.nbt != null && !this.nbt.isEmpty())
        {
            this.nbt = new CompoundTag();
        }
        this.clearPacket();
        this.transactionId = -1;
        this.entityId = -1;
        this.pos = BlockPos.ZERO;
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
        PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE(5),
        PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE(6),
        PACKET_C2S_BULK_ENTITY_NBT_REQUEST(7),
        // For Packet Splitter (Oversize Packets, S2C)
        PACKET_S2C_NBT_RESPONSE_START(10),
        PACKET_S2C_NBT_RESPONSE_DATA(11),
        // For Packet Splitter (Oversize Packets, C2S)
        PACKET_C2S_NBT_RESPONSE_START(12),
        PACKET_C2S_NBT_RESPONSE_DATA(13);

        private final int type;

        Type(int type)
        {
            this.type = type;
        }

        int get() { return this.type; }
    }

    public record Payload(ServuxLitematicaPacket data) implements CustomPacketPayload
    {
        public static final CustomPacketPayload.Type<@NotNull Payload> ID = new CustomPacketPayload.Type<>(ServuxLitematicaHandler.CHANNEL_ID);
        public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull Payload> CODEC = CustomPacketPayload.codec(Payload::write, Payload::new);

        public Payload(FriendlyByteBuf input)
        {
            this(fromPacket(input));
        }

        private void write(FriendlyByteBuf output)
        {
            data.toPacket(output);
        }

        @Override
        public CustomPacketPayload.@NotNull Type<? extends @NotNull CustomPacketPayload> type()
        {
            return ID;
        }
    }
}
