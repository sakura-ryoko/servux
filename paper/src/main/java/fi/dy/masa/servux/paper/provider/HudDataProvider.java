package fi.dy.masa.servux.paper.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.mojang.serialization.DataResult;

import fi.dy.masa.servux.paper.ServuxPaperConfig;
import fi.dy.masa.servux.paper.ServuxPaperReference;
import fi.dy.masa.servux.paper.loggers.MobCapsLogger;
import fi.dy.masa.servux.paper.loggers.TpsLogger;
import fi.dy.masa.servux.paper.network.PacketSplitter;
import fi.dy.masa.servux.paper.network.ServuxHudPacket;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Paper-side mirror of Servux's {@code HudDataProvider} (Fabric side): metadata handshake, spawn
 * data, world seed, weather ticks, TPS/mob-cap data loggers, and the recipe manager dump.
 * <p>
 * Field names must match the Fabric implementation exactly, since MiniHUD parses these NBT
 * compounds by key name.
 *
 * @see <a href="../../../../../../../../../../src/main/java/fi/dy/masa/servux/dataproviders/HudDataProvider.java">HudDataProvider.java (Fabric reference)</a>
 */
public class HudDataProvider
{
    public static final HudDataProvider INSTANCE = new HudDataProvider();

    public static final String CHANNEL_ID = "servux:hud_metadata";
    public static final int PROTOCOL_VERSION = 2;

    private static final String PERMISSION_SHARE_SEED = "servux.hud_data.share_seed";
    private static final String PERMISSION_SHARE_WEATHER = "servux.hud_data.share_weather_status";
    private static final String PERMISSION_LOGGERS = "servux.hud_data.loggers";
    private static final String PERMISSION_RECIPE_MANAGER = "servux.hud_data.recipe_manager";

    private final Map<UUID, Set<String>> loggerSubscriptions = new HashMap<>();

    private Plugin plugin;

    private HudDataProvider()
    {
    }

    public void init(Plugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Mirrors the fields set on {@code HudDataProvider.metadata} in the Fabric constructor,
     * plus the conditionally-included {@code Loggers} sub-compound and {@code worldSeed} field.
     */
    public CompoundTag buildMetadataNbt(Player player, Location spawn)
    {
        CompoundTag nbt = new CompoundTag();

        nbt.putString("name", "hud_data");
        nbt.putString("id", CHANNEL_ID);
        nbt.putInt("version", PROTOCOL_VERSION);
        nbt.putString("servux", ServuxPaperReference.modString());

        this.putSpawnFields(nbt, spawn);
        this.putSeedField(nbt, player, spawn.getWorld());
        this.putLoggersField(nbt);

        return nbt;
    }

    /** Mirrors the fields set in {@code HudDataProvider.refreshSpawnMetadata()}. */
    public CompoundTag buildSpawnNbt(Player player, Location spawn)
    {
        CompoundTag nbt = new CompoundTag();

        nbt.putString("id", CHANNEL_ID);
        nbt.putString("servux", ServuxPaperReference.modString());
        nbt.putInt("version", PROTOCOL_VERSION);

        this.putSpawnFields(nbt, spawn);
        this.putSeedField(nbt, player, spawn.getWorld());

        return nbt;
    }

    private void putSpawnFields(CompoundTag nbt, Location spawn)
    {
        World world = spawn.getWorld();
        String dimensionId = world != null ? world.getKey().toString() : "minecraft:overworld";

        nbt.putString("spawnDimension", dimensionId);
        nbt.putInt("spawnPosX", spawn.getBlockX());
        nbt.putInt("spawnPosY", spawn.getBlockY());
        nbt.putInt("spawnPosZ", spawn.getBlockZ());
    }

    /** Mirrors Fabric's `share_seed` toggle + `hasPermissionsForSeed` check. */
    private void putSeedField(CompoundTag nbt, Player player, World world)
    {
        if (world != null && ServuxPaperConfig.hudDataShareSeed() && player.hasPermission(PERMISSION_SHARE_SEED))
        {
            nbt.putLong("worldSeed", world.getSeed());
        }
    }

    /** Mirrors Fabric's `checkIfLoggersAreInitialized` metadata "Loggers" sub-compound. */
    private void putLoggersField(CompoundTag nbt)
    {
        if (!ServuxPaperConfig.hudDataLoggersEnabled())
        {
            return;
        }

        CompoundTag loggers = new CompoundTag();

        for (String name : ServuxPaperConfig.hudDataLoggersEnableList())
        {
            loggers.putBoolean(name, true);
        }

        nbt.put("Loggers", loggers);
    }

    /** Mirrors Fabric's `refreshWeatherData` - periodic broadcast, not a per-request response. */
    private CompoundTag buildWeatherNbt(World world)
    {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", CHANNEL_ID);
        nbt.putString("servux", ServuxPaperReference.modString());

        boolean isRaining = world.hasStorm();
        int rainTime = world.getWeatherDuration();

        if (isRaining && rainTime > 0)
        {
            nbt.putInt("SetRaining", rainTime);
            nbt.putBoolean("isRaining", true);
        }
        else
        {
            nbt.putBoolean("isRaining", false);
        }

        boolean isThundering = world.isThundering();
        int thunderTime = world.getThunderDuration();

        if (isThundering && thunderTime > 0)
        {
            nbt.putInt("SetThundering", thunderTime);
            nbt.putBoolean("isThundering", true);
        }
        else
        {
            nbt.putBoolean("isThundering", false);
        }

        int clearTime = world.getClearWeatherDuration();

        if (clearTime > 0)
        {
            nbt.putInt("SetClear", clearTime);
        }

        return nbt;
    }

    /** Mirrors Fabric's `refreshLoggers` - validates requested logger names against config + permission. */
    public void updateLoggerSubscription(Player player, CompoundTag requestNbt)
    {
        if (!ServuxPaperConfig.hudDataLoggersEnabled() || !player.hasPermission(PERMISSION_LOGGERS))
        {
            return;
        }

        Set<String> enabled = new HashSet<>();

        for (String key : requestNbt.keySet())
        {
            boolean want = requestNbt.getBooleanOr(key, false);

            if (want && ServuxPaperConfig.hudDataLoggersEnableList().contains(key))
            {
                enabled.add(key);
            }
        }

        UUID uuid = player.getUniqueId();

        if (enabled.isEmpty())
        {
            this.loggerSubscriptions.remove(uuid);
        }
        else
        {
            this.loggerSubscriptions.put(uuid, enabled);
        }
    }

    public void removeSubscriber(Player player)
    {
        this.loggerSubscriptions.remove(player.getUniqueId());
    }

    /** Periodic tick - schedule via Bukkit scheduler every `hud_data.update_interval` ticks. */
    public void tick()
    {
        this.tickWeather();
        this.tickLoggers();
    }

    private void tickWeather()
    {
        if (!ServuxPaperConfig.hudDataShareWeatherStatus())
        {
            return;
        }

        World world = Bukkit.getWorlds().get(0);
        CompoundTag nbt = this.buildWeatherNbt(world);

        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (player.hasPermission(PERMISSION_SHARE_WEATHER))
            {
                this.send(player, ServuxHudPacket.WeatherTick(nbt));
            }
        }
    }

    private void tickLoggers()
    {
        if (!ServuxPaperConfig.hudDataLoggersEnabled() || this.loggerSubscriptions.isEmpty())
        {
            return;
        }

        CompoundTag tpsNbt = null;
        CompoundTag mobCapsNbt = null;

        Iterator<Map.Entry<UUID, Set<String>>> iter = this.loggerSubscriptions.entrySet().iterator();

        while (iter.hasNext())
        {
            Map.Entry<UUID, Set<String>> entry = iter.next();
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player == null || !player.isOnline() || !player.hasPermission(PERMISSION_LOGGERS))
            {
                iter.remove();
                continue;
            }

            CompoundTag nbt = new CompoundTag();

            for (String key : entry.getValue())
            {
                if (key.equals("tps"))
                {
                    if (tpsNbt == null)
                    {
                        tpsNbt = TpsLogger.buildNbt();
                    }

                    nbt.put("tps", tpsNbt);
                }
                else if (key.equals("mob_caps"))
                {
                    if (mobCapsNbt == null)
                    {
                        mobCapsNbt = MobCapsLogger.buildNbt();
                    }

                    nbt.put("mob_caps", mobCapsNbt);
                }
            }

            if (!nbt.isEmpty())
            {
                this.send(player, ServuxHudPacket.DataLoggerTick(nbt));
            }
        }
    }

    /** Mirrors Fabric's `refreshRecipeManager` - sent via {@link PacketSplitter} since it can be large. */
    public void sendRecipeManager(Player player)
    {
        if (!player.hasPermission(PERMISSION_RECIPE_MANAGER))
        {
            return;
        }

        ServerLevel level = ((CraftWorld) player.getWorld()).getHandle();
        Collection<RecipeHolder<?>> recipes = level.recipeAccess().getRecipes();
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();

        for (RecipeHolder<?> holder : recipes)
        {
            DataResult<Tag> dr = Recipe.CODEC.encodeStart(NbtOps.INSTANCE, holder.value());

            if (dr.result().isPresent())
            {
                CompoundTag entry = new CompoundTag();
                entry.putString("id_reg", holder.id().registry().toString());
                entry.putString("id_value", holder.id().identifier().toString());
                entry.put("recipe", dr.result().get());
                list.add(entry);
            }
        }

        nbt.put("RecipeManager", list);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeNbt(nbt);

        PacketSplitter.send(CHANNEL_ID, this.plugin, player, ByteBufUtil.getBytes(buf),
                             bytes -> ServuxHudPacket.ResponseS2CData(bytes).toBytes());
    }

    private void send(Player player, ServuxHudPacket packet)
    {
        player.sendPluginMessage(this.plugin, CHANNEL_ID, packet.toBytes());
    }
}
