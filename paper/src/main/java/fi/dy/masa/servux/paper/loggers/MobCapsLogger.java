package fi.dy.masa.servux.paper.loggers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

/**
 * Paper-side mirror of Servux's {@code DataLoggerMobCaps} (Fabric side). Uses the exact same NMS
 * access pattern as Paper's own first-party {@code /paper mobcaps} command
 * ({@code io.papermc.paper.command.subcommands.MobcapsCommand}) - including
 * {@code NaturalSpawner.globalLimitForCategory(...)}, a Paper-added helper that replaces Fabric's
 * hand-rolled cap-limit math entirely.
 * <p>
 * Produces, per world, a {@link CompoundTag} matching the exact shape of Fabric's
 * {@code fi.dy.masa.servux.loggers.data.MobCapData} Codec: {@code cap_count} (int), {@code cap_data}
 * (a positional list of {@code {current, cap}} compounds - order matters, see {@link #CATEGORY_ORDER}),
 * plus a {@code WorldTick} (long) field added by Fabric's provider on top of the Codec shape.
 *
 * @see <a href="../../../../../../../../../src/main/java/fi/dy/masa/servux/loggers/DataLoggerMobCaps.java">DataLoggerMobCaps.java (Fabric reference)</a>
 * @see <a href="../../../../../../../../../src/main/java/fi/dy/masa/servux/loggers/data/MobCapData.java">MobCapData.java (Fabric reference)</a>
 */
public final class MobCapsLogger
{
    /**
     * Fixed order matching Fabric's {@code MobCapData.EntityCategory} enum declaration order -
     * {@code cap_data} is a plain positional list (no per-entry category name), so the client
     * decodes entries by index; this order must match exactly.
     */
    private static final MobCategory[] CATEGORY_ORDER = {
        MobCategory.MONSTER,
        MobCategory.CREATURE,
        MobCategory.AMBIENT,
        MobCategory.AXOLOTLS,
        MobCategory.UNDERGROUND_WATER_CREATURE,
        MobCategory.WATER_CREATURE,
        MobCategory.WATER_AMBIENT,
        MobCategory.MISC,
    };

    private MobCapsLogger()
    {
    }

    public static CompoundTag buildNbt()
    {
        CompoundTag nbt = new CompoundTag();

        for (World world : Bukkit.getWorlds())
        {
            ServerLevel level = ((CraftWorld) world).getHandle();
            NaturalSpawner.SpawnState state = level.getChunkSource().getLastSpawnState();

            if (state == null)
            {
                continue;
            }

            int spawnableChunks = state.getSpawnableChunkCount();

            if (spawnableChunks <= 0)
            {
                // Not loaded / no spawn data yet for this dimension.
                continue;
            }

            ListTag capData = new ListTag();

            for (MobCategory category : CATEGORY_ORDER)
            {
                int current = state.getMobCategoryCounts().getOrDefault(category, 0);
                int cap = NaturalSpawner.globalLimitForCategory(level, category, spawnableChunks);

                CompoundTag capEntry = new CompoundTag();
                capEntry.putInt("current", current);
                capEntry.putInt("cap", cap);
                capData.add(capEntry);
            }

            CompoundTag worldEntry = new CompoundTag();
            worldEntry.putInt("cap_count", CATEGORY_ORDER.length);
            worldEntry.put("cap_data", capData);
            worldEntry.putLong("WorldTick", level.getGameTime());

            nbt.put(world.getKey().toString(), worldEntry);
        }

        return nbt;
    }
}
