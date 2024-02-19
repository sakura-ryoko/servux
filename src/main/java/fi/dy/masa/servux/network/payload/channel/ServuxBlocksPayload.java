package fi.dy.masa.servux.network.payload.channel;

import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.PayloadTypeRegister;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Intended as a new future Servux Data Provider for sending Block NBT data (i.e., Inventory contents, etc.)
 */
public record ServuxBlocksPayload(NbtCompound data) implements CustomPayload
{
    public static final Id<ServuxBlocksPayload> TYPE = new Id<>(PayloadTypeRegister.INSTANCE.getIdentifier(PayloadType.SERVUX_BLOCKS));
    public static final PacketCodec<PacketByteBuf, ServuxBlocksPayload> CODEC = CustomPayload.codecOf(ServuxBlocksPayload::write, ServuxBlocksPayload::new);
    public static final String KEY = PayloadTypeRegister.INSTANCE.getKey(PayloadType.SERVUX_BLOCKS);

    public ServuxBlocksPayload(PacketByteBuf buf) { this(buf.readNbt()); }

    private void write(PacketByteBuf buf) { buf.writeNbt(data); }

    @Override
    public Id<? extends CustomPayload> getId() { return TYPE; }
}
