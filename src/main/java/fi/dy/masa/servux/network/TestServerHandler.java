package fi.dy.masa.servux.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import fi.dy.masa.malilib.network.handler.server.IPluginServerPlayHandler;
import fi.dy.masa.malilib.network.handler.server.ServerPlayHandler;
import fi.dy.masa.malilib.network.payload.PayloadCodec;
import fi.dy.masa.malilib.network.payload.PayloadManager;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.channel.MaLiLibTestPayload;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;

public abstract class TestServerHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private final static TestServerHandler<MaLiLibTestPayload> INSTANCE = new TestServerHandler<>()
    {
        @Override
        public void receive(MaLiLibTestPayload payload, ServerPlayNetworking.Context context)
        {
            TestServerHandler.INSTANCE.receiveC2SPlayPayload(PayloadType.MALILIB_TEST, payload, context);
        }
    };
    public static TestServerHandler<MaLiLibTestPayload> getInstance() { return INSTANCE; }
    private boolean testRegistered;

    @Override
    public PayloadType getPayloadType() { return PayloadType.MALILIB_TEST; }

    @Override
    public void decodeC2SNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player)
    {
        Servux.printDebug("TestClientHandler#decodeS2CNbtCompound(): received data of size {} bytes from player {}", data.getSizeInBytes(), player);
        String test = data.getString("message");

        Servux.logger.error("test message: {}", test);

        NbtCompound nbt = new NbtCompound();
        nbt.putString("message", "packet reply from "+ Reference.MOD_STRING);
        this.encodeS2CNbtCompound(type, nbt, player);
    }

    @Override
    public void reset(PayloadType type)
    {
        if (type.equals(getPayloadType()) && this.testRegistered)
        {
            this.testRegistered = false;
        }
    }

    @Override
    public void registerPlayPayload(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered() == false)
        {
            PayloadManager.getInstance().registerPlayChannel(getPayloadType(), MaLiLibTestPayload.TYPE, MaLiLibTestPayload.CODEC);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            PayloadManager.getInstance().registerPlayHandler((CustomPayload.Id<T>) MaLiLibTestPayload.TYPE, this);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            PayloadManager.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) MaLiLibTestPayload.TYPE);
        }
    }

    @Override
    public <P extends CustomPayload> void receiveC2SPlayPayload(PayloadType type, P payload, ServerPlayNetworking.Context ctx)
    {
        ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).decodeC2SNbtCompound(this.getPayloadType(), ((MaLiLibTestPayload) payload).data(), ctx.player());
    }

    @Override
    public void encodeS2CNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player)
    {
        MaLiLibTestPayload payload = new MaLiLibTestPayload(data);

        this.sendS2CPlayPayload(this.getPayloadType(), payload, player);
    }

    @Override
    public <P extends CustomPayload> void sendS2CPlayPayload(PayloadType type, P payload, ServerPlayerEntity player)
    {
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public <P extends CustomPayload> void sendS2CPlayPayload(PayloadType type, P payload, ServerPlayNetworkHandler handler)
    {
        Packet<?> packet = new CustomPayloadS2CPacket(payload);

        if (handler != null && handler.accepts(packet))
        {
            handler.sendPacket(packet);
        }
    }
}
