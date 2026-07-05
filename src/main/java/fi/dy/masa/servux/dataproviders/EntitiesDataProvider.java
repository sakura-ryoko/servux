package fi.dy.masa.servux.dataproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxEntitiesHandler;
import fi.dy.masa.servux.network.packet.ServuxEntitiesPacket;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.util.PermissionsUtil;
import fi.dy.masa.servux.util.nbt.NbtView;

public class EntitiesDataProvider extends DataProviderBase
{
    public static final EntitiesDataProvider INSTANCE = new EntitiesDataProvider();
    private final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();
	private final CompoundTag metadata = new CompoundTag();
	private final ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
	private final ServuxBoolSetting nbtQueryOverride = new ServuxBoolSetting(this, "nbt_query_override", false);
	private final ServuxIntSetting nbtQueryPermissionLevel = new ServuxIntSetting(this, "nbt_query_permission_level", 2, 4, 0);
	private final ServuxBoolSetting fixAllayGathering = new ServuxBoolSetting(this, "fix_allay_gathering", true);
	private final ServuxBoolSetting nbtAllowPlayerInventory = new ServuxBoolSetting(this, "nbt_allow_player_inventory", true);
	private final ServuxBoolSetting nbtAllowPlayerEnderItems = new ServuxBoolSetting(this, "nbt_allow_player_ender_items", true);
	private final ServuxIntSetting playerInventoryPermissionLevel = new ServuxIntSetting(this, "player_inventory_permission_level", 2, 4, 0);
	private final ServuxIntSetting playerEnderItemsPermissionLevel = new ServuxIntSetting(this, "player_ender_items_permission_level", 2, 4, 0);
	private final List<IServuxSetting<?>> settings = List.of(
			this.permissionLevel,
			this.nbtQueryOverride,
			this.nbtQueryPermissionLevel,
			this.fixAllayGathering,
			this.nbtAllowPlayerInventory,
			this.nbtAllowPlayerEnderItems,
			this.playerInventoryPermissionLevel,
			this.playerEnderItemsPermissionLevel
	);

	private final List<UUID> registeredPlayers = new ArrayList<>();
    private final List<UUID> invalidPlayers = new ArrayList<>();

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

        if (!this.isRegistered())
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

    @Override
    public boolean isPlayerRegistered(ServerPlayer player)
    {
	    return this.registeredPlayers.contains(player.getUUID()) && !this.isPlayerInvalid(player);
    }

    public void register(ServerPlayer player)
    {
        if (!this.isEnabled()) return;

        if (!this.hasPermission(player))
        {
            // No Permission
            Servux.debugLog("entity_data: Denying access for player {}, Insufficient Permissions", player.getName().tryCollapseToString());
            return;
        }

        Servux.debugLog("entityDataChannel: sendMetadata to player {}", player.getName().tryCollapseToString());

		this.registeredPlayers.add(player.getUUID());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.connection != null)
        {
            HANDLER.sendPlayPayload(player.connection, new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.MetadataResponse(this.metadata)));
        }
    }

    public void onPacketFailure(ServerPlayer player)
    {
        this.setPlayerInvalid(player);
	    this.registeredPlayers.remove(player.getUUID());
    }

    public void removePlayer(ServerPlayer player)
    {
        this.removeInvalidPlayer(player);
	    this.registeredPlayers.remove(player.getUUID());
    }

    private void setPlayerInvalid(ServerPlayer player)
    {
        if (!this.invalidPlayers.contains(player.getUUID()))
        {
            this.invalidPlayers.add(player.getUUID());
        }
    }

    private boolean isPlayerInvalid(ServerPlayer player)
    {
        return this.invalidPlayers.contains(player.getUUID());
    }

    private void removeInvalidPlayer(ServerPlayer player)
    {
        this.invalidPlayers.remove(player.getUUID());
    }

    public void onBlockEntityRequest(ServerPlayer player, BlockPos pos)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("onBlockEntityRequest(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.level().getBlockEntity(pos);
        CompoundTag nbt = be != null ? be.saveWithFullMetadata(player.registryAccess()) : new CompoundTag();
        HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayer player, int entityId)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("onEntityRequest(): from player {} // entityId [{}]", player.getName().getLiteralString(), entityId);
        Entity entity = player.level().getEntity(entityId);

        if (entity != null)
        {
            NbtView view = NbtView.getWriter(player.level().registryAccess());
            Identifier id = EntityType.getKey(entity.getType());

            entity.saveWithoutId(view.getWriter());
            CompoundTag nbt = view.readNbt();

            if (nbt != null && id != null)
            {
				if (entity.getType() == EntityType.PLAYER)
				{
					if (!this.hasPlayerInventoryPermission(player))
					{
						nbt.remove("Inventory");
						nbt.put("Inventory", new ListTag());
					}
					if (!this.hasPlayerEnderItemsPermission(player))
					{
						nbt.remove("EnderItems");
						nbt.put("EnderItems", new ListTag());
					}
				}

                nbt.putString("id", id.toString());
                HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleEntityResponse(entityId, nbt));
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
     */

    public boolean hasNbtQueryOverride()
    {
        return this.isEnabled() && this.nbtQueryOverride.getValue();
    }

	public boolean hasFixAllayGathering()
	{
		return this.isEnabled() && this.fixAllayGathering.getValue();
	}

	/**
	 * Tweaks Data Provider also uses the same settings here.
	 * @param player ()
	 * @return ()
	 */
    public boolean hasNbtQueryPermission(ServerPlayer player)
    {
        if (this.nbtQueryOverride.getValue())
        {
            return PermissionsUtil.check(player, this.permNode+".nbt_query_override", this.nbtQueryPermissionLevel.getValue());
        }

        return player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

	public boolean hasNbtAllowPlayerInventory()
	{
		return this.nbtAllowPlayerInventory.getValue();
	}

	/**
	 * Tweaks Data Provider also uses the same settings here.
	 * @param player ()
	 * @return ()
	 */
	public boolean hasPlayerInventoryPermission(ServerPlayer player)
	{
		if (this.hasNbtAllowPlayerInventory())
		{
			return PermissionsUtil.check(player, this.permNode+".nbt_allow_player_inventory", this.playerInventoryPermissionLevel.getValue());
		}

		return false;
	}

	public boolean hasNbtAllowPlayerEnderItems()
	{
		return this.nbtAllowPlayerEnderItems.getValue();
	}

	public boolean hasPlayerEnderItemsPermission(ServerPlayer player)
	{
		if (this.hasNbtAllowPlayerEnderItems())
		{
			return PermissionsUtil.check(player, this.permNode+".nbt_allow_player_ender_items", this.playerEnderItemsPermissionLevel.getValue());
		}

		return false;
	}

	@Override
    public boolean hasPermission(ServerPlayer player)
    {
        return PermissionsUtil.check(player, this.permNode, this.permissionLevel.getValue());
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
