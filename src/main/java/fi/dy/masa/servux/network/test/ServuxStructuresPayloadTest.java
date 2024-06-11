package fi.dy.masa.servux.network.test;

import org.jetbrains.annotations.ApiStatus;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * ServuX Structures Data Provider; the actual Payload shared between MiniHUD and ServuX
 */
@ApiStatus.Experimental
public record ServuxStructuresPayloadTest(ServuxStructuresDataTest data) implements CustomPayload
{
    public static final Id<ServuxStructuresPayloadTest> TYPE = new Id<>(ServuxStructuresHandlerTest.CHANNEL_ID);
    public static final PacketCodec<PacketByteBuf, ServuxStructuresPayloadTest> CODEC = CustomPayload.codecOf(ServuxStructuresPayloadTest::write, ServuxStructuresPayloadTest::new);

    public ServuxStructuresPayloadTest(PacketByteBuf input)
    {
        this(ServuxStructuresDataTest.fromPacket(input));
    }

    private void write(PacketByteBuf output)
    {
        data.toPacket(output);
    }

    @Override
    public Id<ServuxStructuresPayloadTest> getId() { return TYPE; }
}
