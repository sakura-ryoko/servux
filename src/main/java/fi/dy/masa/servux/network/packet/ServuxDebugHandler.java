package fi.dy.masa.servux.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.buffer.Unpooled;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.DebugDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;

@Environment(EnvType.SERVER)
public abstract class ServuxDebugHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxDebugHandler<ServuxDebugPacket.Payload> INSTANCE = new ServuxDebugHandler<>() {
        @Override
        public void receive(ServuxDebugPacket.Payload payload, ServerPlayNetworking.Context context)
        {
            ServuxDebugHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxDebugHandler<ServuxDebugPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "debug_service");

    private boolean payloadRegistered = false;
    private final Map<UUID, Integer> failures = new HashMap<>();
    private static final int MAX_FAILURES = 4;
    private final Map<UUID, Long> readingSessionKeys = new HashMap<>();

    @Override
    public Identifier getPayloadChannel() { return CHANNEL_ID; }

    @Override
    public boolean isPlayRegistered(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            return payloadRegistered;
        }

        return false;
    }

    @Override
    public void setPlayRegistered(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            this.payloadRegistered = true;
        }
    }

    @Override
    public <P extends IServerPayloadData> void decodeServerData(Identifier channel, ServerPlayerEntity player, P data)
    {
        ServuxDebugPacket packet = (ServuxDebugPacket) data;

        if (!channel.equals(CHANNEL_ID))
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_C2S_METADATA_REQUEST -> DebugDataProvider.INSTANCE.sendMetadata(player);
            case PACKET_C2S_METADATA_CONFIRM -> DebugDataProvider.INSTANCE.confirmMetadata(player, packet.getCompound());
            case PACKET_C2S_DEBUG_SERVICE_REGISTER ->
            {
                Servux.debugLog("decodeServerData(): received Debug Service Register from player {}", player.getName().getLiteralString());
                DebugDataProvider.INSTANCE.unregister(player, packet.getCompound());
                DebugDataProvider.INSTANCE.register(player, packet.getCompound());
            }
            case PACKET_C2S_DEBUG_SERVICE_UNREGISTER ->
            {
                Servux.debugLog("decodeServerData(): received Debug Service Un-Register from player {}", player.getName().getLiteralString());
                DebugDataProvider.INSTANCE.unregister(player, packet.getCompound());
            }
            default -> Servux.logger.warn("ServuxDebugHandler#decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().getLiteralString(), packet.getTotalSize());
        }
    }

    @Override
    public void reset(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            this.failures.clear();
        }
    }

    public void resetFailures(Identifier channel, ServerPlayerEntity player)
    {
        if (channel.equals(CHANNEL_ID))
        {
            this.failures.remove(player.getUuid());
        }
    }

    @Override
    public void receivePlayPayload(T payload, ServerPlayNetworking.Context ctx)
    {
        if (payload.getId().id().equals(CHANNEL_ID))
        {
            ServerPlayerEntity player = ctx.player();
            ServuxDebugHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxDebugPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(ServerPlayerEntity player, PacketByteBuf buffer, ServerPlayNetworkHandler networkHandler)
    {
        // Send each PacketSplitter buffer slice
        ServuxDebugHandler.INSTANCE.sendPlayPayload(player, new ServuxDebugPacket.Payload(ServuxDebugPacket.ResponseS2CData(buffer)));
    }

    @Override
    public <P extends IServerPayloadData> void encodeServerData(ServerPlayerEntity player, P data)
    {
        ServuxDebugPacket packet = (ServuxDebugPacket) data;

        // Send Response Data via Packet Splitter
        if (packet.getType().equals(ServuxDebugPacket.Type.PACKET_S2C_NBT_RESPONSE_START))
        {
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, player, player.networkHandler);
        }
        else if (!ServuxDebugHandler.INSTANCE.sendPlayPayload(player, new ServuxDebugPacket.Payload(packet)))
        {
            UUID id = player.getUuid();

            // Packet failure tracking
            if (!this.failures.containsKey(id))
            {
                this.failures.put(id, 1);
            }
            else if (this.failures.get(id) > MAX_FAILURES)
            {
                //Servux.logger.info("Unregistering Entities Client {} after {} failures (MiniHUD not installed perhaps)", player.getName().getLiteralString(), MAX_FAILURES);
                DebugDataProvider.INSTANCE.onPacketFailure(player);
            }
            else
            {
                int count = this.failures.get(id) + 1;
                this.failures.put(id, count);
            }
        }
    }
}
