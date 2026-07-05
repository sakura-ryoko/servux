package fi.dy.masa.servux.dataproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxEntitiesHandler;
import fi.dy.masa.servux.network.packet.ServuxEntitiesPacket;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;

public class EntitiesDataProvider extends DataProviderBase
{
    public static final EntitiesDataProvider INSTANCE = new EntitiesDataProvider();
    private final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();
	private final NbtCompound metadata = new NbtCompound();
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
    public boolean isPlayerRegistered(ServerPlayerEntity player)
    {
	    return this.registeredPlayers.contains(player.getUuid()) && !this.isPlayerInvalid(player);
    }

    public void register(ServerPlayerEntity player)
    {
        if (!this.isEnabled()) return;

        if (this.hasPermission(player) == false)
        {
            // No Permission
            Servux.debugLog("entity_data: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        Servux.debugLog("entityDataChannel: sendMetadata to player {}", player.getName().getLiteralString());

		this.registeredPlayers.add(player.getUuid());

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
        this.setPlayerInvalid(player);
	    this.registeredPlayers.remove(player.getUuid());
    }

    public void removePlayer(ServerPlayerEntity player)
    {
        this.removeInvalidPlayer(player);
	    this.registeredPlayers.remove(player.getUuid());
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
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("onBlockEntityRequest(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.getEntityWorld().getBlockEntity(pos);
        NbtCompound nbt = be != null ? be.createNbtWithIdentifyingData(player.getRegistryManager()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayerEntity player, int entityId)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("onEntityRequest(): from player {} // entityId [{}]", player.getName().getLiteralString(), entityId);
        Entity entity = player.getWorld().getEntityById(entityId);
        NbtCompound nbt = new NbtCompound();

        if (entity != null)
        {
	        Identifier id = EntityType.getId(entity.getType());

            if (entity instanceof PlayerEntity)
            {
                nbt = entity.writeNbt(nbt);

                if (id != null)
                {
                    nbt.putString("id", id.toString());
                }

                if (!this.hasPlayerInventoryPermission(player))
                {
                    nbt.remove("Inventory");
                    nbt.put("Inventory", new NbtList());
                }
                if (!this.hasPlayerEnderItemsPermission(player))
                {
                    nbt.remove("EnderItems");
                    nbt.put("EnderItems", new NbtList());
                }

                HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleEntityResponse(entityId, nbt));
            }
            else if (entity.saveSelfNbt(nbt))
            {
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
    public boolean hasNbtQueryPermission(ServerPlayerEntity player)
    {
        if (this.nbtQueryOverride.getValue())
        {
            return Permissions.check(player, this.permNode+".nbt_query_override", this.nbtQueryPermissionLevel.getValue());
        }

        return player.hasPermissionLevel(2);
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
	public boolean hasPlayerInventoryPermission(ServerPlayerEntity player)
	{
		if (this.hasNbtAllowPlayerInventory())
		{
			return Permissions.check(player, this.permNode+".nbt_allow_player_inventory", this.playerInventoryPermissionLevel.getValue());
		}

		return false;
	}

	public boolean hasNbtAllowPlayerEnderItems()
	{
		return this.nbtAllowPlayerEnderItems.getValue();
	}

	public boolean hasPlayerEnderItemsPermission(ServerPlayerEntity player)
	{
		if (this.hasNbtAllowPlayerEnderItems())
		{
			return Permissions.check(player, this.permNode+".nbt_allow_player_ender_items", this.playerEnderItemsPermissionLevel.getValue());
		}

		return false;
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
