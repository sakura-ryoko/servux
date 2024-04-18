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
import fi.dy.masa.malilib.network.payload.channel.ServuxStructuresPayload;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

public abstract class ServuxStructuresHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxStructuresHandler<ServuxStructuresPayload> INSTANCE = new ServuxStructuresHandler<>() {
        @Override
        public void receive(ServuxStructuresPayload payload, ServerPlayNetworking.Context context)
        {
            ServuxStructuresHandler.INSTANCE.receiveC2SPlayPayload(payload, context);
        }
    };
    public static ServuxStructuresHandler<ServuxStructuresPayload> getInstance() { return INSTANCE; }
    private boolean servuxRegistered;

    @Override
    public PayloadType getPayloadType() { return PayloadType.SERVUX_STRUCTURES; }

    @Override
    public void decodeC2SNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player)
    {
        int packetType = data.getInt("packetType");

        if (packetType == PacketType.Structures.PACKET_C2S_STRUCTURES_REGISTER)
        {
            Servux.printDebug("ServuxStructuresHandler#decodeC2SNbtCompound(): received a STRUCTURES_REGISTER packet from player: {}.", player.getName().getLiteralString());

            StructureDataProvider.INSTANCE.register(player, player.getGameProfile());
        }
        else if (packetType == PacketType.Structures.PACKET_C2S_REQUEST_SPAWN_METADATA)
        {
            Servux.printDebug("ServuxStructuresHandler#decodeC2SNbtCompound(): received a REQUEST_SPAWN_METADATA packet from player: {}.", player.getName().getLiteralString());

            StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data);
        }
        else if (packetType == PacketType.Structures.PACKET_C2S_STRUCTURES_UNREGISTER)
        {
            Servux.printDebug("ServuxStructuresHandler#decodeC2SNbtCompound(): received a STRUCTURES_UNREGISTER packet from player: {}.", player.getName().getLiteralString());

            StructureDataProvider.INSTANCE.unregister(player);
        }
        else
        {
            Servux.logger.warn("ServuxStructuresHandler#decodeC2SNbtCompound(): Invalid packetType from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
        }
    }

    @Override
    public void reset(PayloadType type)
    {
        if (type.equals(getPayloadType()) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
        }
    }

    @Override
    public void registerPlayPayload(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered() == false)
        {
            PayloadManager.getInstance().registerPlayChannel(type, ServuxStructuresPayload.TYPE, ServuxStructuresPayload.CODEC);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            PayloadManager.getInstance().registerPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE, this);
            this.servuxRegistered = true;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            reset(getPayloadType());

            PayloadManager.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE);
        }
    }

    @Override
    public <P extends CustomPayload> void receiveC2SPlayPayload(P payload, ServerPlayNetworking.Context ctx)
    {
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;
        ServerPlayerEntity player = ctx.player();

        ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).decodeC2SNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data(), player);
    }

    @Override
    public void encodeS2CNbtCompound(NbtCompound data, ServerPlayerEntity player)
    {
        ServuxStructuresPayload payload = new ServuxStructuresPayload(data);

        ServuxStructuresHandler.INSTANCE.sendS2CPlayPayload(payload, player);
    }

    @Override
    public <P extends CustomPayload> void sendS2CPlayPayload(P payload, ServerPlayerEntity player)
    {
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public <P extends CustomPayload> void sendS2CPlayPayload(P payload, ServerPlayNetworkHandler handler)
    {
        Packet<?> packet = new CustomPayloadS2CPacket(payload);

        if (handler != null && handler.accepts(packet))
        {
            handler.sendPacket(packet);
        }
    }
}
