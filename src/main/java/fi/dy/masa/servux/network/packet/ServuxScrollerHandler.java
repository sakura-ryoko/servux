package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.ScrollerDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.SERVER)
public abstract class ServuxScrollerHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxScrollerHandler<ServuxScrollerPacket.Payload> INSTANCE = new ServuxScrollerHandler<>() {
        @Override
        public void receive(ServuxScrollerPacket.Payload payload, ServerPlayNetworking.Context context)
        {
            ServuxScrollerHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxScrollerHandler<ServuxScrollerPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "scroller");

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
        ServuxScrollerPacket packet = (ServuxScrollerPacket) data;

        if (!channel.equals(CHANNEL_ID))
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_C2S_METADATA_REQUEST -> ScrollerDataProvider.INSTANCE.sendMetadata(player);
            case PACKET_C2S_MASS_CRAFT_REQUEST-> ScrollerDataProvider.INSTANCE.requestMassCraft(player, packet.getCompound());
            case PACKET_C2S_RECIPE_MANAGER_REQUEST -> ScrollerDataProvider.INSTANCE.refreshRecipeManager(player, packet.getCompound());
            default -> Servux.logger.warn("ServuxScrollerHandler#decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().getLiteralString(), packet.getTotalSize());
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
            ServuxScrollerHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxScrollerPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(ServerPlayerEntity player, PacketByteBuf buffer, ServerPlayNetworkHandler networkHandler)
    {
        // Send each PacketSplitter buffer slice
        ServuxScrollerHandler.INSTANCE.sendPlayPayload(player, new ServuxScrollerPacket.Payload(ServuxScrollerPacket.ResponseS2CData(buffer)));
    }

    @Override
    public <P extends IServerPayloadData> void encodeServerData(ServerPlayerEntity player, P data)
    {
        ServuxScrollerPacket packet = (ServuxScrollerPacket) data;

        // Send Response Data via Packet Splitter
        if (packet.getType().equals(ServuxScrollerPacket.Type.PACKET_S2C_NBT_RESPONSE_START))
        {
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, player, player.networkHandler);
        }
        else if (!ServuxScrollerHandler.INSTANCE.sendPlayPayload(player, new ServuxScrollerPacket.Payload(packet)))
        {
            UUID id = player.getUuid();

            // Packet failure tracking
            if (!this.failures.containsKey(id))
            {
                this.failures.put(id, 1);
            }
            else if (this.failures.get(id) > MAX_FAILURES)
            {
                //Servux.logger.info("Unregistering Entities Client {} after {} failures (ItemScroller not installed perhaps)", player.getName().getLiteralString(), MAX_FAILURES);
                ScrollerDataProvider.INSTANCE.onPacketFailure(player);
            }
            else
            {
                int count = this.failures.get(id) + 1;
                this.failures.put(id, count);
            }
        }
    }
}
