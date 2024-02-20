package fi.dy.masa.servux.network.payload.channel;

import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.PayloadTypeRegister;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Intended as a new future Servux Data Provider for providing server-based Metadata / Rules (I.e, SPAWN_CHUNK_RADIUS, etc)
 */
public record ServuxS2CMetadataPayload(NbtCompound data) implements CustomPayload
{
    public static final Id<ServuxS2CMetadataPayload> TYPE = new Id<>(PayloadTypeRegister.INSTANCE.getIdentifier(PayloadType.SERVUX_METADATA));
    public static final PacketCodec<PacketByteBuf, ServuxS2CMetadataPayload> CODEC = CustomPayload.codecOf(ServuxS2CMetadataPayload::write, ServuxS2CMetadataPayload::new);
    public static final String KEY = PayloadTypeRegister.INSTANCE.getKey(PayloadType.SERVUX_METADATA);

    public ServuxS2CMetadataPayload(PacketByteBuf buf) { this(buf.readNbt()); }

    private void write(PacketByteBuf buf) { buf.writeNbt(data); }

    @Override
    public Id<? extends CustomPayload> getId() { return TYPE; }
}
