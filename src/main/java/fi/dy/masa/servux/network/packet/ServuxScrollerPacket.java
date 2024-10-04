package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IServerPayloadData;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ServuxScrollerPacket implements IServerPayloadData
{
    private Type packetType;
    private NbtCompound nbt;
    private PacketByteBuf buffer;
    public static final int PROTOCOL_VERSION = 1;

    private ServuxScrollerPacket(Type type)
    {
        this.packetType = type;
        this.nbt = new NbtCompound();
        this.clearPacket();
    }

    public static ServuxScrollerPacket MetadataRequest(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxScrollerPacket(Type.PACKET_C2S_METADATA_REQUEST);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    public static ServuxScrollerPacket MetadataResponse(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxScrollerPacket(Type.PACKET_S2C_METADATA);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    public static ServuxScrollerPacket MassCraftRequest(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxScrollerPacket(Type.PACKET_C2S_MASS_CRAFT_REQUEST);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    public static ServuxScrollerPacket MassCraftResponse(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxScrollerPacket(Type.PACKET_S2C_MASS_CRAFT_RESPONSE);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    public static ServuxScrollerPacket RecipeManagerRequest(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxScrollerPacket(Type.PACKET_C2S_RECIPE_MANAGER_REQUEST);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    // Nbt Packet, using Packet Splitter
    public static ServuxScrollerPacket ResponseS2CStart(@Nonnull NbtCompound nbt)
    {
        var packet = new ServuxScrollerPacket(Type.PACKET_S2C_NBT_RESPONSE_START);
        packet.nbt.copyFrom(nbt);
        return packet;
    }

    public static ServuxScrollerPacket ResponseS2CData(@Nonnull PacketByteBuf buffer)
    {
        var packet = new ServuxScrollerPacket(Type.PACKET_S2C_NBT_RESPONSE_DATA);
        packet.buffer = buffer;
        packet.nbt = new NbtCompound();
        return packet;
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

        if (this.nbt != null && !this.nbt.isEmpty())
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

        switch (this.packetType)
        {
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                // Write Packet Buffer (Slice)
                try
                {
                    output.writeBytes(this.buffer.readBytes(this.buffer.readableBytes()));
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxHudPacket#toPacket: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST, PACKET_S2C_METADATA, PACKET_C2S_MASS_CRAFT_REQUEST, PACKET_S2C_MASS_CRAFT_RESPONSE, PACKET_C2S_RECIPE_MANAGER_REQUEST ->
            {
                // Write NBT
                try
                {
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxHudPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
                }
            }
            default -> Servux.logger.error("ServuxHudPacket#toPacket: Unknown packet type!");
        }
    }

    @Nullable
    public static ServuxScrollerPacket fromPacket(PacketByteBuf input)
    {
        int i = input.readVarInt();
        Type type = getType(i);

        if (type == null)
        {
            // Invalid Type
            Servux.logger.warn("ServuxHudPacket#fromPacket: invalid packet type received");
            return null;
        }
        switch (type)
        {
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                // Read Packet Buffer Slice
                try
                {
                    return ServuxScrollerPacket.ResponseS2CData(new PacketByteBuf(input.readBytes(input.readableBytes())));
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxHudPacket#fromPacket: error reading S2C Bulk Response Buffer from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST ->
            {
                // Read Nbt
                try
                {
                    return ServuxScrollerPacket.MetadataRequest(input.readNbt());
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxHudPacket#fromPacket: error reading Metadata Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_METADATA ->
            {
                // Read Nbt
                try
                {
                    return ServuxScrollerPacket.MetadataResponse(input.readNbt());
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxHudPacket#fromPacket: error reading Metadata Response from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_MASS_CRAFT_REQUEST ->
            {
                // Read Nbt
                try
                {
                    return ServuxScrollerPacket.MassCraftRequest(input.readNbt());
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxHudPacket#fromPacket: error reading Mass Craft Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_MASS_CRAFT_RESPONSE ->
            {
                // Read Nbt
                try
                {
                    return ServuxScrollerPacket.MassCraftResponse(input.readNbt());
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxHudPacket#fromPacket: error reading Mass Craft Response from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_RECIPE_MANAGER_REQUEST ->
            {
                // Read Nbt
                try
                {
                    return ServuxScrollerPacket.RecipeManagerRequest(input.readNbt());
                }
                catch (Exception e)
                {
                    Servux.logger.error("ServuxHudPacket#fromPacket: error reading Recipe Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            default -> Servux.logger.error("ServuxHudPacket#fromPacket: Unknown packet type!");
        }

        return null;
    }

    @Override
    public void clear()
    {
        if (this.nbt != null && !this.nbt.isEmpty())
        {
            this.nbt = new NbtCompound();
        }
        this.clearPacket();
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
        PACKET_C2S_MASS_CRAFT_REQUEST(3),
        PACKET_S2C_MASS_CRAFT_RESPONSE(4),
        PACKET_C2S_RECIPE_MANAGER_REQUEST(6),
        // For Packet Splitter (Oversize Packets, S2C)
        PACKET_S2C_NBT_RESPONSE_START(10),
        PACKET_S2C_NBT_RESPONSE_DATA(11);

        private final int type;

        Type(int type)
        {
            this.type = type;
        }

        int get() { return this.type; }
    }

    public record Payload(ServuxScrollerPacket data) implements CustomPayload
    {
        public static final Id<Payload> ID = new Id<>(ServuxScrollerHandler.CHANNEL_ID);
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
