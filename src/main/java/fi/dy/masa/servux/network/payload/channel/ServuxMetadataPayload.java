package fi.dy.masa.servux.network.payload.channel;

import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.PayloadTypeRegister;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ServuxMetadataPayload(NbtCompound data) implements CustomPayload
{
    public static final Id<ServuxMetadataPayload> TYPE = new Id<>(PayloadTypeRegister.getIdentifier(PayloadType.SERVUX_METADATA));
    public static final PacketCodec<PacketByteBuf, ServuxMetadataPayload> CODEC = CustomPayload.codecOf(ServuxMetadataPayload::write, ServuxMetadataPayload::new);
    public static final String KEY = PayloadTypeRegister.getKey(PayloadType.SERVUX_METADATA);

    public ServuxMetadataPayload(PacketByteBuf buf) { this(buf.readNbt()); }

    private void write(PacketByteBuf buf) { buf.writeNbt(data); }

    @Override
    public Id<? extends CustomPayload> getId() { return TYPE; }
}
