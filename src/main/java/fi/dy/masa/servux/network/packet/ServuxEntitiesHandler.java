package fi.dy.masa.servux.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;

@Environment(EnvType.SERVER)
public abstract class ServuxEntitiesHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> INSTANCE = new ServuxEntitiesHandler<>() {
        @Override
        public void receive(ServuxEntitiesPacket.Payload payload, ServerPlayNetworking.Context context)
        {
            ServuxEntitiesHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "entity_data");

    private boolean payloadRegistered = false;
    private final Map<UUID, Integer> failures = new HashMap<>();
    private static final int MAX_FAILURES = 4;

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
        ServuxEntitiesPacket packet = (ServuxEntitiesPacket) data;

        if (channel.equals(CHANNEL_ID) == false)
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_C2S_METADATA_REQUEST ->
            {
                Servux.logger.warn("ServuxEntitiesHandler#decodeServerData(): received Metadata Request from player {}", player.getName().getLiteralString());
                EntitiesDataProvider.INSTANCE.sendMetadata(player);
            }
            case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
            {
                Servux.logger.warn("ServuxEntitiesHandler#decodeServerData(): received Block Entity Request from player {}", player.getName().getLiteralString());
                EntitiesDataProvider.INSTANCE.onBlockEntityRequest(player, packet.getPos());
            }
            case PACKET_C2S_ENTITY_REQUEST ->
            {
                Servux.logger.warn("ServuxEntitiesHandler#decodeServerData(): received Entity Request from player {}", player.getName().getLiteralString());
                EntitiesDataProvider.INSTANCE.onEntityRequest(player, packet.getEntityId());
            }
            default -> Servux.logger.warn("decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().getLiteralString(), packet.getTotalSize());
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
            ServuxEntitiesHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxEntitiesPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(ServerPlayerEntity player, PacketByteBuf buffer, ServerPlayNetworkHandler networkHandler)
    {
        // Send each PacketSplitter buffer slice
        ServuxEntitiesHandler.INSTANCE.sendPlayPayload(player, new ServuxEntitiesPacket.Payload(new ServuxEntitiesPacket(buffer)));
    }

    @Override
    public <P extends IServerPayloadData> void encodeServerData(ServerPlayerEntity player, P data)
    {
        ServuxEntitiesPacket packet = (ServuxEntitiesPacket) data;

        // Send Response Data via Packet Splitter
        if (packet.getType().equals(ServuxEntitiesPacket.Type.PACKET_S2C_NBT_RESPONSE_START))
        {
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
            buffer.writeVarInt(packet.getTransactionId());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, player, player.networkHandler);
        }
        else if (ServuxEntitiesHandler.INSTANCE.sendPlayPayload(player, new ServuxEntitiesPacket.Payload(packet)) == false)
        {
            UUID id = player.getUuid();

            // Packet failure tracking
            if (this.failures.containsKey(id) == false)
            {
                this.failures.put(id, 1);
            }
            else if (this.failures.get(id) > MAX_FAILURES)
            {
                Servux.logger.info("Unregistering Entities Client {} after {} failures (Mod not installed perhaps)", player.getName().getLiteralString(), MAX_FAILURES);
                EntitiesDataProvider.INSTANCE.onPacketFailure(player);
            }
            else
            {
                int count = this.failures.get(id) + 1;
                this.failures.put(id, count);
            }
        }
    }
}
