package fi.dy.masa.servux.dataproviders;

import java.util.List;
import me.lucko.fabric.api.permissions.v0.Permissions;
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
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.util.StringUtils;

public class EntitiesDataProvider extends DataProviderBase
{
    public static final EntitiesDataProvider INSTANCE = new EntitiesDataProvider();
    protected final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();
    protected final NbtCompound metadata = new NbtCompound();
    protected ServuxIntSetting permissionLevel = new ServuxIntSetting(this,
            "permission_level",
            StringUtils.translate("servux.default_permission_level.name"),
            StringUtils.translate("servux.default_permission_level.comment"),
            0, 4, 0);
    private List<IServuxSetting<?>> settings = List.of(this.permissionLevel);

    protected EntitiesDataProvider()
    {
        super("entity_data",
                ServuxEntitiesHandler.CHANNEL_ID,
                ServuxEntitiesPacket.PROTOCOL_VERSION,
                0, Reference.MOD_ID+ ".provider.entity_data",
                "Entity Data provider for Client Side mods.");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);
    }

    @Override
    public List<IServuxSetting<?>> getSettings()
    {
        return settings;
    }

    @Override
    public void registerHandler()
    {
        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);
        if (this.isRegistered() == false)
        {
            HANDLER.registerPlayPayload(ServuxEntitiesPacket.Payload.ID, ServuxEntitiesPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
            this.setRegistered(true);
        }
        HANDLER.registerPlayReceiver(ServuxEntitiesPacket.Payload.ID, HANDLER::receivePlayPayload);
    }

    @Override
    public void unregisterHandler()
    {
        HANDLER.unregisterPlayReceiver();
        ServerPlayHandler.getInstance().unregisterServerPlayHandler(HANDLER);
    }

    @Override
    public IPluginServerPlayHandler<?> getPacketHandler()
    {
        return HANDLER;
    }


    public void sendMetadata(ServerPlayerEntity player)
    {
        if (this.hasPermission(player) == false)
        {
            // No Permission
            Servux.debugLog("entity_data: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        //Servux.logger.warn("entityDataChannel: sendMetadata to player {}", player.getName().getLiteralString());

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

    public void onPacketFailure(ServerPlayerEntity player)
    {
        // Do something when packets fail, if required
    }

    public void onBlockEntityRequest(ServerPlayerEntity player, BlockPos pos)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        //Servux.logger.warn("onBlockEntityRequest(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.getEntityWorld().getBlockEntity(pos);
        NbtCompound nbt = be != null ? be.createNbt(player.getRegistryManager()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayerEntity player, int entityId)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        //Servux.logger.warn("onEntityRequest(): from player {}", player.getName().getLiteralString());

        Entity entity = player.getWorld().getEntityById(entityId);
        NbtCompound nbt = entity != null ? entity.writeNbt(new NbtCompound()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleEntityResponse(entityId, nbt));
    }

    public void handleBulkClientRequest(ServerPlayerEntity player, int transactionId, NbtCompound tags)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        Servux.logger.warn("handleBulkClientRequest(): from player {} -- Not Implemented!", player.getName().getLiteralString());
        // todo
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode, this.permissionLevel.getValue());
    }

    @Override
    public void onTickEndPre()
    {
        // NO-OP
    }

    @Override
    public void onTickEndPost()
    {
        // NO-OP
    }
}
