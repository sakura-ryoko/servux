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
public record ServuxS2CBlocksPayload(NbtCompound data) implements CustomPayload
{
    public static final Id<ServuxS2CBlocksPayload> TYPE = new Id<>(PayloadTypeRegister.INSTANCE.getIdentifier(PayloadType.SERVUX_BLOCKS));
    public static final PacketCodec<PacketByteBuf, ServuxS2CBlocksPayload> CODEC = CustomPayload.codecOf(ServuxS2CBlocksPayload::write, ServuxS2CBlocksPayload::new);
    public static final String KEY = PayloadTypeRegister.INSTANCE.getKey(PayloadType.SERVUX_BLOCKS);

    public ServuxS2CBlocksPayload(PacketByteBuf buf) { this(buf.readNbt()); }

    private void write(PacketByteBuf buf) { buf.writeNbt(data); }

    @Override
    public Id<? extends CustomPayload> getId() { return TYPE; }
}
