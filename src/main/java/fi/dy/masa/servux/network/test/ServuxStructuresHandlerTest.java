package fi.dy.masa.servux.network.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.ApiStatus;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.server.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.server.PayloadSplitter;

@ApiStatus.Experimental
public abstract class ServuxStructuresHandlerTest<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxStructuresHandlerTest<ServuxStructuresPayloadTest> INSTANCE = new ServuxStructuresHandlerTest<>() {
        @Override
        public void receive(ServuxStructuresPayloadTest payload, ServerPlayNetworking.Context context)
        {
            ServuxStructuresHandlerTest.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxStructuresHandlerTest<ServuxStructuresPayloadTest> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "structures-test");

    private boolean payloadRegistered = false;
    private final Map<UUID, Integer> failures = new HashMap<>();
    private static final int MAX_FAILURES = 3;
    // Tracks PacketSend failures until MAX_FAILURES

    public final int PROTOCOL_VERSION = 2;
    public static final int PACKET_S2C_METADATA = 1;
    public static final int PACKET_S2C_STRUCTURE_DATA = 2;
    public static final int PACKET_C2S_STRUCTURES_REGISTER = 3;
    public static final int PACKET_C2S_STRUCTURES_UNREGISTER = 4;
    public static final int PACKET_S2C_STRUCTURE_DATA_START = 5;
    public static final int PACKET_S2C_STRUCTURE_DATA_END = 6;
    public static final int PACKET_S2C_SPAWN_METADATA = 10;
    public static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;

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

    public void decodeStructuresData(Identifier channel, ServerPlayerEntity player, ServuxStructuresDataTest data)
    {
        if (channel.equals(CHANNEL_ID) == false)
        {
            return;
        }
        Servux.logger.error("decodeStructuresData(): received packet from {}, of packetType {} // size in bytes [{}]", player.getName().getLiteralString(), data.getPacketType(), data.getTotalSize());

        switch (data.getPacketType())
        {
            case PACKET_C2S_STRUCTURES_REGISTER ->
            {
                Servux.logger.warn("decodeStructuresData(): received Structures Register from player {}", player.getName().getLiteralString());
                //StructureDataProvider.INSTANCE.unregister(player);
                //StructureDataProvider.INSTANCE.register(player);
            }
            case PACKET_C2S_REQUEST_SPAWN_METADATA ->
            {
                Servux.logger.warn("decodeStructuresData(): received Spawn Metadata Request from player {}", player.getName().getLiteralString());
                //StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data.getCompound());
            }
            case PACKET_C2S_STRUCTURES_UNREGISTER ->
            {
                Servux.logger.warn("decodeStructuresData(): received Structures Un-Register from player {}", player.getName().getLiteralString());
                //StructureDataProvider.INSTANCE.unregister(player);
                //StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data.getCompound());
            }
            default ->
            {
                Servux.logger.warn("decodeStructuresData(): Invalid packetType from player: {}, of size in bytes: {}.", player.getName().getLiteralString(), data.getTotalSize());
            }
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
            ServuxStructuresHandlerTest.INSTANCE.decodeStructuresData(CHANNEL_ID, player, ((ServuxStructuresPayloadTest) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(ServerPlayerEntity player, PacketByteBuf packet)
    {
        Servux.logger.warn("encodeWithSplitter(): packet yeet");
        ServuxStructuresHandlerTest.INSTANCE.encodeStructuresData(player, new ServuxStructuresDataTest(PACKET_S2C_STRUCTURE_DATA, packet));
    }

    public void encodeStructuresData(ServerPlayerEntity player, ServuxStructuresDataTest data)
    {
        if (data.getPacketType() == PACKET_S2C_STRUCTURE_DATA_START)
        {
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
            buffer.writeNbt(data.getCompound());

            if (PayloadSplitter.send(this, buffer, player))
            {
                Servux.logger.warn("encodeStructuresData(): splitter Finished");
                ServuxStructuresHandlerTest.INSTANCE.encodeStructuresData(player, new ServuxStructuresDataTest(PACKET_S2C_STRUCTURE_DATA_END, new NbtCompound()));
            }
        }
        else if (ServuxStructuresHandlerTest.INSTANCE.sendPlayPayload(player, new ServuxStructuresPayloadTest(data)) == false)
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
