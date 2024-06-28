package fi.dy.masa.servux.dataproviders;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxEntitiesHandler;
import fi.dy.masa.servux.network.packet.ServuxEntitiesPacket;
import fi.dy.masa.servux.network.packet.ServuxLitematicaHandler;
import fi.dy.masa.servux.network.packet.ServuxLitematicaPacket;

public class EntitiesDataProvider extends DataProviderBase
{
    public static final EntitiesDataProvider INSTANCE = new EntitiesDataProvider();
    protected final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();
    protected final static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> LITEMATICA_HANDLER = ServuxLitematicaHandler.getInstance();
    protected final NbtCompound metadata = new NbtCompound();

    protected EntitiesDataProvider()
    {
        super("entity_data",
                ServuxEntitiesHandler.CHANNEL_ID,
                ServuxEntitiesPacket.PROTOCOL_VERSION,
                "Entity Data provider for Client Side mods.");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);
    }

    @Override
    public void registerHandler()
    {
        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxEntitiesPacket.Payload.ID, ServuxEntitiesPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
        HANDLER.registerPlayReceiver(ServuxEntitiesPacket.Payload.ID, HANDLER::receivePlayPayload);

        ServerPlayHandler.getInstance().registerServerPlayHandler(LITEMATICA_HANDLER);
        LITEMATICA_HANDLER.registerPlayPayload(ServuxLitematicaPacket.Payload.ID, ServuxLitematicaPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
        LITEMATICA_HANDLER.registerPlayReceiver(ServuxLitematicaPacket.Payload.ID, LITEMATICA_HANDLER::receivePlayPayload);
    }

    @Override
    public void unregisterHandler()
    {
        HANDLER.unregisterPlayReceiver();
        ServerPlayHandler.getInstance().unregisterServerPlayHandler(HANDLER);

        LITEMATICA_HANDLER.unregisterPlayReceiver();
        ServerPlayHandler.getInstance().unregisterServerPlayHandler(LITEMATICA_HANDLER);
    }

    @Override
    public IPluginServerPlayHandler<?> getPacketHandler()
    {
        return HANDLER;
    }

    public IPluginServerPlayHandler<?> getPacketHandler_Litematica()
    {
        return LITEMATICA_HANDLER;
    }

    public void sendMetadata(ServerPlayerEntity player)
    {
        Servux.logger.warn("entityDataChannel: sendMetadata to player {}", player.getName().getLiteralString());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.networkHandler != null)
        {
            HANDLER.sendPlayPayload(player.networkHandler, new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.MetadataResponse(this.metadata)));
        }
    }

    public void sendMetadata_Litematica(ServerPlayerEntity player)
    {
        Servux.logger.warn("sendMetadata_Litematica: sendMetadata to player {}", player.getName().getLiteralString());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.networkHandler != null)
        {
            LITEMATICA_HANDLER.sendPlayPayload(player.networkHandler, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            LITEMATICA_HANDLER.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.MetadataResponse(this.metadata)));
        }
    }

    public void onPacketFailure(ServerPlayerEntity player)
    {
        // Do something when packets fail, if required
    }

    public void onPacketFailure_Litematica(ServerPlayerEntity player)
    {
        // Do something when packets fail, if required
    }

    public void onBlockEntityRequest(ServerPlayerEntity player, BlockPos pos)
    {
        Servux.logger.warn("onBlockEntityRequest(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.getEntityWorld().getBlockEntity(pos);
        NbtCompound nbt = be != null ? be.createNbt(player.getRegistryManager()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onBlockEntityRequest_Litematica(ServerPlayerEntity player, BlockPos pos)
    {
        Servux.logger.warn("onBlockEntityRequest_Litematica(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.getEntityWorld().getBlockEntity(pos);
        NbtCompound nbt = be != null ? be.createNbt(player.getRegistryManager()) : new NbtCompound();
        LITEMATICA_HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayerEntity player, int entityId)
    {
        Servux.logger.warn("onEntityRequest(): from player {}", player.getName().getLiteralString());

        Entity entity = player.getWorld().getEntityById(entityId);
        NbtCompound nbt = entity != null ? entity.writeNbt(new NbtCompound()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleEntityResponse(entityId, nbt));
    }

    public void onEntityRequest_Litematica(ServerPlayerEntity player, int entityId)
    {
        Servux.logger.warn("onEntityRequest_Litematica(): from player {}", player.getName().getLiteralString());

        Entity entity = player.getWorld().getEntityById(entityId);
        NbtCompound nbt = entity != null ? entity.writeNbt(new NbtCompound()) : new NbtCompound();
        LITEMATICA_HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleEntityResponse(entityId, nbt));
    }

    public void handleBulkEntityData(ServerPlayerEntity player, int transactionId, NbtCompound tags)
    {
        Servux.logger.warn("handleBulkEntityData(): from player {}", player.getName().getLiteralString());
        // Paste to World
    }
}
