package fi.dy.masa.servux.network.handler;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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
    private boolean servuxRegistered;
    private boolean payloadRegistered = false;

    @Override
    public Identifier getPayloadChannel() { return CHANNEL_ID; }

    @Override
    public boolean isPlayRegistered(Identifier channel)
    {
        if (channel.equals(getPayloadChannel()))
        {
            return payloadRegistered;
        }

        return false;
    }

    @Override
    public void setPlayRegistered(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()))
        {
            this.payloadRegistered = true;
        }
    }

    @Override
    public void decodeNbtCompound(Identifier channel, ServerPlayerEntity player, NbtCompound data)
    {
        int packetType = data.getInt("packetType");

        if (packetType == PACKET_C2S_STRUCTURES_REGISTER)
        {
            StructureDataProvider.INSTANCE.unregister(player);
            StructureDataProvider.INSTANCE.register(player);
        }
        else if (packetType == PACKET_C2S_REQUEST_SPAWN_METADATA)
        {
            StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data);
        }
        else if (packetType == PACKET_C2S_STRUCTURES_UNREGISTER)
        {
            StructureDataProvider.INSTANCE.unregister(player);
            StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data);
        }
        else
        {
            Servux.logger.warn("ServuxStructuresHandler#decodeC2SNbtCompound(): Invalid packetType from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
        }
    }

    @Override
    public void reset(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
        }
    }

    @Override
    public void registerPlayPayload(Identifier channel)
    {
        if (this.servuxRegistered == false && this.payloadRegistered == false &&
        ServerPlayHandler.getInstance().isServerPlayChannelRegistered(this) == false)
        {
            //Servux.logger.info("registerPlayPayload() registering for {}", channel.toString());

            PayloadTypeRegistry.playS2C().register(ServuxStructuresPayload.TYPE, ServuxStructuresPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(ServuxStructuresPayload.TYPE, ServuxStructuresPayload.CODEC);
        }

        this.payloadRegistered = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()) && this.payloadRegistered)
        {
            //Servux.logger.info("registerPlayHandler() called for {}", channel.toString());

            ServerPlayNetworking.registerGlobalReceiver((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE, this);
            this.servuxRegistered = true;
        }
    }

    @Override
    public void unregisterPlayHandler(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()) && this.payloadRegistered)
        {
            //Servux.logger.info("unregisterPlayHandler() called for {}", channel.toString());

            this.servuxRegistered = false;
            ServerPlayNetworking.unregisterGlobalReceiver(ServuxStructuresPayload.TYPE.id());
        }
    }

    @Override
    public <P extends CustomPayload> void receivePlayPayload(P payload, ServerPlayNetworking.Context ctx)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()))
        {
            ServerPlayerEntity player = ctx.player();

            ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).decodeNbtCompound(CHANNEL_ID, player, ((ServuxStructuresPayload) payload).data());
        }
    }

    @Override
    public void encodeNbtCompound(ServerPlayerEntity player, NbtCompound data)
    {
        ServuxStructuresHandler.INSTANCE.sendPlayPayload(player, new ServuxStructuresPayload(data));
    }
}
