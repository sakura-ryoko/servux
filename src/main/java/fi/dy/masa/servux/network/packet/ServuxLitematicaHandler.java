package fi.dy.masa.servux.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.tuple.Pair;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;
import fi.dy.masa.servux.schematic.LitematicaSchematic;

@Environment(EnvType.SERVER)
public abstract class ServuxLitematicaHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> INSTANCE = new ServuxLitematicaHandler<>() {
        @Override
        public void receive(ServuxLitematicaPacket.Payload payload, ServerPlayNetworking.Context context)
        {
            ServuxLitematicaHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "litematics");

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
                    readingSessionKey = Random.create(Util.getMeasuringTimeMs()).nextLong();
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
                PacketByteBuf fullPacket = PacketSplitter.receive(this, readingSessionKey, packet.getBuffer());

                if (fullPacket != null)
                {
                    if (Reference.DEV_DEBUG)
                    {
                        Servux.LOGGER.info("ServuxLitematicaHandler#decodeServerData(): received Litematic Data Full Packet of size {} (in bytes) // reading session key [{}]", fullPacket.readableBytes(), readingSessionKey);
                    }

                    try
                    {
                        this.readingSessionKeys.remove(uuid);
                        this.handleBulkData(player, fullPacket.readVarInt(), (NbtCompound) fullPacket.readNbt(NbtSizeTracker.ofUnlimitedBytes()));
                    }
                    catch (Exception e)
                    {
                        Servux.LOGGER.error("ServuxLitematicaHandler#decodeServerData(): Litematic Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }
                }
            }
            default -> Servux.LOGGER.warn("ServuxLitematicaHandler#decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().getLiteralString(), packet.getTotalSize());
        }
    }

    private void handleBulkData(ServerPlayerEntity player, final int type, NbtCompound nbt)
    {
        String task = nbt.getString("Task", "LitematicaPaste");
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
            ServuxLitematicaHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxLitematicaPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(ServerPlayerEntity player, PacketByteBuf buffer, ServerPlayNetworkHandler networkHandler)
    {
        // Send each PacketSplitter buffer slice
        ServuxLitematicaHandler.INSTANCE.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.ResponseS2CData(buffer)));
    }

    @Override
    public <P extends IServerPayloadData> void encodeServerData(ServerPlayerEntity player, P data)
    {
        if (!LitematicsDataProvider.INSTANCE.isEnabled()) return;

        ServuxLitematicaPacket packet = (ServuxLitematicaPacket) data;

        // Send Response Data via Packet Splitter
        if (packet.getType().equals(ServuxLitematicaPacket.Type.PACKET_S2C_NBT_RESPONSE_START))
        {
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
            buffer.writeVarInt(packet.getTransactionId());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, player, player.networkHandler);
        }
        else if (!ServuxLitematicaHandler.INSTANCE.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(packet)))
        {
            UUID id = player.getUuid();

            // Packet failure tracking
            if (!this.failures.containsKey(id))
            {
                this.failures.put(id, 1);
            }
            else if (this.failures.get(id) > MAX_FAILURES)
            {
                if (Reference.DEV_DEBUG)
                {
                    Servux.LOGGER.info("Unregistering Litematic Client {} after {} failures (Litematica not installed perhaps)", player.getName().getLiteralString(), MAX_FAILURES);
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
