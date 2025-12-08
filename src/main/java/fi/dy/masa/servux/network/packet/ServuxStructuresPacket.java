package fi.dy.masa.servux.network.packet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IServerPayloadData;

public class ServuxStructuresPacket implements IServerPayloadData
{
    private Type packetType;
    private CompoundTag nbt;
    private FriendlyByteBuf buffer;
    public static final int PROTOCOL_VERSION = 2;

    public ServuxStructuresPacket(Type type, @Nullable CompoundTag nbt)
    {
        this.packetType = type;

        if (nbt != null && nbt.isEmpty() == false)
        {
            this.nbt = new CompoundTag();
            this.nbt.merge(nbt);
        }
        if (this.buffer != null)
        {
            this.buffer.clear();
            this.buffer = new FriendlyByteBuf(Unpooled.buffer());
        }
    }

    public ServuxStructuresPacket(Type type, @Nonnull FriendlyByteBuf packet)
    {
        this.packetType = type;
        this.nbt = new CompoundTag();
        this.buffer = new FriendlyByteBuf(packet.copy());
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

    public CompoundTag getCompound()
    {
        return this.nbt;
    }

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

        if (this.packetType.equals(Type.PACKET_S2C_STRUCTURE_DATA))
        {
            // Write Packet Buffer
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
                Servux.LOGGER.error("ServuxStructuresPacket#toPacket: error writing data to packet: [{}]", e.getLocalizedMessage());
            }
        }
        else
        {
            // Write NBT
            try
            {
                output.writeNbt(this.nbt);
            }
            catch (Exception e)
            {
                Servux.LOGGER.error("ServuxStructuresPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
            }
        }
    }

    @Nullable
    public static ServuxStructuresPacket fromPacket(FriendlyByteBuf input)
    {
        int i = input.readVarInt();
        Type type = getType(i);

        if (type == null)
        {
            // Invalid Type
            Servux.LOGGER.warn("ServuxStructuresPacket#fromPacket: invalid packet type received");
        }
        else if (type.equals(Type.PACKET_S2C_STRUCTURE_DATA))
        {
            // Read Packet Buffer
            try
            {
                return new ServuxStructuresPacket(type, new FriendlyByteBuf(input.readBytes(input.readableBytes())));
            }
            catch (Exception e)
            {
                Servux.LOGGER.error("ServuxStructuresPacket#fromPacket: error reading Buffer from packet: [{}]", e.getLocalizedMessage());
            }
        }
        else
        {
            // Read Nbt
            try
            {
                return new ServuxStructuresPacket(type, input.readNbt());
            }
            catch (Exception e)
            {
                Servux.LOGGER.error("ServuxStructuresPacket#fromPacket: error reading NBT from packet: [{}]", e.getLocalizedMessage());
            }
        }

        return null;
    }

    @Override
    public void clear()
    {
        if (this.nbt != null && this.nbt.isEmpty() == false)
        {
            this.nbt = new CompoundTag();
        }
        if (this.buffer != null && this.buffer.readableBytes() > 0)
        {
            this.buffer.clear();
            this.buffer = new FriendlyByteBuf(Unpooled.buffer());
        }

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
        PACKET_S2C_STRUCTURE_DATA(2),
        PACKET_C2S_STRUCTURES_REGISTER(3),
        PACKET_C2S_STRUCTURES_UNREGISTER(4),
        PACKET_S2C_STRUCTURE_DATA_START(5),
        PACKET_S2C_SPAWN_METADATA(10),
        PACKET_C2S_REQUEST_SPAWN_METADATA(11),
        PACKET_S2C_WEATHER_DATA(12);

        private final int type;

        Type(int type)
        {
            this.type = type;
        }

        int get() { return this.type; }
    }

    public record Payload(ServuxStructuresPacket data) implements CustomPacketPayload
    {
        public static final CustomPacketPayload.Type<@NotNull Payload> ID = new CustomPacketPayload.Type<>(ServuxStructuresHandler.CHANNEL_ID);
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
        public CustomPacketPayload.@NotNull Type<@NotNull Payload> type()
        {
            return ID;
        }
    }
}
