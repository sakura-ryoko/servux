package fi.dy.masa.servux.network.packet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IServerPayloadData;

public class ServuxEntitiesPacket implements IServerPayloadData
{
    private Type packetType;
    private NbtCompound nbt;
    private PacketByteBuf buffer;
    public static final int PROTOCOL_VERSION = 1;

    public ServuxEntitiesPacket(Type type, @Nullable NbtCompound nbt)
    {
        this.packetType = type;

        if (nbt != null && nbt.isEmpty() == false)
        {
            this.nbt = new NbtCompound();
            this.nbt.copyFrom(nbt);
        }
        if (this.buffer != null)
        {
            this.buffer.clear();
            this.buffer = new PacketByteBuf(Unpooled.buffer());
        }
    }

    public ServuxEntitiesPacket(Type type, @Nonnull PacketByteBuf packet)
    {
        this.packetType = type;
        this.nbt = new NbtCompound();
        this.buffer = packet;
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

        if (this.packetType.equals(Type.PACKET_S2C_ENTITY_DATA))
        {
            // Write Packet Buffer
            try
            {
                output.writeBytes(this.buffer.readBytes(this.buffer.readableBytes()));
            }
            catch (Exception e)
            {
                Servux.logger.error("ServuxEntitiesPacket#toPacket: error writing data to packet: [{}]", e.getLocalizedMessage());
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
                Servux.logger.error("ServuxEntitiesPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
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
        }
        else if (type.equals(Type.PACKET_S2C_ENTITY_DATA))
        {
            // Read Packet Buffer
            try
            {
                return new ServuxEntitiesPacket(type, new PacketByteBuf(input.readBytes(input.readableBytes())));
            }
            catch (Exception e)
            {
                Servux.logger.error("ServuxEntitiesPacket#fromPacket: error reading Buffer from packet: [{}]", e.getLocalizedMessage());
            }
        }
        else
        {
            // Read Nbt
            try
            {
                return new ServuxEntitiesPacket(type, input.readNbt());
            }
            catch (Exception e)
            {
                Servux.logger.error("ServuxEntitiesPacket#fromPacket: error reading NBT from packet: [{}]", e.getLocalizedMessage());
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
        if (this.buffer != null && this.buffer.readableBytes() > 0)
        {
            this.buffer.clear();
            this.buffer = new PacketByteBuf(Unpooled.buffer());
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
        PACKET_C2S_REQUEST_METADATA(2),
        PACKET_C2S_ENTITY_REGISTER(3),
        PACKET_C2S_ENTITY_UNREGISTER(4),
        PACKET_S2C_ENTITY_DATA_START(5),
        PACKET_S2C_ENTITY_DATA(6),
        PACKET_C2S_ENTITY_REQUEST(7),
        PACKET_S2C_ENTITY_REQUEST_DENIED(8);

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
        public Id<Payload> getId()
        {
            return ID;
        }
    }
}
