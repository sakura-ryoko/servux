package fi.dy.masa.servux.network.payload.channel;

import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.PayloadTypeRegister;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * The functional Production Servux Structures Data Provider.
 */
public record ServuxS2CStructuresPayload(NbtCompound data) implements CustomPayload
{
    public static final Id<ServuxS2CStructuresPayload> TYPE = new Id<>(PayloadTypeRegister.INSTANCE.getIdentifier(PayloadType.SERVUX_STRUCTURES));
    public static final PacketCodec<PacketByteBuf, ServuxS2CStructuresPayload> CODEC = CustomPayload.codecOf(ServuxS2CStructuresPayload::write, ServuxS2CStructuresPayload::new);
    public static final String KEY = PayloadTypeRegister.INSTANCE.getKey(PayloadType.SERVUX_STRUCTURES);

    public ServuxS2CStructuresPayload(PacketByteBuf buf) { this(buf.readNbt()); }

    private void write(PacketByteBuf buf) { buf.writeNbt(data); }

    @Override
    public Id<? extends CustomPayload> getId() { return TYPE; }
}
