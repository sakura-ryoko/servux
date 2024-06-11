package fi.dy.masa.servux.network.test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import fi.dy.masa.servux.Servux;

@ApiStatus.Experimental
public class ServuxStructuresDataTest
{
    private NbtCompound NBT;
    private PacketByteBuf BUFFER;
    private int packetType;

    public ServuxStructuresDataTest(int packetType, @Nullable NbtCompound nbtCompound)
    {
        this.packetType = packetType;

        if (nbtCompound != null && nbtCompound.isEmpty() == false)
        {
            this.NBT = new NbtCompound();
            this.NBT.copyFrom(nbtCompound);
        }
        if (this.BUFFER != null)
        {
            this.BUFFER.clear();
            this.BUFFER = new PacketByteBuf(Unpooled.buffer());
        }
    }

    public ServuxStructuresDataTest(int packetType, @Nonnull PacketByteBuf packet)
    {
        this.packetType = packetType;
        this.BUFFER = packet;
    }

    public int getPacketType() { return this.packetType; }

    @Nullable
    public PacketByteBuf getPacket() { return this.BUFFER; }

    @Nullable
    public NbtCompound getCompound() { return this.NBT; }

    public boolean hasPacket() { return this.BUFFER != null && this.BUFFER.isReadable(); }

    public boolean hasNbt() { return this.NBT != null && this.NBT.isEmpty() == false; }

    public int getTotalSize()
    {
        int total = 1;

        if (this.NBT != null && this.NBT.isEmpty() == false)
        {
            total += this.NBT.getSizeInBytes();
        }

        if (this.BUFFER != null && this.BUFFER.isReadable())
        {
            total += this.BUFFER.readableBytes();
        }

        return total;
    }

    public void fromBufferToNbt()
    {
        if (this.packetType == ServuxStructuresHandlerTest.PACKET_S2C_STRUCTURE_DATA)
        {
            if (this.BUFFER != null && this.BUFFER.readableBytes() > 0)
            {
                try
                {
                    this.NBT = this.BUFFER.readNbt();
                }
                catch (Exception e)
                {
                    Servux.logger.error("fromPacketToNbt: error reading packet: [{}]", e.getLocalizedMessage());
                }
            }

            Servux.logger.error("fromPacketToNbt: Packet Buffer is empty / null");
        }
    }

    public void toBufferFromNbt()
    {
        if (this.packetType == ServuxStructuresHandlerTest.PACKET_S2C_STRUCTURE_DATA)
        {
            if (this.BUFFER != null)
            {
                this.BUFFER.release();
            }

            if (this.NBT == null || this.NBT.isEmpty())
            {
                Servux.logger.error("toPacketFromNbt: NbtCompound is empty");
                return;
            }

            try
            {
                this.BUFFER = new PacketByteBuf(Unpooled.buffer());
                this.BUFFER.writeNbt(this.NBT);
            }
            catch (Exception e)
            {
                Servux.logger.error("toPacketFromNbt: error writing to packet: [{}]", e.getLocalizedMessage());
            }
        }
    }

    @Nullable
    public static ServuxStructuresDataTest fromPacket(PacketByteBuf input)
    {
        int type = input.readVarInt();

        if (type == ServuxStructuresHandlerTest.PACKET_S2C_STRUCTURE_DATA)
        {
            try
            {
                return new ServuxStructuresDataTest(type, new PacketByteBuf(input.readBytes(input.readableBytes())));
            }
            catch (Exception e)
            {
                Servux.logger.error("fromPacket: error reading Buffer from packet: [{}]", e.getLocalizedMessage());
            }
        }
        else
        {
            try
            {
                return new ServuxStructuresDataTest(type, input.readNbt());
            }
            catch (Exception e)
            {
                Servux.logger.error("fromPacket: error reading NBT from packet: [{}]", e.getLocalizedMessage());
            }
        }

        return null;
    }

    public void toPacket(PacketByteBuf output)
    {
        if (this.packetType == ServuxStructuresHandlerTest.PACKET_S2C_STRUCTURE_DATA)
        {
            try
            {
                output.writeVarInt(this.packetType);
                output.writeBytes(this.BUFFER.readBytes(this.BUFFER.readableBytes()));
            }
            catch (Exception e)
            {
                Servux.logger.error("toPacket: error writing data to packet: [{}]", e.getLocalizedMessage());
            }
        }
        else
        {
            try
            {
                output.writeVarInt(this.packetType);
                output.writeNbt(this.NBT);
            }
            catch (Exception e)
            {
                Servux.logger.error("toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
            }
        }
    }

    public void reset()
    {
        this.packetType = -1;

        if (this.BUFFER != null)
        {
            this.BUFFER.clear();
            this.BUFFER = new PacketByteBuf(Unpooled.buffer());
        }
        this.NBT = new NbtCompound();
    }
}
