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
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;

@Environment(EnvType.SERVER)
public abstract class ServuxBlockEntitiesHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxBlockEntitiesHandler<ServuxBlockEntitiesPacket.Payload> INSTANCE = new ServuxBlockEntitiesHandler<>() {
        @Override
        public void receive(ServuxBlockEntitiesPacket.Payload payload, ServerPlayNetworking.Context context)
        {
            ServuxBlockEntitiesHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxBlockEntitiesHandler<ServuxBlockEntitiesPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "block_entities");

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
        ServuxBlockEntitiesPacket packet = (ServuxBlockEntitiesPacket) data;

        if (channel.equals(CHANNEL_ID) == false)
        {
            return;
        }
        Servux.logger.error("ServuxBlockEntitiesHandler#decodeServerData(): received packet from {}, of packetType {} // size in bytes [{}]", player.getName().getLiteralString(), packet.getPacketType(), packet.getTotalSize());

        switch (packet.getType())
        {
            // Only NBT type packets are received from MiniHUD, not using PacketSplitter
            case PACKET_C2S_BLOCK_ENTITY_REGISTER ->
            {
                Servux.logger.warn("ServuxBlockEntitiesHandler#decodeServerData(): received Block Entities Register from player {}", player.getName().getLiteralString());
                //StructureDataProvider.INSTANCE.unregister(player);
                //StructureDataProvider.INSTANCE.register(player);
            }
            case PACKET_C2S_BLOCK_ENTITY_UNREGISTER ->
            {
                Servux.logger.warn("ServuxBlockEntitiesHandler#decodeServerData(): received Block Entities Un-Register from player {}", player.getName().getLiteralString());
                //StructureDataProvider.INSTANCE.unregister(player);
                //StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, packet.getCompound());
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
            ServuxBlockEntitiesHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxBlockEntitiesPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(ServerPlayerEntity player, PacketByteBuf buffer, ServerPlayNetworkHandler networkHandler)
    {
        // Send each PacketSplitter buffer slice
        ServuxBlockEntitiesHandler.INSTANCE.encodeServerData(player, new ServuxBlockEntitiesPacket(ServuxBlockEntitiesPacket.Type.PACKET_S2C_BLOCK_ENTITY_DATA, buffer));
    }

    @Override
    public <P extends IServerPayloadData> void encodeServerData(ServerPlayerEntity player, P data)
    {
        ServuxBlockEntitiesPacket packet = (ServuxBlockEntitiesPacket) data;

        if (packet.getType().equals(ServuxBlockEntitiesPacket.Type.PACKET_S2C_BLOCK_ENTITY_DATA_START))
        {
            // Send Structure Data via Packet Splitter
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, player, player.networkHandler);
        }
        else if (ServuxBlockEntitiesHandler.INSTANCE.sendPlayPayload(player, new ServuxBlockEntitiesPacket.Payload(packet)) == false)
        {
            // Packet failure tracking
            UUID id = player.getUuid();

            if (this.failures.containsKey(id) == false)
            {
                this.failures.put(id, 1);
            }
            else if (this.failures.get(id) > MAX_FAILURES)
            {
                Servux.logger.info("Unregistering Block Entities Client {} after {} failures (Mod not installed perhaps)", player.getName().getLiteralString(), MAX_FAILURES);
                //StructureDataProvider.INSTANCE.unregister(player);
            }
            else
            {
                int count = this.failures.get(id) + 1;
                this.failures.put(id, count);
            }
        }
    }
}
