package fi.dy.masa.servux.network.payload;

import fi.dy.masa.servux.network.PayloadTypeRegister;
import fi.dy.masa.servux.network.PayloadTypes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record StringPayload(String data) implements CustomPayload
{
    public static final Id<StringPayload> TYPE = new Id<>(PayloadTypeRegister.getIdentifier(PayloadTypes.PayloadType.STRING));
    public static final PacketCodec<RegistryByteBuf, StringPayload> CODEC = CustomPayload.codecOf(StringPayload::write, StringPayload::new);
    public StringPayload(PacketByteBuf buf) { this(buf.readString()); }
    public void write(PacketByteBuf buf) { buf.writeString(this.data); }
    @Override
    public Id<? extends CustomPayload> getId() { return TYPE; }
}
