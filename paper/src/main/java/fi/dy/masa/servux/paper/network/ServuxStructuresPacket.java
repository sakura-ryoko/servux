package fi.dy.masa.servux.paper.network;

import javax.annotation.Nullable;

import fi.dy.masa.servux.paper.ServuxPaperReference;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Minimal Paper-side mirror of Servux's {@code ServuxStructuresPacket} (Fabric side), covering
 * only the packet types needed for the structures channel: metadata handshake,
 * register/unregister, and the (possibly split) structure-data payload.
 * <p>
 * Fabric also defines {@code PACKET_S2C_SPAWN_METADATA}/{@code PACKET_C2S_REQUEST_SPAWN_METADATA}/
 * {@code PACKET_S2C_WEATHER_DATA} on this same channel - not implemented here, since hud_data
 * already covers spawn/weather on its own channel.
 *
 * @see <a href="../../../../../../../../../../src/main/java/fi/dy/masa/servux/network/packet/ServuxStructuresPacket.java">ServuxStructuresPacket.java (Fabric reference)</a>
 */
public class ServuxStructuresPacket
{
    public static final int PROTOCOL_VERSION = 2;

    public enum Type
    {
        PACKET_S2C_METADATA(1),
        PACKET_S2C_STRUCTURE_DATA(2),
        PACKET_C2S_STRUCTURES_REGISTER(3),
        PACKET_C2S_STRUCTURES_UNREGISTER(4),
        PACKET_S2C_STRUCTURE_DATA_START(5);

        private final int id;

        Type(int id)
        {
            this.id = id;
        }

        int get() { return this.id; }
    }

    private final Type type;
    private final CompoundTag nbt;
    private final byte[] rawPayload;

    private ServuxStructuresPacket(Type type, @Nullable CompoundTag nbt, @Nullable byte[] rawPayload)
    {
        this.type = type;
        this.nbt = nbt;
        this.rawPayload = rawPayload;
    }

    public static ServuxStructuresPacket Metadata(CompoundTag nbt)
    {
        return new ServuxStructuresPacket(Type.PACKET_S2C_METADATA, nbt, null);
    }

    public static ServuxStructuresPacket StructureDataStart(CompoundTag nbt)
    {
        return new ServuxStructuresPacket(Type.PACKET_S2C_STRUCTURE_DATA_START, nbt, null);
    }

    /** Raw-bytes fragment produced by {@link PacketSplitter} - not NBT-wrapped, matches Fabric's wire format. */
    public static ServuxStructuresPacket StructureDataFragment(byte[] rawPayload)
    {
        return new ServuxStructuresPacket(Type.PACKET_S2C_STRUCTURE_DATA, null, rawPayload);
    }

    public Type getType()
    {
        return this.type;
    }

    public CompoundTag getCompound()
    {
        return this.nbt;
    }

    public byte[] toBytes()
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(this.type.get());

        if (this.type == Type.PACKET_S2C_STRUCTURE_DATA)
        {
            try
            {
                buffer.writeBytes(this.rawPayload);
            }
            catch (Exception e)
            {
                ServuxPaperReference.logger().error("ServuxStructuresPacket#toBytes: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
            }
        }
        else
        {
            try
            {
                buffer.writeNbt(this.nbt);
            }
            catch (Exception e)
            {
                ServuxPaperReference.logger().error("ServuxStructuresPacket#toBytes: error writing NBT to packet: [{}]", e.getLocalizedMessage());
            }
        }

        return ByteBufUtil.getBytes(buffer);
    }

    /** Only ever receives {@code C2S_STRUCTURES_REGISTER}/{@code UNREGISTER}, both small NBT (possibly empty). */
    @Nullable
    public static ServuxStructuresPacket fromBytes(byte[] data)
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int typeId = buffer.readVarInt();
        Type type = getType(typeId);

        if (type == null)
        {
            ServuxPaperReference.logger().warn("ServuxStructuresPacket#fromBytes: invalid packet type received");
            return null;
        }

        try
        {
            CompoundTag nbt = buffer.readableBytes() > 0 ? buffer.readNbt() : new CompoundTag();
            return new ServuxStructuresPacket(type, nbt != null ? nbt : new CompoundTag(), null);
        }
        catch (Exception e)
        {
            ServuxPaperReference.logger().error("ServuxStructuresPacket#fromBytes: error reading packet data: [{}]", e.getLocalizedMessage());
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
