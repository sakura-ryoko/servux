package fi.dy.masa.servux.paper.network;

import javax.annotation.Nullable;

import fi.dy.masa.servux.paper.ServuxPaperReference;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Minimal Paper-side mirror of Servux's {@code ServuxEntitiesPacket} (Fabric side), covering only
 * the custom-channel request/response path: metadata handshake, block-entity request, and entity
 * request. The vanilla {@code NbtQuery} permission-override path (a separate feature entirely,
 * patching vanilla packet handling rather than this channel) is intentionally not implemented.
 * <p>
 * Fabric's bulk-request path ({@code PACKET_S2C_NBT_RESPONSE_START}/{@code _DATA}(10/11) and
 * {@code PACKET_C2S_NBT_RESPONSE_START}/{@code _DATA}(12/13)) is unfinished even in the Fabric
 * source itself (the C2S handler is commented out) - nothing working to port yet.
 *
 * @see <a href="../../../../../../../../../../src/main/java/fi/dy/masa/servux/network/packet/ServuxEntitiesPacket.java">ServuxEntitiesPacket.java (Fabric reference)</a>
 */
public class ServuxEntitiesPacket
{
    public static final int PROTOCOL_VERSION = 1;

    public enum Type
    {
        PACKET_S2C_METADATA(1),
        PACKET_C2S_METADATA_REQUEST(2),
        PACKET_C2S_BLOCK_ENTITY_REQUEST(3),
        PACKET_C2S_ENTITY_REQUEST(4),
        PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE(5),
        PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE(6);

        private final int id;

        Type(int id)
        {
            this.id = id;
        }

        int get() { return this.id; }
    }

    private final Type type;
    private final CompoundTag nbt;
    private final BlockPos pos;
    private final int entityId;

    private ServuxEntitiesPacket(Type type, @Nullable CompoundTag nbt, @Nullable BlockPos pos, int entityId)
    {
        this.type = type;
        this.nbt = nbt;
        this.pos = pos;
        this.entityId = entityId;
    }

    public static ServuxEntitiesPacket MetadataResponse(CompoundTag nbt)
    {
        return new ServuxEntitiesPacket(Type.PACKET_S2C_METADATA, nbt, null, -1);
    }

    public static ServuxEntitiesPacket SimpleBlockResponse(BlockPos pos, CompoundTag nbt)
    {
        return new ServuxEntitiesPacket(Type.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE, nbt, pos, -1);
    }

    public static ServuxEntitiesPacket SimpleEntityResponse(int entityId, CompoundTag nbt)
    {
        return new ServuxEntitiesPacket(Type.PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE, nbt, null, entityId);
    }

    public Type getType()
    {
        return this.type;
    }

    public CompoundTag getCompound()
    {
        return this.nbt;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }

    public int getEntityId()
    {
        return this.entityId;
    }

    public byte[] toBytes()
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(this.type.get());

        try
        {
            switch (this.type)
            {
                case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
                {
                    buffer.writeBlockPos(this.pos);
                    buffer.writeNbt(this.nbt);
                }
                case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
                {
                    buffer.writeVarInt(this.entityId);
                    buffer.writeNbt(this.nbt);
                }
                default -> buffer.writeNbt(this.nbt);
            }
        }
        catch (Exception e)
        {
            ServuxPaperReference.logger().error("ServuxEntitiesPacket#toBytes: error writing {} to packet: [{}]", this.type, e.getLocalizedMessage());
        }

        return ByteBufUtil.getBytes(buffer);
    }

    /** Only ever receives C2S_METADATA_REQUEST/C2S_BLOCK_ENTITY_REQUEST/C2S_ENTITY_REQUEST. */
    @Nullable
    public static ServuxEntitiesPacket fromBytes(byte[] data)
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int typeId = buffer.readVarInt();
        Type type = getType(typeId);

        if (type == null)
        {
            ServuxPaperReference.logger().warn("ServuxEntitiesPacket#fromBytes: invalid packet type received");
            return null;
        }

        try
        {
            switch (type)
            {
                case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
                {
                    buffer.readVarInt(); // transaction id - unused, kept for wire compatibility
                    BlockPos pos = buffer.readBlockPos();
                    return new ServuxEntitiesPacket(type, new CompoundTag(), pos, -1);
                }
                case PACKET_C2S_ENTITY_REQUEST ->
                {
                    buffer.readVarInt(); // transaction id - unused, kept for wire compatibility
                    int entityId = buffer.readVarInt();
                    return new ServuxEntitiesPacket(type, new CompoundTag(), null, entityId);
                }
                case PACKET_C2S_METADATA_REQUEST ->
                {
                    CompoundTag nbt = buffer.readableBytes() > 0 ? buffer.readNbt() : new CompoundTag();
                    return new ServuxEntitiesPacket(type, nbt != null ? nbt : new CompoundTag(), null, -1);
                }
                default ->
                {
                    return null;
                }
            }
        }
        catch (Exception e)
        {
            ServuxPaperReference.logger().error("ServuxEntitiesPacket#fromBytes: error reading {} from packet: [{}]", type, e.getLocalizedMessage());
            return null;
        }
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
}
