package fi.dy.masa.servux.dataproviders;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import me.lucko.fabric.api.permissions.v0.Permissions;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.GameRules;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.*;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;

public class HudDataProvider extends DataProviderBase
{
    public static final HudDataProvider INSTANCE = new HudDataProvider();
    protected final static ServuxHudHandler<ServuxHudPacket.Payload> HANDLER = ServuxHudHandler.getInstance();
    protected final NbtCompound metadata = new NbtCompound();
    protected ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
    protected ServuxIntSetting updateInterval = new ServuxIntSetting(this, "update_interval", 40, 1200, 1);
    protected ServuxBoolSetting shareWeatherStatus = new ServuxBoolSetting(this, "share_weather_status", false);
    protected ServuxIntSetting weatherPermissionLevel = new ServuxIntSetting(this, "weather_permission_level", 0, 4, 0);
    protected ServuxBoolSetting shareSeed = new ServuxBoolSetting(this, "share_seed", false);
    protected ServuxIntSetting seedPermissionLevel = new ServuxIntSetting(this, "seed_permission_level", 2, 4, 0);
    protected List<IServuxSetting<?>> settings = List.of(this.permissionLevel, this.updateInterval, this.shareWeatherStatus, this.weatherPermissionLevel, this.shareSeed, this.seedPermissionLevel);

    private BlockPos spawnPos = BlockPos.ORIGIN;
    private int spawnChunkRadius = -1;
    private long worldSeed = 0;
    private int weatherTime = -1;
    private boolean isRaining;
    private boolean isThundering;
    private long lastTick;
    private long lastWeatherTick;
    private boolean refreshSpawnMetadata;
    private boolean refreshWeatherData;

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
        this.metadata.putInt("spawnPosX", this.getSpawnPos().getX());
        this.metadata.putInt("spawnPosY", this.getSpawnPos().getY());
        this.metadata.putInt("spawnPosZ", this.getSpawnPos().getZ());
        this.metadata.putInt("spawnChunkRadius", this.getSpawnChunkRadius());
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
    public boolean shouldTick()
    {
        return this.enabled;
    }

    @Override
    public void tick(MinecraftServer server, int tickCounter, Profiler profiler)
    {
        if ((tickCounter % this.updateInterval.getValue()) == 0)
        {
            profiler.push(this.getName());
            List<ServerPlayerEntity> playerList = server.getPlayerManager().getPlayerList();
            this.lastTick = tickCounter;

            int radius = this.getSpawnChunkRadius();
            int rule = server.getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS);
            if (radius != rule)
            {
                this.setSpawnChunkRadius(rule);
            }
            if (this.worldSeed == 0)
            {
                this.checkWorldSeed(server);
            }
            else if (this.shareSeed.getValue() == false)
            {
                this.setWorldSeed(0);
            }

            profiler.swap(this.getName() + "_players");
            for (ServerPlayerEntity player : playerList)
            {
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
    }

    public void tickWeather(int clearTime, int rainTime, boolean isThunder)
    {
        if (rainTime > 1)
        {
            if (isThunder)
            {
                this.isThundering = true;
                this.isRaining = false;
            }
            else
            {
                this.isThundering = false;
                this.isRaining = true;
            }

            this.weatherTime = rainTime;

            if ((this.lastTick - this.lastWeatherTick) > this.getTickInterval())
            {
                // Don't spam players with weather ticks
                this.refreshWeatherData = true;
            }
        }
        else if (clearTime > 0 && (this.isRaining || this.isThundering))
        {
            this.isThundering = false;
            this.isRaining = false;
            this.weatherTime = clearTime;
            this.refreshWeatherData = true;
        }
        else
        {
            this.weatherTime = clearTime;

            if ((this.lastTick - this.lastWeatherTick) > (this.getTickInterval() * 4))
            {
                // Don't spam players with weather ticks
                this.refreshWeatherData = true;
            }
        }
    }

    public void sendMetadata(ServerPlayerEntity player)
    {
        if (this.hasPermission(player) == false)
        {
            // No Permission
            Servux.debugLog("hud_service: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(this.metadata);

        if (this.hasPermissionsForSeed(player) == false && nbt.contains("worldSeed"))
        {
            nbt.remove("worldSeed");
        }

        Servux.debugLog("hudDataChannel: sendMetadata to player {}", player.getName().getLiteralString());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.networkHandler != null)
        {
            HANDLER.sendPlayPayload(player.networkHandler, new ServuxHudPacket.Payload(ServuxHudPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxHudPacket.Payload(ServuxHudPacket.MetadataResponse(this.metadata)));
        }

        this.refreshWeatherData(player, null);
    }

    public void onPacketFailure(ServerPlayerEntity player)
    {
        // Do something when packets fail, if required
    }

    public void refreshSpawnMetadata(ServerPlayerEntity player, @Nullable NbtCompound data)
    {
        NbtCompound nbt = new NbtCompound();
        BlockPos spawnPos = HudDataProvider.INSTANCE.getSpawnPos();

        nbt.putString("id", getNetworkChannel().toString());
        nbt.putString("servux", Reference.MOD_STRING);
        nbt.putInt("spawnPosX", spawnPos.getX());
        nbt.putInt("spawnPosY", spawnPos.getY());
        nbt.putInt("spawnPosZ", spawnPos.getZ());
        nbt.putInt("spawnChunkRadius", HudDataProvider.INSTANCE.getSpawnChunkRadius());

        if (this.shareSeed.getValue() && this.hasPermissionsForSeed(player))
        {
            Servux.debugLog("refreshSpawnMetadata() player [{}] has seedPermissions.", player.getName().getLiteralString());
            nbt.putLong("worldSeed", this.worldSeed);
        }
        else
        {
            Servux.debugLog("refreshSpawnMetadata() player [{}] does not have seedPermissions.", player.getName().getLiteralString());
        }

        HANDLER.encodeServerData(player, ServuxHudPacket.SpawnResponse(nbt));
    }

    public void refreshWeatherData(ServerPlayerEntity player, @Nullable NbtCompound data)
    {
        NbtCompound nbt = new NbtCompound();

        if (this.hasPermissionsForWeather(player) == false)
        {
            return;
        }
        nbt.putString("id", getNetworkChannel().toString());
        nbt.putString("servux", Reference.MOD_STRING);

        if (this.isRaining)
        {
            nbt.putInt("SetRaining", this.weatherTime);
        }
        else if (this.isThundering)
        {
            nbt.putInt("SetThundering", this.weatherTime);
        }
        else
        {
            nbt.putInt("SetClear", this.weatherTime);
        }

        HANDLER.encodeServerData(player, ServuxHudPacket.WeatherTick(nbt));
    }

    public void refreshRecipeManager(ServerPlayerEntity player, @Nullable NbtCompound data)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        ServerWorld world = player.getServerWorld();
        Collection<RecipeEntry<?>> recipes = world.getRecipeManager().values();
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();

        // This is ugly
        recipes.forEach((recipeEntry ->
        {
            RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), world.getRegistryManager());
            RecipeEntry.PACKET_CODEC.encode(buf, recipeEntry);
            list.add(new NbtByteArray(buf.readByteArray()));
        }));

        nbt.put("RecipeManager", list);

        // Use Packet Splitter
        HANDLER.sendPlayPayload(player, new ServuxHudPacket.Payload(ServuxHudPacket.ResponseS2CStart(nbt)));
    }

    public BlockPos getSpawnPos()
    {
        if (this.spawnPos == null)
        {
            this.setSpawnPos(BlockPos.ORIGIN);
        }

        return this.spawnPos;
    }

    public void setSpawnPos(BlockPos spawnPos)
    {
        if (this.spawnPos.equals(spawnPos) == false)
        {
            this.metadata.remove("spawnPosX");
            this.metadata.remove("spawnPosY");
            this.metadata.remove("spawnPosZ");
            this.metadata.putInt("spawnPosX", spawnPos.getX());
            this.metadata.putInt("spawnPosY", spawnPos.getY());
            this.metadata.putInt("spawnPosZ", spawnPos.getZ());
            this.refreshSpawnMetadata = true;

            Servux.debugLog("setSpawnPos(): updating World Spawn [{}] -> [{}]", this.spawnPos.toShortString(), spawnPos.toShortString());
        }

        this.spawnPos = spawnPos;
    }

    public int getSpawnChunkRadius()
    {
        if (this.spawnChunkRadius < 0)
        {
            this.spawnChunkRadius = 2;
        }

        return this.spawnChunkRadius;
    }

    public void setSpawnChunkRadius(int radius)
    {
        if (this.spawnChunkRadius != radius)
        {
            this.metadata.remove("spawnChunkRadius");
            this.metadata.putInt("spawnChunkRadius", radius);
            this.refreshSpawnMetadata = true;

            Servux.debugLog("setSpawnPos(): updating Spawn Chunk Radius [{}] -> [{}]", this.spawnChunkRadius, radius);
        }

        this.spawnChunkRadius = radius;
    }

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
        Servux.debugLog("setRefreshWeatherDataComplete()");
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
            ServerWorld world = server.getOverworld();

            if (world != null)
            {
                this.setWorldSeed(world.getSeed());
            }
        }
    }

    public boolean hasPermissionsForWeather(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode + ".weather", this.weatherPermissionLevel.getValue());
    }

    public boolean hasPermissionsForSeed(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode + ".seed", this.seedPermissionLevel.getValue());
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
