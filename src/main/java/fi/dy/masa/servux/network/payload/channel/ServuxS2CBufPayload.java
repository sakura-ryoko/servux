package fi.dy.masa.servux.network.payload.channel;

import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.PayloadTypeRegister;
import fi.dy.masa.servux.network.payload.ServuxByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Example Payload Type for extending a PacketByteBuf
 */
public record ServuxS2CBufPayload(ServuxByteBuf byteBuf) implements CustomPayload
{
    public static final Id<ServuxS2CBufPayload> TYPE = new Id<>(PayloadTypeRegister.INSTANCE.getIdentifier(PayloadType.SERVUX_BYTEBUF));
    public static final PacketCodec<PacketByteBuf, ServuxS2CBufPayload> CODEC = CustomPayload.codecOf(ServuxS2CBufPayload::write, ServuxS2CBufPayload::new);

    public ServuxS2CBufPayload(PacketByteBuf input)
    {
        this(new ServuxByteBuf(input.readBytes(input.readableBytes())));
    }

    private void write(PacketByteBuf output)
    {
        output.writeBytes(byteBuf);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return TYPE;
    }
}
