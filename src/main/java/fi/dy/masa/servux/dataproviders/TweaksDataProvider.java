package fi.dy.masa.servux.dataproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.dy.masa.servux.settings.IServuxSettingCallback;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.util.InventoryUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.util.profiler.Profiler;

public class TweaksDataProvider extends DataProviderBase
{
    public static final TweaksDataProvider INSTANCE = new TweaksDataProvider();
	private final static ServuxTweaksHandler<ServuxTweaksPacket.Payload> HANDLER = ServuxTweaksHandler.getInstance();
    private final NbtCompound metadata = new NbtCompound();
    private final BoolCallbacks boolCallback = new BoolCallbacks();
    private final IntCallbacks intCallback = new IntCallbacks();
	private final ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0, this.intCallback);
	private final ServuxIntSetting updateInterval = new ServuxIntSetting(this, "update_interval", 120, 1200, 40, this.intCallback);
	private final ServuxBoolSetting stackableShulkers = new ServuxBoolSetting(this, "stackable_shulkers", false, this.boolCallback);
	private final ServuxIntSetting stackableShulkersSize = new ServuxIntSetting(this, "stackable_shulkers_count", 64, 99, 1, this.intCallback);
	private final ServuxBoolSetting stackableShulkersFix = new ServuxBoolSetting(this, "stackable_shulkers_fix", true, this.boolCallback);
	private final List<IServuxSetting<?>> settings = List.of(
            this.permissionLevel,
            this.updateInterval,
            this.stackableShulkers,
            this.stackableShulkersSize,
            this.stackableShulkersFix
    );

    private final List<UUID> registeredPlayers = new ArrayList<>();
    private final List<UUID> invalidPlayers = new ArrayList<>();
    private boolean configDirty = false;

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

        this.setTickRate(40);
        this.checkTweaksMetadata();
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
    public boolean shouldTick()
    {
        return this.isEnabled();
    }

    @Override
    public void tick(MinecraftServer server, int tickCounter, Profiler profiler)
    {
        if (!this.isEnabled()) return;

        if ((tickCounter % this.updateInterval.getValue()) == 0)
        {
            profiler.push(this.getName());

            if (this.configDirty)
            {
                this.updateAllTweaks(server);
                this.configDirty = false;
            }

            profiler.pop();
        }
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

    private void checkTweaksMetadata()
    {
        // Only send the config when the Tweak is enabled;
        // (ie; don't turn it off in case they are using Carpet)
        if (this.shouldEmptyShulkersStack())
        {
            this.metadata.putBoolean("stackingShulkers", this.shouldEmptyShulkersStack());
            this.metadata.putInt("stackingShulkersMax", this.stackableShulkersSize.getValue());
        }
        else
        {
            if (this.metadata.contains("stackingShulkers"))
            {
                this.metadata.remove("stackingShulkers");
            }

            if (this.metadata.contains("stackingShulkersMax"))
            {
                this.metadata.remove("stackingShulkersMax");
            }
        }
    }

    public void updateAllTweaks(MinecraftServer server)
    {
        Servux.debugLog("tweaksData: Invoke updateAllTweaks()");
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();

        this.checkTweaksMetadata();

        for (ServerPlayerEntity player : players)
        {
            if (this.isPlayerRegistered(player))
            {
                this.register(player);
            }
        }
    }

    public void register(ServerPlayerEntity player)
    {
        if (!this.isEnabled()) return;

        if (!this.hasPermission(player))
        {
            // No Permission
            Servux.debugLog("tweaks_service: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        Servux.debugLog("tweaksDataChannel: sendMetadata to player {}", player.getName().getLiteralString());
        this.checkTweaksMetadata();

        this.registeredPlayers.add(player.getUuid());

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
        NbtCompound nbt = be != null ? be.createNbt(player.getRegistryManager()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxTweaksPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayerEntity player, int entityId)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("onEntityRequest(): from player {} // entityId [{}]", player.getName().getLiteralString(), entityId);
        Entity entity = player.getEntityWorld().getEntityById(entityId);

        if (entity != null)
        {
            NbtView view = NbtView.getWriter(player.getEntityWorld().getRegistryManager());
            Identifier id = EntityType.getId(entity.getType());

            entity.writeData(view.getWriter());
            NbtCompound nbt = view.readNbt();

            if (nbt != null && id != null)
            {
	            if (entity.getType() == EntityType.PLAYER)
	            {
		            if (!EntitiesDataProvider.INSTANCE.hasPlayerInventoryPermission(player))
		            {
			            nbt.remove("Inventory");
			            nbt.put("Inventory", new NbtList());
		            }
		            if (!EntitiesDataProvider.INSTANCE.hasPlayerEnderItemsPermission(player))
		            {
			            nbt.remove("EnderItems");
			            nbt.put("EnderItems", new NbtList());
		            }
	            }

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

    public boolean shouldEmptyShulkersStack()
    {
        return this.stackableShulkers.getValue();
    }

    public boolean isStackableShulkersFixActive()
    {
        return this.shouldEmptyShulkersStack() && this.stackableShulkersFix.getValue();
    }

    public int defaultEmptyShulkersMaxCount()
    {
        if (this.shouldEmptyShulkersStack())
        {
            return this.stackableShulkersSize.getValue();
        }

        return 1;
    }

    public int getEmptyShulkersMaxCount(ItemStack stack)
    {
        if (this.shouldEmptyShulkersStack() && InventoryUtils.isShulkerBox(stack))
        {
            return this.defaultEmptyShulkersMaxCount();
        }

        return stack.getComponents().getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);
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

    // Callbacks marks the config as dirty so that we can broadcast the config changes
    public static class BoolCallbacks implements IServuxSettingCallback<Boolean>
    {
        @Override
        public void onValueChanged(IServuxSetting<Boolean> setting, Boolean oldValue, Boolean value)
        {
            Servux.debugLog("Config Change detected; {}:{}", setting.dataProvider().getName(), setting.name());
            TweaksDataProvider.INSTANCE.configDirty = true;
        }
    }

    public static class IntCallbacks implements IServuxSettingCallback<Integer>
    {
        @Override
        public void onValueChanged(IServuxSetting<Integer> setting, Integer oldValue, Integer value)
        {
            Servux.debugLog("Config Change detected; {}:{}", setting.dataProvider().getName(), setting.name());
            TweaksDataProvider.INSTANCE.configDirty = true;
        }
    }
}
