package fi.dy.masa.servux.network.payload.channel;

import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.PayloadTypeRegister;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Intended as a new future Servux Data Provider for sending Entity NBT data (i.e., Mob Health Information, etc.)
 */
public record ServuxS2CEntitiesPayload(NbtCompound data) implements CustomPayload
{
    public static final Id<ServuxS2CEntitiesPayload> TYPE = new Id<>(PayloadTypeRegister.INSTANCE.getIdentifier(PayloadType.SERVUX_ENTITIES));
    public static final PacketCodec<PacketByteBuf, ServuxS2CEntitiesPayload> CODEC = CustomPayload.codecOf(ServuxS2CEntitiesPayload::write, ServuxS2CEntitiesPayload::new);
    public static final String KEY = PayloadTypeRegister.INSTANCE.getKey(PayloadType.SERVUX_ENTITIES);

    public ServuxS2CEntitiesPayload(PacketByteBuf buf) { this(buf.readNbt()); }

    private void write(PacketByteBuf buf) { buf.writeNbt(data); }

    @Override
    public Id<? extends CustomPayload> getId() { return TYPE; }
}
