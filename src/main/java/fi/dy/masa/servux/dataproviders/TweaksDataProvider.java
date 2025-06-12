package fi.dy.masa.servux.dataproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.lucko.fabric.api.permissions.v0.Permissions;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxTweaksHandler;
import fi.dy.masa.servux.network.packet.ServuxTweaksPacket;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.util.nbt.NbtView;

public class TweaksDataProvider extends DataProviderBase
{
    public static final TweaksDataProvider INSTANCE = new TweaksDataProvider();
    protected final static ServuxTweaksHandler<ServuxTweaksPacket.Payload> HANDLER = ServuxTweaksHandler.getInstance();
    protected final NbtCompound metadata = new NbtCompound();
    protected ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
    protected List<IServuxSetting<?>> settings = List.of(this.permissionLevel);

    private final List<UUID> invalidPlayers = new ArrayList<>();

    protected TweaksDataProvider()
    {
        super("tweaks_data",
                ServuxTweaksHandler.CHANNEL_ID,
                ServuxTweaksPacket.PROTOCOL_VERSION,
                0, Reference.MOD_ID+ ".provider.tweaks_data",
                "Tweaks Data provider for Client Side mods.");

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
            HANDLER.registerPlayPayload(ServuxTweaksPacket.Payload.ID, ServuxTweaksPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
            this.setRegistered(true);
        }
        HANDLER.registerPlayReceiver(ServuxTweaksPacket.Payload.ID, HANDLER::receivePlayPayload);
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

    @Override
    public boolean isPlayerRegistered(ServerPlayerEntity player)
    {
        return !this.isPlayerInvalid(player);
    }

    public void sendMetadata(ServerPlayerEntity player)
    {
        if (!this.isEnabled()) return;

        if (this.hasPermission(player) == false)
        {
            // No Permission
            Servux.debugLog("tweaks_service: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        Servux.debugLog("tweaksDataChannel: sendMetadata to player {}", player.getName().getLiteralString());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.networkHandler != null)
        {
            HANDLER.sendPlayPayload(player.networkHandler, new ServuxTweaksPacket.Payload(ServuxTweaksPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxTweaksPacket.Payload(ServuxTweaksPacket.MetadataResponse(this.metadata)));
        }
    }

    public void onPacketFailure(ServerPlayerEntity player)
    {
        this.setPlayerInvalid(player);
    }

    public void removePlayer(ServerPlayerEntity player)
    {
        this.removeInvalidPlayer(player);
    }

    private void setPlayerInvalid(ServerPlayerEntity player)
    {
        if (!this.invalidPlayers.contains(player.getUuid()))
        {
            this.invalidPlayers.add(player.getUuid());
        }
    }

    private boolean isPlayerInvalid(ServerPlayerEntity player)
    {
        return this.invalidPlayers.contains(player.getUuid());
    }

    private void removeInvalidPlayer(ServerPlayerEntity player)
    {
        this.invalidPlayers.remove(player.getUuid());
    }

    public void onBlockEntityRequest(ServerPlayerEntity player, BlockPos pos)
    {
        if (this.hasPermission(player) == false || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("onBlockEntityRequest(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.getWorld().getBlockEntity(pos);
        NbtCompound nbt = be != null ? be.createNbt(player.getRegistryManager()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxTweaksPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayerEntity player, int entityId)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        //Servux.logger.warn("onEntityRequest(): from player {} // entityId [{}]", player.getName().getLiteralString(), entityId);
        Entity entity = player.getWorld().getEntityById(entityId);

        if (entity != null)
        {
            NbtView view = NbtView.getWriter(player.getWorld().getRegistryManager());
            Identifier id = EntityType.getId(entity.getType());

            entity.writeData(view.getWriter());
            NbtCompound nbt = view.readNbt();

            if (nbt != null && id != null)
            {
                nbt.putString("id", id.toString());
                HANDLER.encodeServerData(player, ServuxTweaksPacket.SimpleEntityResponse(entityId, nbt.copy()));
            }
        }
    }

    /*
    public void handleBulkClientRequest(ServerPlayerEntity player, int transactionId, NbtCompound tags)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        Servux.logger.warn("handleBulkClientRequest(): from player {} -- Not Implemented!", player.getName().getLiteralString());
    }

    public void handleClientBulkData(ServerPlayerEntity player, int transactionId, NbtCompound nbtCompound)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        Servux.logger.warn("handleClientBulkData(): from player {} -- Not Implemented!", player.getName().getLiteralString());
    }
     */

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
