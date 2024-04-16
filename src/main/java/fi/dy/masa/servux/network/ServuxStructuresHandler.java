package fi.dy.masa.servux.network;

import java.util.Objects;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import fi.dy.masa.malilib.network.handler.CommonHandlerRegister;
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
            ServuxStructuresHandler.INSTANCE.receiveC2SPlayPayload(PayloadType.SERVUX_STRUCTURES, payload, context);
        }
    };
    public static ServuxStructuresHandler<ServuxStructuresPayload> getInstance() { return INSTANCE; }
    private boolean servuxRegistered;
    private boolean register;

    @Override
    public PayloadType getPayloadType() { return PayloadType.SERVUX_STRUCTURES; }

    public void setRegister(boolean toggle)
    {
        this.register = toggle;
    }

    @Override
    public void reset(PayloadType type)
    {
        if (type.equals(getPayloadType()) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
        }
        this.register = false;
    }

    @Override
    public <P extends CustomPayload> void receiveC2SPlayPayload(PayloadType type, P payload, ServerPlayNetworking.Context ctx)
    {
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;
        ServerPlayerEntity player = ctx.player();

        ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).decodeC2SNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data(), player);
    }

    @Override
    public <P extends CustomPayload> void receiveC2SPlayPayload(PayloadType type, P payload, ServerPlayNetworkHandler handler, CallbackInfo ci)
    {
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;
        ServerPlayerEntity player = handler.getPlayer();

        ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).decodeC2SNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data(), player);

        if (ci.isCancellable())
            ci.cancel();
    }

    @Override
    public void decodeC2SNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player)
    {
        if (Objects.equals(type, StructureDataProvider.INSTANCE.getNetworkChannel()))
        {
            int packetType = data.getInt("packetType");

            if (packetType == PacketType.Structures.PACKET_C2S_REQUEST_METADATA)
            {
                Servux.printDebug("ServuxStructuresHandler#decodeC2SNbtCompound(): received a REQUEST_METADATA packet from player: {}.", player.getName().getLiteralString());

                StructureDataProvider.INSTANCE.refreshMetadata(player, null);
            }
            else if (packetType == PacketType.Structures.PACKET_C2S_REQUEST_SPAWN_METADATA)
            {
                Servux.printDebug("ServuxStructuresHandler#decodeC2SNbtCompound(): received a REQUEST_SPAWN_METADATA packet from player: {}.", player.getName().getLiteralString());

                StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data);
            }
            else if (packetType == PacketType.Structures.PACKET_C2S_STRUCTURES_ACCEPT)
            {
                Servux.printDebug("ServuxStructuresHandler#decodeC2SNbtCompound(): received a STRUCTURES_ACCEPT packet from player: {}.", player.getName().getLiteralString());

                StructureDataProvider.INSTANCE.acceptStructuresFromPlayer(player, data);
            }
            else if (packetType == PacketType.Structures.PACKET_C2S_STRUCTURES_DECLINED)
            {
                Servux.printDebug("ServuxStructuresHandler#decodeC2SNbtCompound(): received a STRUCTURES_DECLINED packet from player: {}.", player.getName().getLiteralString());

                StructureDataProvider.INSTANCE.declineStructuresFromPlayer(player);
            }
            else
            {
                Servux.logger.warn("ServuxStructuresHandler#decodeC2SNbtCompound(): Invalid packetType from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
            }
        }
    }

    @Override
    public void encodeS2CNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player)
    {
        ServuxStructuresPayload payload = new ServuxStructuresPayload(data);

        ServuxStructuresHandler.INSTANCE.sendS2CPlayPayload(type, payload, player);
    }

    @Override
    public <P extends CustomPayload> void sendS2CPlayPayload(PayloadType type, P payload, ServerPlayerEntity player)
    {
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
        }
        else
        {
            Servux.logger.error("sendS2CPlayPayload(): [API].canSend() -> {} is FALSE type: {}", player.getName().getLiteralString(), type.toString());
        }
    }

    @Override
    public <P extends CustomPayload> void sendS2CPlayPayload(PayloadType type, P payload, ServerPlayNetworkHandler handler)
    {
        Packet<?> packet = new CustomPayloadS2CPacket(payload);

        if (handler != null && handler.accepts(packet))
        {
            ServerPlayerEntity player = handler.getPlayer();

            handler.sendPacket(packet);
        }
        else
        {
            Servux.logger.error("sendS2CPlayPayload(): [Handler].accepts() is FALSE type: {}", type.toString());
        }
    }

    @Override
    public void registerPlayPayload(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered() == false)
        {
            PayloadManager.getInstance().registerPlayChannel(type, CommonHandlerRegister.getInstance().getPayloadType(type), CommonHandlerRegister.getInstance().getPacketCodec(type));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            CommonHandlerRegister.getInstance().registerPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE, this);
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

            CommonHandlerRegister.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE);
        }
    }
}
