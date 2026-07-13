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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;

@Environment(EnvType.SERVER)
public abstract class ServuxLitematicaHandler<T extends CustomPacketPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> INSTANCE = new ServuxLitematicaHandler<>()
    {
        @Override
        public void receive(ServuxLitematicaPacket.@NonNull Payload payload, ServerPlayNetworking.@NotNull Context context)
        {
            ServuxLitematicaHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "litematics");

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
        ServuxLitematicaPacket packet = (ServuxLitematicaPacket) data;

        if (!channel.equals(CHANNEL_ID))
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_C2S_METADATA_REQUEST -> LitematicsDataProvider.INSTANCE.registerPlayer(player);
            case PACKET_C2S_BLOCK_ENTITY_REQUEST -> LitematicsDataProvider.INSTANCE.onBlockEntityRequest(player, packet.getPos());
            case PACKET_C2S_ENTITY_REQUEST -> LitematicsDataProvider.INSTANCE.onEntityRequest(player, packet.getEntityId());
            case PACKET_C2S_BULK_ENTITY_NBT_REQUEST -> LitematicsDataProvider.INSTANCE.onBulkEntityRequest(player, packet.getChunkPos(), packet.getCompound());
            case PACKET_C2S_NBT_RESPONSE_DATA ->
            {
                if (!LitematicsDataProvider.INSTANCE.isPlayerRegistered(player)) { return; }
                UUID uuid = player.getUUID();
                long readingSessionKey;

                if (!this.readingSessionKeys.containsKey(uuid))
                {
                    readingSessionKey = RandomSource.create(Util.getMillis()).nextLong();
                    this.readingSessionKeys.put(uuid, readingSessionKey);
                }
                else
                {
                    readingSessionKey = this.readingSessionKeys.get(uuid);
                }

                if (Reference.DEV_DEBUG)
                {
                    Servux.LOGGER.info("ServuxLitematicaHandler#decodeServerData(): received Litematic Data Packet Slice of size {} (in bytes) // reading session key [{}]", packet.getTotalSize(), readingSessionKey);
                }
                FriendlyByteBuf fullPacket = PacketSplitter.receive(this, readingSessionKey, packet.getBuffer());

                if (fullPacket != null)
                {
                    if (Reference.DEV_DEBUG)
                    {
                        Servux.LOGGER.info("ServuxLitematicaHandler#decodeServerData(): received Litematic Data Full Packet of size {} (in bytes) // reading session key [{}]", fullPacket.readableBytes(), readingSessionKey);
                    }

                    try
                    {
                        this.readingSessionKeys.remove(uuid);
                        this.handleBulkData(player, fullPacket.readVarInt(), (CompoundTag) fullPacket.readNbt(NbtAccounter.unlimitedHeap()));
                    }
                    catch (Exception e)
                    {
                        Servux.LOGGER.error("ServuxLitematicaHandler#decodeServerData(): Litematic Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }
                }
            }
            default -> Servux.LOGGER.warn("ServuxLitematicaHandler#decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().tryCollapseToString(), packet.getTotalSize());
        }
    }

    private void handleBulkData(ServerPlayer player, final int type, CompoundTag nbt)
    {
        String task = nbt.getStringOr("Task", "LitematicaPaste");
        Servux.debugLog("handleBulkData: received task: {} from {}", task, player.getName().getString());

        // For future Granular Task Management
//        switch (task)
//        {
//            // File-Transmit support
//            case "Litematic-TransmitStart", "Litematic-TransmitCancel", "Litematic-TransmitData", "Litematic-TransmitEnd" ->
//            {
//                Pair<LitematicaSchematic, CompoundTag> schemPair = LitematicaSchematic.receiveFileTransmit(nbt, player);
//
//                if (schemPair != null && schemPair.getLeft().getFile() != null)
//                {
//                    Servux.debugLog("handleBulkData(): Received litematic '{}' from player {}", schemPair.getLeft().getFile().toAbsolutePath().toString(), player.getName().tryCollapseToString());
//                    LitematicsDataProvider.INSTANCE.handleClientPasteRequestPair(player, type, schemPair);
//                }
//            }
//            default -> LitematicsDataProvider.INSTANCE.handleClientPasteRequest(player, type, nbt);
//        }

        LitematicsDataProvider.INSTANCE.handleClientPasteRequest(player, type, nbt);
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
            ServuxLitematicaHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxLitematicaPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(ServerPlayer player, FriendlyByteBuf buffer, ServerGamePacketListenerImpl networkHandler)
    {
        // Send each PacketSplitter buffer slice
        ServuxLitematicaHandler.INSTANCE.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.ResponseS2CData(buffer)));
    }

    @Override
    public <P extends IServerPayloadData> void encodeServerData(ServerPlayer player, P data)
    {
        if (!LitematicsDataProvider.INSTANCE.isEnabled()) return;

        ServuxLitematicaPacket packet = (ServuxLitematicaPacket) data;

        // Send Response Data via Packet Splitter
        if (packet.getType().equals(ServuxLitematicaPacket.Type.PACKET_S2C_NBT_RESPONSE_START))
        {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(packet.getTransactionId());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, player, player.connection);
        }
        else if (!ServuxLitematicaHandler.INSTANCE.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(packet)))
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
                    Servux.LOGGER.info("Unregistering Litematic Client {} after {} failures (Litematica not installed perhaps)", player.getName().tryCollapseToString(), MAX_FAILURES);
                }

                LitematicsDataProvider.INSTANCE.onPacketFailure(player);
            }
            else
            {
                int count = this.failures.get(id) + 1;
                this.failures.put(id, count);
            }
        }
    }
}
