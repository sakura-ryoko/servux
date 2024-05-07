package fi.dy.masa.servux.network.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.server.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.server.ServerPlayHandler;

public abstract class ServuxStructuresHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxStructuresHandler<ServuxStructuresPayload> INSTANCE = new ServuxStructuresHandler<>() {
        @Override
        public void receive(ServuxStructuresPayload payload, ServerPlayNetworking.Context context)
        {
            ServuxStructuresHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxStructuresHandler<ServuxStructuresPayload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = new Identifier("servux", "structures");
    public static final int PROTOCOL_VERSION = 2;
    public static final int PACKET_S2C_METADATA = 1;
    public static final int PACKET_S2C_STRUCTURE_DATA = 2;
    public static final int PACKET_C2S_STRUCTURES_REGISTER = 3;
    public static final int PACKET_C2S_STRUCTURES_UNREGISTER = 4;
    public static final int PACKET_S2C_SPAWN_METADATA = 10;
    public static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;

    private boolean payloadRegistered = false;
    private final Map<UUID, Integer> failures = new HashMap<>();
    private static final int MAX_FAILURES = 3;
    // Tracks PacketSend failures until MAX_FAILURES

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
    public void decodeNbtCompound(Identifier channel, ServerPlayerEntity player, NbtCompound data)
    {
        switch (data.getInt("packetType"))
        {
            case PACKET_C2S_STRUCTURES_REGISTER ->
            {
                StructureDataProvider.INSTANCE.unregister(player);
                StructureDataProvider.INSTANCE.register(player);
            }
            case PACKET_C2S_REQUEST_SPAWN_METADATA -> StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data);
            case PACKET_C2S_STRUCTURES_UNREGISTER ->
            {
                StructureDataProvider.INSTANCE.unregister(player);
                StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data);
            }
            default -> Servux.logger.warn("ServuxStructuresHandler#decodeC2SNbtCompound(): Invalid packetType from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
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

            ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).decodeNbtCompound(CHANNEL_ID, player, ((ServuxStructuresPayload) payload).data());
        }
    }

    @Override
    public void encodeNbtCompound(ServerPlayerEntity player, NbtCompound data)
    {
        if (ServuxStructuresHandler.INSTANCE.sendPlayPayload(player, new ServuxStructuresPayload(data)) == false)
        {
            // Packet failure tracking
            UUID id = player.getUuid();

            if (this.failures.containsKey(id) == false)
            {
                this.failures.put(id, 1);
            }
            else if (this.failures.get(id) > MAX_FAILURES)
            {
                Servux.logger.info("Unregistering Structure Client {} after {} failures (MiniHUD not installed perhaps)", player.getName().getLiteralString(), MAX_FAILURES);
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
