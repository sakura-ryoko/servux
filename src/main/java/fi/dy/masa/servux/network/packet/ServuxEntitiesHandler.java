package fi.dy.masa.servux.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;

@Environment(EnvType.SERVER)
public abstract class ServuxEntitiesHandler<T extends CustomPacketPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> INSTANCE = new ServuxEntitiesHandler<>()
    {
        @Override
        public void receive(ServuxEntitiesPacket.@NonNull Payload payload, ServerPlayNetworking.@NotNull Context context)
        {
            ServuxEntitiesHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "entity_data");

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
    public <P extends IServerPayloadData> void decodeServerData(Identifier channel, ServerPlayer player, P data)
    {
        ServuxEntitiesPacket packet = (ServuxEntitiesPacket) data;

        if (!channel.equals(CHANNEL_ID))
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_C2S_METADATA_REQUEST -> EntitiesDataProvider.INSTANCE.register(player);
            case PACKET_C2S_BLOCK_ENTITY_REQUEST -> EntitiesDataProvider.INSTANCE.onBlockEntityRequest(player, packet.getPos());
            case PACKET_C2S_ENTITY_REQUEST -> EntitiesDataProvider.INSTANCE.onEntityRequest(player, packet.getEntityId());
            /*
            case PACKET_C2S_NBT_RESPONSE_DATA ->
            {
                UUID uuid = player.getUuid();
                long readingSessionKey;

                if (!this.readingSessionKeys.containsKey(uuid))
                {
                    readingSessionKey = Random.create(Util.getMeasuringTimeMs()).nextLong();
                    this.readingSessionKeys.put(uuid, readingSessionKey);
                }
                else
                {
                    readingSessionKey = this.readingSessionKeys.get(uuid);
                }

                if (Reference.DEV_DEBUG)
                {
                    Servux.logger.info("ServuxEntitiesHandler#decodeServerData(): received Entity Data Packet Slice of size {} (in bytes) // reading session key [{}]", packet.getTotalSize(), readingSessionKey);
                }
                PacketByteBuf fullPacket = PacketSplitter.receive(this, readingSessionKey, packet.getBuffer());

                if (fullPacket != null)
                {
                    try
                    {
                        this.readingSessionKeys.remove(uuid);
                        EntitiesDataProvider.INSTANCE.handleBulkClientRequest(player, fullPacket.readVarInt(), (NbtCompound) fullPacket.readNbt(NbtSizeTracker.ofUnlimitedBytes()));
                    }
                    catch (Exception e)
                    {
                        Servux.logger.error("ServuxEntitiesHandler#decodeServerData(): Entity Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }
                }
            }
             */
            default -> Servux.LOGGER.warn("ServuxEntitiesHandler#decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().tryCollapseToString(), packet.getTotalSize());
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

    public void resetFailures(Identifier channel, ServerPlayer player)
    {
        if (channel.equals(CHANNEL_ID))
        {
            this.failures.remove(player.getUUID());
        }
    }

    @Override
    public void receivePlayPayload(T payload, ServerPlayNetworking.Context ctx)
    {
        if (payload.type().id().equals(CHANNEL_ID))
        {
            ServerPlayer player = ctx.player();
            ServuxEntitiesHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxEntitiesPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(ServerPlayer player, FriendlyByteBuf buffer, ServerGamePacketListenerImpl networkHandler)
    {
        // Send each PacketSplitter buffer slice
        ServuxEntitiesHandler.INSTANCE.sendPlayPayload(player, new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.ResponseS2CData(buffer)));
    }

    @Override
    public <P extends IServerPayloadData> void encodeServerData(ServerPlayer player, P data)
    {
        if (!EntitiesDataProvider.INSTANCE.isEnabled()) return;

        ServuxEntitiesPacket packet = (ServuxEntitiesPacket) data;

        // Send Response Data via Packet Splitter
        if (packet.getType().equals(ServuxEntitiesPacket.Type.PACKET_S2C_NBT_RESPONSE_START))
        {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(packet.getTransactionId());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, player, player.connection);
        }
        else if (!ServuxEntitiesHandler.INSTANCE.sendPlayPayload(player, new ServuxEntitiesPacket.Payload(packet)))
        {
            UUID id = player.getUUID();

            // Packet failure tracking
            if (!this.failures.containsKey(id))
            {
                this.failures.put(id, 1);
            }
            else if (this.failures.get(id) > MAX_FAILURES)
            {
                if (Reference.DEV_DEBUG)
                {
                    Servux.LOGGER.info("Unregistering Entities Client {} after {} failures (Mod not installed perhaps)", player.getName().tryCollapseToString(), MAX_FAILURES);
                }
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
