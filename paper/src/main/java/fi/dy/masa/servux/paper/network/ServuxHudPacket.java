package fi.dy.masa.servux.paper.network;

import javax.annotation.Nullable;

import fi.dy.masa.servux.paper.ServuxPaperReference;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Minimal Paper-side mirror of Servux's {@code ServuxHudPacket} (Fabric side): metadata handshake,
 * spawn data, weather ticks, data-logger (TPS/mob-caps) subscription + ticks, and the recipe
 * manager dump (sent via {@link PacketSplitter} as raw-bytes {@code PACKET_S2C_NBT_RESPONSE_DATA}
 * fragments - the {@code PACKET_S2C_NBT_RESPONSE_START} type is never actually put on the wire,
 * mirroring the Fabric implementation).
 * <p>
 * Wire format must stay byte-identical to the Fabric implementation so an unmodified MiniHUD
 * client accepts it: {@code writeVarInt(packetTypeId)} followed by {@code writeNbt(compoundTag)}
 * (or raw bytes for the splitter fragment type).
 *
 * @see <a href="../../../../../../../../../../src/main/java/fi/dy/masa/servux/network/packet/ServuxHudPacket.java">ServuxHudPacket.java (Fabric reference)</a>
 */
public class ServuxHudPacket
{
    /** Must match {@code ServuxHudPacket.PROTOCOL_VERSION} on the Fabric side. */
    public static final int PROTOCOL_VERSION = 2;

    public enum Type
    {
        PACKET_S2C_METADATA(1),
        PACKET_C2S_METADATA_REQUEST(2),
        PACKET_S2C_SPAWN_DATA(3),
        PACKET_C2S_SPAWN_DATA_REQUEST(4),
        PACKET_S2C_WEATHER_TICK(5),
        PACKET_C2S_RECIPE_MANAGER_REQUEST(6),
        PACKET_S2C_DATA_LOGGER_TICK(7),
        PACKET_C2S_DATA_LOGGER_REQUEST(8),
        PACKET_S2C_NBT_RESPONSE_DATA(11);

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

    private ServuxHudPacket(Type type, @Nullable CompoundTag nbt, @Nullable byte[] rawPayload)
    {
        this.type = type;
        this.nbt = nbt;
        this.rawPayload = rawPayload;
    }

    public static ServuxHudPacket MetadataResponse(CompoundTag nbt)
    {
        return new ServuxHudPacket(Type.PACKET_S2C_METADATA, nbt, null);
    }

    public static ServuxHudPacket SpawnResponse(CompoundTag nbt)
    {
        return new ServuxHudPacket(Type.PACKET_S2C_SPAWN_DATA, nbt, null);
    }

    public static ServuxHudPacket WeatherTick(CompoundTag nbt)
    {
        return new ServuxHudPacket(Type.PACKET_S2C_WEATHER_TICK, nbt, null);
    }

    public static ServuxHudPacket DataLoggerTick(CompoundTag nbt)
    {
        return new ServuxHudPacket(Type.PACKET_S2C_DATA_LOGGER_TICK, nbt, null);
    }

    /** Raw-bytes fragment produced by {@link PacketSplitter} - not NBT-wrapped, matches Fabric's wire format. */
    public static ServuxHudPacket ResponseS2CData(byte[] rawPayload)
    {
        return new ServuxHudPacket(Type.PACKET_S2C_NBT_RESPONSE_DATA, null, rawPayload);
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

        if (this.type == Type.PACKET_S2C_NBT_RESPONSE_DATA)
        {
            try
            {
                buffer.writeBytes(this.rawPayload);
            }
            catch (Exception e)
            {
                ServuxPaperReference.logger().error("ServuxHudPacket#toBytes: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
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
                ServuxPaperReference.logger().error("ServuxHudPacket#toBytes: error writing NBT to packet: [{}]", e.getLocalizedMessage());
            }
        }

        return ByteBufUtil.getBytes(buffer);
    }

    /** Only ever receives C2S_METADATA_REQUEST/C2S_SPAWN_DATA_REQUEST/C2S_RECIPE_MANAGER_REQUEST/C2S_DATA_LOGGER_REQUEST, all small NBT (possibly empty). */
    @Nullable
    public static ServuxHudPacket fromBytes(byte[] data)
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int typeId = buffer.readVarInt();
        Type type = getType(typeId);

        if (type == null)
        {
            ServuxPaperReference.logger().warn("ServuxHudPacket#fromBytes: invalid packet type received");
            return null;
        }

        try
        {
            CompoundTag nbt = buffer.readableBytes() > 0 ? buffer.readNbt() : new CompoundTag();
            return new ServuxHudPacket(type, nbt != null ? nbt : new CompoundTag(), null);
        }
        catch (Exception e)
        {
            ServuxPaperReference.logger().error("ServuxHudPacket#fromBytes: error reading packet data: [{}]", e.getLocalizedMessage());
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

