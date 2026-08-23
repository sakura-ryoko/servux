package fi.dy.masa.servux.dataproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxTweaksHandler;
import fi.dy.masa.servux.network.packet.ServuxTweaksPacket;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.IServuxSettingCallback;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.util.InventoryUtils;
import fi.dy.masa.servux.util.PermissionsUtil;
import fi.dy.masa.servux.util.StringUtils;
import fi.dy.masa.servux.util.data.Constants;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.ListData;
import fi.dy.masa.servux.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.servux.util.nbt.NbtView;

public class TweaksDataProvider extends DataProviderBase
{
    public static final TweaksDataProvider INSTANCE = new TweaksDataProvider();
	private final static ServuxTweaksHandler<ServuxTweaksPacket.Payload> HANDLER = ServuxTweaksHandler.getInstance();
    private final CompoundData metadata = new CompoundData();
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
    public void tick(MinecraftServer server, int tickCounter, ProfilerFiller profiler)
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
    public boolean isPlayerRegistered(ServerPlayer player)
    {
        return this.registeredPlayers.contains(player.getUUID()) && !this.isPlayerInvalid(player);
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
            if (this.metadata.contains("stackingShulkers", Constants.NBT.TAG_BYTE))
            {
                this.metadata.remove("stackingShulkers");
            }

            if (this.metadata.contains("stackingShulkersMax", Constants.NBT.TAG_INT))
            {
                this.metadata.remove("stackingShulkersMax");
            }
        }
    }

    public void updateAllTweaks(MinecraftServer server)
    {
        Servux.debugLog("tweaksData: Invoke updateAllTweaks()");
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        this.checkTweaksMetadata();

        for (ServerPlayer player : players)
        {
            if (this.isPlayerRegistered(player))
            {
                this.sendMetadataOnly(player);
            }
        }
    }

    @Override
    public void register(ServerPlayer player, CompoundData tags)
    {
        if (!this.isEnabled()) { return; }
        UUID uuid = player.getUUID();

        if (tags == null || tags.getIntOrDefault("version", -1) < this.getProtocolVersion())
        {
            Servux.LOGGER.warn("tweaks_data: Denying access for player {}, Insufficient Protocol Version; This Server Requires: Version {}", player.getName().tryCollapseToString(), this.getProtocolVersion());
            player.sendSystemMessage(StringUtils.translate("servux.general.error.protocol_version_too_low", this.getName()));
            HANDLER.tickFailures(player);
            return;
        }

        if (!this.hasPermission(player))
        {
            // No Permission
            Servux.debugLog("tweaks_data: Denying access for player {}, Insufficient Permissions", player.getName().tryCollapseToString());
            return;
        }

//        player.sendSystemMessage(StringUtils.translate("servux.general.error.protocol_version_too_low", this.getName()));

        Servux.debugLog("tweaks_data: sendMetadata to player {}", player.getName().tryCollapseToString());
        this.checkTweaksMetadata();
        this.registeredPlayers.add(uuid);
        this.sendMetadataOnly(player);
    }

    private void sendMetadataOnly(ServerPlayer player)
    {
        if (!this.isEnabled()) { return; }

        if (!this.hasPermission(player))
        {
            // No Permission
            Servux.debugLog("tweaks_data: Denying access for player {}, Insufficient Permissions", player.getName().tryCollapseToString());
            return;
        }

        CompoundData tags = new CompoundData();

        tags.combine(this.metadata);

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.connection != null)
        {
            HANDLER.sendPlayPayload(player.connection, new ServuxTweaksPacket.Payload(ServuxTweaksPacket.MetadataResponse(tags)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxTweaksPacket.Payload(ServuxTweaksPacket.MetadataResponse(tags)));
        }
    }

    @Override
    public void unregister(ServerPlayer player, CompoundData tags)
    {
        if (this.isEnabled())
        {
            Servux.debugLog("tweaks_data: Unregistered player {}", player.getName().tryCollapseToString());
        }

        UUID uuid = player.getUUID();
        HANDLER.resetFailures(this.getNetworkChannel(), player);
        this.registeredPlayers.remove(uuid);
    }

    @Override
    public void onPacketFailure(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        this.setPlayerInvalid(player);
        this.registeredPlayers.remove(uuid);
    }

    @Override
    public void removePlayer(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        this.removeInvalidPlayer(player);
        this.registeredPlayers.remove(uuid);
        HANDLER.resetFailures(this.getNetworkChannel(), player);
    }

    private void setPlayerInvalid(ServerPlayer player)
    {
        UUID uuid = player.getUUID();

        if (!this.invalidPlayers.contains(uuid))
        {
            this.invalidPlayers.add(uuid);
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

    public void onBlockEntityRequest(ServerPlayer player, BlockPos pos, @Nullable CompoundData tags)
    {
        if (!this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        if (!this.hasPermission(player))
        {
            Servux.debugLog("tweaks_data: Denying onBlockEntityRequest from player {}, Insufficient Permissions.", player.getName().getString());
            return;
        }

        //Servux.LOGGER.warn("onBlockEntityRequest(): from player {}", player.getName().getLiteralString());
        BlockEntity be = player.level().getBlockEntity(pos);

        if (be != null)
        {
            CompoundData nbt = DataConverterNbt.fromVanillaCompound(be.saveWithFullMetadata(player.registryAccess()));
            HANDLER.encodeServerData(player, ServuxTweaksPacket.SimpleBlockResponse(pos, nbt));
        }
    }

    public void onEntityRequest(ServerPlayer player, int entityId, @Nullable CompoundData tags)
    {
        if (!this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        if (!this.hasPermission(player))
        {
            Servux.debugLog("tweaks_data: Denying onEntityRequest from player {}, Insufficient Permissions.", player.getName().getString());
            return;
        }

        //Servux.logger.warn("onEntityRequest(): from player {} // entityId [{}]", player.getName().getLiteralString(), entityId);
        Entity entity = player.level().getEntity(entityId);

        if (entity != null)
        {
            NbtView view = NbtView.getWriter(player.level().registryAccess());
            Identifier id = EntityType.getKey(entity.getType());

            entity.saveWithoutId(view.getWriter());
            CompoundData nbt = view.readData();

            if (nbt != null && id != null)
            {
                if (entity.getType() == EntityTypes.PLAYER && !entity.getUUID().equals(player.getUUID()))
                {
                    if (!EntitiesDataProvider.INSTANCE.hasPlayerInventoryPermission(player))
                    {
                        nbt.remove("Inventory");
                        nbt.put("Inventory", new ListData());
                    }
                    if (!EntitiesDataProvider.INSTANCE.hasPlayerEnderItemsPermission(player))
                    {
                        nbt.remove("EnderItems");
                        nbt.put("EnderItems", new ListData());
                    }
                }

                nbt.putString("id", id.toString());
                HANDLER.encodeServerData(player, ServuxTweaksPacket.SimpleEntityResponse(entityId, nbt));
            }
        }
    }

    /*
    public void handleBulkClientRequest(ServerPlayer player, CompoundTag tags)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        Servux.LOGGER.warn("handleBulkClientRequest(): from player {} -- Not Implemented!", player.getName().getLiteralString());
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

        return stack.getComponents().getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

	@Override
    public boolean hasPermission(ServerPlayer player)
    {
        return PermissionsUtil.check(player, this.permNode, this.permissionLevel.getValue());
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
