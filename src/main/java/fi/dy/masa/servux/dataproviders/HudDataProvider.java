package fi.dy.masa.servux.dataproviders;

import com.mojang.serialization.DataResult;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.loggers.DataLogger;
import fi.dy.masa.servux.loggers.DataLoggerBase;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxHudHandler;
import fi.dy.masa.servux.network.packet.ServuxHudPacket;
import fi.dy.masa.servux.settings.*;
import fi.dy.masa.servux.util.StringUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class HudDataProvider extends DataProviderBase
{
    public static final HudDataProvider INSTANCE = new HudDataProvider();
    protected final static ServuxHudHandler<ServuxHudPacket.Payload> HANDLER = ServuxHudHandler.getInstance();
    protected final CompoundTag metadata = new CompoundTag();
    private final ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
	private final ServuxIntSetting updateInterval = new ServuxIntSetting(this, "update_interval", 40, 300, 20);
	private final ServuxBoolSetting shareWeatherStatus = new ServuxBoolSetting(this, "share_weather_status", false);
	private final ServuxIntSetting weatherPermissionLevel = new ServuxIntSetting(this, "weather_permission_level", 0, 4, 0);
	private final ServuxBoolSetting shareSeed = new ServuxBoolSetting(this, "share_seed", false);
	private final ServuxIntSetting seedPermissionLevel = new ServuxIntSetting(this, "seed_permission_level", 2, 4, 0);
	private final ServuxBoolSetting loggersEnabled = new ServuxBoolSetting(this, "loggers_enabled", false, new BoolCallback());
	private final ServuxStringListSetting loggersEnableList = new ServuxStringListSetting(this, "loggers_enable_list", this.getDefaultLoggers(), new StringListCallback());
	private final ServuxIntSetting loggerPermissionLevel = new ServuxIntSetting(this, "logger_permission_level", 0, 4, 0);
	private final List<IServuxSetting<?>> settings = List.of(
            this.permissionLevel, this.updateInterval,
            this.shareWeatherStatus, this.weatherPermissionLevel,
            this.shareSeed, this.seedPermissionLevel,
            this.loggersEnabled, this.loggersEnableList, this.loggerPermissionLevel
    );

    private GlobalPos spawnPos = new GlobalPos(Level.OVERWORLD, BlockPos.ZERO);
//    private int spawnChunkRadius = -1;
    private long worldSeed = 0;
    private int clearWeatherTime = -1;
    private int rainWeatherTime = -1;
    private int thunderWeatherTime = -1;
    private boolean isRaining;
    private boolean isThundering;
    private long lastTick;
    private long lastWeatherTick;
    private boolean refreshSpawnMetadata;
    private boolean refreshWeatherData;
    private final List<UUID> registeredPlayers = new ArrayList<>();
    private final List<UUID> invalidPlayers = new ArrayList<>();

    private final HashMap<UUID, List<DataLogger>> loggerPlayers = new HashMap<>();
    private final HashMap<DataLogger, DataLoggerBase<?>> LOGGERS = new HashMap<>();
    private final HashMap<DataLogger, Tag> DATA = new HashMap<>();

    protected HudDataProvider()
    {
        super("hud_data",
              ServuxHudHandler.CHANNEL_ID,
              ServuxHudPacket.PROTOCOL_VERSION,
              0, Reference.MOD_ID+ ".provider.hud_data",
              "MiniHUD Meta Data provider for various Server-Side information");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);

        // Spawn Metadata
        this.metadata.putString("spawnDimension", this.getSpawnPos().dimension().identifier().toString());
        this.metadata.putInt("spawnPosX", this.getSpawnPos().pos().getX());
        this.metadata.putInt("spawnPosY", this.getSpawnPos().pos().getY());
        this.metadata.putInt("spawnPosZ", this.getSpawnPos().pos().getZ());
//        this.metadata.putInt("spawnChunkRadius", this.getSpawnChunkRadius());

        // Loggers
        this.checkIfLoggersAreInitialized();
    }

    private List<String> getDefaultLoggers()
    {
        List<String> list = new ArrayList<>();

        for (DataLogger type : DataLogger.VALUES)
        {
            list.add(type.getSerializedName());
        }

        return list;
    }

    private void resetLoggersFromConfig()
    {
        if (this.isLoggersEnabled())
        {
            this.loggersEnabled.setValueNoCallback(false);
            this.checkIfLoggersAreInitialized();
            this.loggersEnabled.setValueNoCallback(true);
        }
        else
        {
            this.checkIfLoggersAreInitialized();
        }
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
            HANDLER.registerPlayPayload(ServuxHudPacket.Payload.ID, ServuxHudPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
            this.setRegistered(true);
        }

        HANDLER.registerPlayReceiver(ServuxHudPacket.Payload.ID, HANDLER::receivePlayPayload);
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

    @Override
    public boolean shouldTick()
    {
        return this.enabled;
    }

    @Override
    public void tick(MinecraftServer server, int tickCounter, ProfilerFiller profiler)
    {
        if (!this.isEnabled()) return;

        List<ServerPlayer> playerList = server.getPlayerList().getPlayers();

        if ((tickCounter % this.updateInterval.getValue()) == 0)
        {
            profiler.push(this.getName()+"_tick_weather");
            this.lastTick = tickCounter;
            
//            int radius = this.getSpawnChunkRadius();
//            int rule = server.getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS);
//            if (radius != rule)
//            {
//                this.setSpawnChunkRadius(rule);
//            }
            if (this.worldSeed == 0)
            {
                this.checkWorldSeed(server);
            }
            else if (this.shareSeed.getValue() == false)
            {
                this.setWorldSeed(0);
            }

            profiler.popPush(this.getName() + "_weather_players");
            for (ServerPlayer player : playerList)
            {
                if (this.isPlayerInvalid(player)) continue;

                if (this.shouldRefreshWeatherData())
                {
                    this.refreshWeatherData(player, null);
                }
                if (this.shouldRefreshSpawnMetadata())
                {
                    this.refreshSpawnMetadata(player, null);
                }
            }

            if (this.shouldRefreshWeatherData())
            {
                this.lastWeatherTick = tickCounter;
                this.setRefreshWeatherDataComplete();
            }
            if (this.shouldRefreshSpawnMetadata())
            {
                this.setRefreshSpawnMetadataComplete();
            }

            profiler.pop();
        }

        this.checkIfLoggersAreInitialized();

        if (this.isLoggersEnabled())
        {
            // Update Logger Data
            profiler.push(this.getName() + "_tick_loggers");
            this.tickLoggers(server);

            profiler.popPush(this.getName() + "_logger_players");
            for (ServerPlayer player : playerList)
            {
                this.tickLoggerPlayer(player);
            }
            profiler.pop();
        }
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

    public void tickWeather(int clearTime, int rainTime, int thunderTime, boolean isRaining, boolean isThunder)
    {
        if (!this.isEnabled()) return;

        this.clearWeatherTime = clearTime;
        this.rainWeatherTime = rainTime;
        this.thunderWeatherTime = thunderTime;
        this.isRaining = isRaining;
        this.isThundering = isThunder;

        if ((this.lastTick - this.lastWeatherTick) > this.getTickInterval())
        {
            // Don't spam players with weather ticks
            this.refreshWeatherData = true;
        }
    }

    private void checkIfLoggersAreInitialized()
    {
        if (this.isLoggersEnabled())
        {
            if (!this.metadata.contains("Loggers"))
            {
                this.metadata.put("Loggers", this.putEnabledLoggers());
            }

            this.setTickRate(15);

            if (this.LOGGERS.isEmpty())
            {
                this.initializeLoggers();
            }
        }
        else
        {
            if (this.metadata.contains("Loggers"))
            {
                this.metadata.remove("Loggers");
            }

            if (!this.LOGGERS.isEmpty())
            {
                this.LOGGERS.clear();
            }

            this.setTickRate(40);

            if (!this.DATA.isEmpty())
            {
                this.DATA.clear();
            }
        }
    }

    private CompoundTag putEnabledLoggers()
    {
        CompoundTag nbt = new CompoundTag();

        this.validateLoggerListConfig();

        for (DataLogger type : DataLogger.VALUES)
        {
            nbt.putBoolean(type.getSerializedName(), this.isLoggerTypeEnabled(type));
        }

        return nbt;
    }

    private void validateLoggerListConfig()
    {
        List<String> list = this.loggersEnableList.getValue();
        List<String> safeList = new ArrayList<>();
        boolean dirty = false;

        for (String entry : list)
        {
            DataLogger type = DataLogger.fromStringStatic(entry);

            if (type == null)
            {
                Servux.LOGGER.warn("validateLoggerListConfig: Removing invalid logger type: '{}'", entry);
                dirty = true;
            }
            else
            {
                safeList.add(entry);
            }
        }

        if (dirty)
        {
            this.loggersEnableList.setValueNoCallback(safeList);
        }
    }

    private boolean isLoggerTypeEnabled(DataLogger type)
    {
        return this.loggersEnableList.getValue().contains(type.getSerializedName());
    }

    private void initializeLoggers()
    {
        if (this.LOGGERS.isEmpty())
        {
            for (DataLogger type : DataLogger.VALUES)
            {
                if (this.isLoggerTypeEnabled(type))
                {
                    DataLoggerBase<?> entry = type.init();

                    if (entry != null)
                    {
                        this.LOGGERS.put(type, entry);
                    }
                }
            }
        }
    }

    private void tickLoggers(MinecraftServer server)
    {
        this.DATA.clear();
        if (!this.isLoggersEnabled()) return;

        this.LOGGERS.forEach(
                (type, logger) ->
                        this.DATA.put(type, (Tag) (logger.getResult(server)))
        );
    }

    private void tickLoggerPlayer(ServerPlayer player)
    {
        if (!this.isLoggersEnabled()) return;

        UUID uuid = player.getUUID();

        if (this.loggerPlayers.containsKey(uuid))
        {
            List<DataLogger> list = this.loggerPlayers.get(uuid);

            if (!list.isEmpty())
            {
                CompoundTag nbt = new CompoundTag();

                for (DataLogger type : list)
                {
                    if (this.DATA.containsKey(type))
                    {
                        nbt.put(type.getSerializedName(), this.DATA.get(type));
                    }
                }

                HANDLER.encodeServerData(player, ServuxHudPacket.DataLoggerTick(nbt));
            }
        }
    }

    public void register(ServerPlayer player)
    {
        if (!this.isEnabled()) return;

        if (!this.hasPermission(player))
        {
            // No Permission
            Servux.debugLog("hud_service: Denying access for player {}, Insufficient Permissions", player.getName().tryCollapseToString());
            return;
        }

        this.removeInvalidPlayer(player);

        CompoundTag nbt = new CompoundTag();
        nbt.merge(this.metadata);

        if (!this.hasPermissionsForSeed(player) && nbt.contains("worldSeed"))
        {
            nbt.remove("worldSeed");
        }

        Servux.debugLog("hudDataChannel: sendMetadata to player {}", player.getName().tryCollapseToString());

        this.registeredPlayers.add(player.getUUID());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.connection != null)
        {
            HANDLER.sendPlayPayload(player.connection, new ServuxHudPacket.Payload(ServuxHudPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxHudPacket.Payload(ServuxHudPacket.MetadataResponse(this.metadata)));
        }
    }

    public void refreshLoggers(ServerPlayer player, @Nonnull CompoundTag nbt)
    {
        if (!this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }
        if (!this.hasPermissionsForLoggers(player))
        {
            player.sendSystemMessage(StringUtils.translate("servux.hud_data.error.insufficient_for_loggers", "any"));
            return;
        }

        if (!nbt.isEmpty())
        {
            List<DataLogger> list = new ArrayList<>();
            UUID uuid = player.getUUID();

            for (String key : nbt.keySet())
            {
                DataLogger type = DataLogger.fromStringStatic(key);
                boolean enable = nbt.getBooleanOr(key, false);

                if (type != null)
                {
                    if (this.hasPermissionsForLogger(player, key) && enable)
                    {
                        list.add(type);
                    }
                    else if (!enable)
                    {
                        continue;
                    }
                    else
                    {
                        player.sendSystemMessage(StringUtils.translate("servux.hud_data.error.insufficient_for_loggers", key));
                    }
                }
            }

            if (!list.isEmpty())
            {
                this.loggerPlayers.put(uuid, list);
            }
            else
            {
                this.loggerPlayers.remove(uuid);
            }
        }
    }

    public void onPacketFailure(ServerPlayer player)
    {
        this.setPlayerInvalid(player);
        this.removePlayerLoggers(player);
        this.registeredPlayers.remove(player.getUUID());
    }

    public void removePlayer(ServerPlayer player)
    {
        this.removeInvalidPlayer(player);
        this.removePlayerLoggers(player);
        this.registeredPlayers.remove(player.getUUID());
    }

    private void removePlayerLoggers(ServerPlayer player)
    {
        this.loggerPlayers.remove(player.getUUID());
    }

    public void refreshSpawnMetadata(ServerPlayer player, @Nullable CompoundTag data)
    {
        if (!this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

	    GlobalPos spawnPos = this.getSpawnPos();
        CompoundTag nbt = new CompoundTag();

        nbt.putString("id", getNetworkChannel().toString());
        nbt.putString("servux", Reference.MOD_STRING);
        nbt.putInt("version", this.getProtocolVersion());
        nbt.putString("spawnDimension", spawnPos.dimension().identifier().toString());
        nbt.putInt("spawnPosX", spawnPos.pos().getX());
        nbt.putInt("spawnPosY", spawnPos.pos().getY());
        nbt.putInt("spawnPosZ", spawnPos.pos().getZ());
//        nbt.putInt("spawnChunkRadius", this.getSpawnChunkRadius());

        if (this.shareSeed.getValue() && this.hasPermissionsForSeed(player))
        {
            Servux.debugLog("refreshSpawnMetadata() player [{}] has seedPermissions.", player.getName().tryCollapseToString());
            nbt.putLong("worldSeed", this.worldSeed);
        }
        else
        {
            Servux.debugLog("refreshSpawnMetadata() player [{}] does not have seedPermissions.", player.getName().tryCollapseToString());
        }

        HANDLER.encodeServerData(player, ServuxHudPacket.SpawnResponse(nbt));
    }

    public void refreshWeatherData(ServerPlayer player, @Nullable CompoundTag data)
    {
        if (!this.hasPermissionsForWeather(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

	    CompoundTag nbt = new CompoundTag();

	    nbt.putString("id", getNetworkChannel().toString());
        nbt.putString("servux", Reference.MOD_STRING);

        if (this.isRaining && this.rainWeatherTime > -1)
        {
            nbt.putInt("SetRaining", this.rainWeatherTime);
            nbt.putBoolean("isRaining", true);
        }
        else
        {
            nbt.putBoolean("isRaining", false);
        }

        if (this.isThundering && this.thunderWeatherTime > -1)
        {
            nbt.putInt("SetThundering", this.thunderWeatherTime);
            nbt.putBoolean("isThundering", true);
        }
        else
        {
            nbt.putBoolean("isThundering", false);
        }

        if (this.clearWeatherTime > -1)
        {
            nbt.putInt("SetClear", this.clearWeatherTime);
        }

        HANDLER.encodeServerData(player, ServuxHudPacket.WeatherTick(nbt));
    }

    public void refreshRecipeManager(ServerPlayer player, @Nullable CompoundTag data)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        ServerLevel world = player.level();
        Collection<RecipeHolder<?>> recipes = world.recipeAccess().getRecipes();
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();

        if (data != null)
        {
            Servux.debugLog("hudDataChannel: received RecipeManager request from {}, client version: {}", player.getName().tryCollapseToString(), data.getStringOr("version", "?"));
        }

        recipes.forEach((recipeEntry ->
        {
            DataResult<Tag> dr = Recipe.CODEC.encodeStart(NbtOps.INSTANCE, recipeEntry.value());

            if (dr.result().isPresent())
            {
                CompoundTag entry = new CompoundTag();
                entry.putString("id_reg", recipeEntry.id().registry().toString());
                entry.putString("id_value", recipeEntry.id().identifier().toString());
                entry.put("recipe", dr.result().get());
                list.add(entry);
            }
        }));

        nbt.put("RecipeManager", list);

        // Use Packet Splitter
        HANDLER.encodeServerData(player, ServuxHudPacket.ResponseS2CStart(nbt));
    }

    public GlobalPos getSpawnPos()
    {
        if (this.spawnPos == null)
        {
            this.spawnPos = new GlobalPos(ServerLevel.OVERWORLD, BlockPos.ZERO);
        }

        return this.spawnPos;
    }

    public String getSpawnPosAsString()
    {
        GlobalPos pos = this.getSpawnPos();

        return String.format("[%s: %d, %d, %d]", pos.dimension().identifier().toString(), pos.pos().getX(), pos.pos().getY(), pos.pos().getZ());
    }

    public String getSpawnPosAsString(GlobalPos pos)
    {
        return String.format("[%s: %d, %d, %d]", pos.dimension().identifier().toString(), pos.pos().getX(), pos.pos().getY(), pos.pos().getZ());
    }

    public void setSpawnPos(GlobalPos spawnPos)
    {
        if (!this.spawnPos.equals(spawnPos))
        {
            this.metadata.remove("spawnDimension");
            this.metadata.remove("spawnPosX");
            this.metadata.remove("spawnPosY");
            this.metadata.remove("spawnPosZ");
            this.metadata.putString("spawnDimension", spawnPos.dimension().identifier().toString());
            this.metadata.putInt("spawnPosX", spawnPos.pos().getX());
            this.metadata.putInt("spawnPosY", spawnPos.pos().getY());
            this.metadata.putInt("spawnPosZ", spawnPos.pos().getZ());
            this.refreshSpawnMetadata = true;

            Servux.debugLog("setSpawnPos(): updating World Spawn {} -> {}", this.getSpawnPosAsString(), this.getSpawnPosAsString(spawnPos));
        }

        this.spawnPos = spawnPos;

        if (this.spawnPos.equals(new GlobalPos(ServerLevel.OVERWORLD, BlockPos.ZERO)))
        {
            Servux.LOGGER.warn("setSpawnPos(): Warning! Spawn pos was set to [{}]; please verify that this was intended", this.getSpawnPosAsString());
        }
    }

//    public int getSpawnChunkRadius()
//    {
//        if (this.spawnChunkRadius < 0)
//        {
//            this.spawnChunkRadius = 2;
//        }
//
//        return this.spawnChunkRadius;
//    }
//
//    public void setSpawnChunkRadius(int radius)
//    {
//        if (this.spawnChunkRadius != radius)
//        {
//            this.metadata.remove("spawnChunkRadius");
//            this.metadata.putInt("spawnChunkRadius", radius);
//            this.refreshSpawnMetadata = true;
//
//            Servux.debugLog("setSpawnPos(): updating Spawn Chunk Radius [{}] -> [{}]", this.spawnChunkRadius, radius);
//        }
//
//        this.spawnChunkRadius = radius;
//    }

    public boolean shouldRefreshSpawnMetadata() { return this.refreshSpawnMetadata; }

    public void setRefreshSpawnMetadataComplete()
    {
        this.refreshSpawnMetadata = false;
        Servux.debugLog("setRefreshSpawnMetadataComplete()");
    }

    public boolean shouldRefreshWeatherData() { return this.refreshWeatherData; }

    public void setRefreshWeatherDataComplete()
    {
        this.refreshWeatherData = false;
        //Servux.debugLog("setRefreshWeatherDataComplete()");
    }

    public long getWorldSeed()
    {
        return this.worldSeed;
    }

    public void setWorldSeed(long seed)
    {
        if (this.worldSeed != seed)
        {
            if (this.shareSeed.getValue())
            {
                this.metadata.remove("worldSeed");
                this.metadata.putLong("worldSeed", seed);
                this.refreshSpawnMetadata = true;
            }

            Servux.debugLog("setWorldSeed(): updating World Seed [{}] -> [{}]", this.worldSeed, seed);
        }

        this.worldSeed = seed;
    }

    public void checkWorldSeed(MinecraftServer server)
    {
        if (this.shareSeed.getValue())
        {
            ServerLevel world = server.overworld();

            if (world != null)
            {
                this.setWorldSeed(world.getSeed());
            }
        }
    }

    public boolean isLoggersEnabled() { return this.loggersEnabled.getValue(); }

    public boolean hasPermissionsForWeather(ServerPlayer player)
    {
        return Permissions.check(player, this.permNode + ".weather", this.weatherPermissionLevel.getValue());
    }

    public boolean hasPermissionsForSeed(ServerPlayer player)
    {
        return Permissions.check(player, this.permNode + ".seed", this.seedPermissionLevel.getValue());
    }

    public boolean hasPermissionsForLoggers(ServerPlayer player)
    {
        return Permissions.check(player, this.permNode + ".logger", this.loggerPermissionLevel.getValue());
    }

    public boolean hasPermissionsForLogger(ServerPlayer player, String type)
    {
        return Permissions.check(player, this.permNode + ".logger."+type, this.loggerPermissionLevel.getValue());
    }

    @Override
    public boolean hasPermission(ServerPlayer player)
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

    public static class BoolCallback implements IServuxSettingCallback<Boolean>
    {
        @Override
        public void onValueChanged(IServuxSetting<Boolean> setting, Boolean oldValue, Boolean value)
        {
            HudDataProvider.INSTANCE.resetLoggersFromConfig();
        }
    }

    public static class StringListCallback implements IServuxSettingCallback<List<String>>
    {
        @Override
        public void onValueChanged(IServuxSetting<List<String>> setting, List<String> oldValue, List<String> value)
        {
            HudDataProvider.INSTANCE.resetLoggersFromConfig();
        }
    }
}
