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
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.PacketSplitter;

@Environment(EnvType.SERVER)
public abstract class ServuxStructuresHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxStructuresHandler<ServuxStructuresPacket.Payload> INSTANCE = new ServuxStructuresHandler<>() {
        @Override
        public void receive(ServuxStructuresPacket.Payload payload, ServerPlayNetworking.Context context)
        {
            ServuxStructuresHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxStructuresHandler<ServuxStructuresPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "structures");

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

    public void decodeStructuresPacket(Identifier channel, ServerPlayerEntity player, ServuxStructuresPacket packet)
    {
        if (!channel.equals(CHANNEL_ID))
        {
            return;
        }
        //Servux.logger.error("decodeStructuresPacket(): received packet from {}, of packetType {} // size in bytes [{}]", player.getName().getLiteralString(), packet.getPacketType(), packet.getTotalSize());

        switch (packet.getType())
        {
            // Only NBT type packets are received from MiniHUD, not using PacketSplitter
            case PACKET_C2S_STRUCTURES_REGISTER ->
            {
                //Servux.logger.warn("decodeStructuresPacket(): received Structures Register from player {}", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.unregister(player);
                StructureDataProvider.INSTANCE.register(player);
            }
            case PACKET_C2S_REQUEST_SPAWN_METADATA -> StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, packet.getCompound());
            case PACKET_C2S_STRUCTURES_UNREGISTER ->
            {
                //Servux.logger.warn("decodeStructuresPacket(): received Structures Un-Register from player {}", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.unregister(player);
                StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, packet.getCompound());
            }
            default -> Servux.logger.warn("decodeStructuresPacket(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().getLiteralString(), packet.getTotalSize());
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
            ServuxStructuresHandler.INSTANCE.decodeStructuresPacket(CHANNEL_ID, player, ((ServuxStructuresPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(ServerPlayerEntity player, PacketByteBuf buffer, ServerPlayNetworkHandler networkHandler)
    {
        // Send each PacketSplitter buffer slice
        ServuxStructuresHandler.INSTANCE.encodeStructuresPacket(player, new ServuxStructuresPacket(ServuxStructuresPacket.Type.PACKET_S2C_STRUCTURE_DATA, buffer));
    }

    public void encodeStructuresPacket(ServerPlayerEntity player, ServuxStructuresPacket packet)
    {
        if (packet.getType().equals(ServuxStructuresPacket.Type.PACKET_S2C_STRUCTURE_DATA_START))
        {
            // Send Structure Data via Packet Splitter
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, player, player.networkHandler);
        }
        else if (!ServuxStructuresHandler.INSTANCE.sendPlayPayload(player, new ServuxStructuresPacket.Payload(packet)))
        {
            // Packet failure tracking
            UUID id = player.getUuid();

            if (!this.failures.containsKey(id))
            {
                this.failures.put(id, 1);
            }
            else if (this.failures.get(id) > MAX_FAILURES)
            {
                //Servux.logger.info("Unregistering Structure Client {} after {} failures (MiniHUD not installed perhaps)", player.getName().getLiteralString(), MAX_FAILURES);
                StructureDataProvider.INSTANCE.unregister(player);
            }
            else
            {
                int count = this.failures.get(id) + 1;
                this.failures.put(id, count);
            }
        }
    }
}
