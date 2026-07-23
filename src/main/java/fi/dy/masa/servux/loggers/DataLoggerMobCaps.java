package fi.dy.masa.servux.loggers;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

import fi.dy.masa.servux.loggers.data.MobCapData;
import fi.dy.masa.servux.util.MathUtils;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.util.DataOps;

public class DataLoggerMobCaps extends DataLoggerBase<CompoundData>
{
    public DataLoggerMobCaps(DataLogger type)
    {
        super(type);
    }

    @Override
    public CompoundData getResult(MinecraftServer server)
    {
        CompoundData nbt = new CompoundData();

        for (ServerLevel world : server.getAllLevels())
        {
            String dimKey = world.dimension().identifier().toString();
            MobCapData mobCapData = new MobCapData();
            MobCapData.Cap[] data = MobCapData.createCapArray();
            NaturalSpawner.SpawnState info = world.getChunkSource().getLastSpawnState();

            if (info != null)
            {
                // Fix the math
//                int spawnableChunks = world.getChunkSource().chunkMap.getDistanceManager().getNaturalSpawnChunkCount();
//                int divisor = 17 * 17;
                int spawnableChunks = info.getSpawnableChunkCount();
                int divisor = NaturalSpawner.MAGIC_NUMBER;
                long worldTime = world.getGameTime();

                if (spawnableChunks <= 0)
                {
//                    Servux.debugLog("DataLoggerMobCaps#getResult(): Skipping Dimension: [{}] (No loaded chunks)", dimKey);
                    continue;       // Not loaded
                }

                for (Object2IntMap.Entry<MobCategory> entry : info.getMobCategoryCounts().object2IntEntrySet())
                {
                    MobCapData.EntityCategory category = MobCapData.EntityCategory.fromVanillaCategory(entry.getKey());

                    final int vanillaCap = entry.getKey().getMaxInstancesPerChunk();
                    int current = entry.getIntValue();
                    int capacity = MathUtils.clamp(entry.getKey().getMaxInstancesPerChunk() * (spawnableChunks / divisor), 0, vanillaCap);

                    data[category.ordinal()].setCurrentAndCap(current, capacity);

                    for (MobCapData.EntityCategory type : MobCapData.EntityCategory.values())
                    {
                        MobCapData.Cap cap = data[type.ordinal()];
                        mobCapData.setCurrentAndCapValues(type, cap.getCurrent(), cap.getCap(), worldTime);
                    }
                }

                try
                {
                    CompoundData nbtEntry = (CompoundData) MobCapData.CODEC.encodeStart(world.registryAccess().createSerializationContext(DataOps.INSTANCE), mobCapData).getPartialOrThrow();
                    nbtEntry.putLong("WorldTick", worldTime);
                    nbt.put(dimKey, nbtEntry);
                }
                catch (Exception ignored) { }

            }
        }

        return nbt;
    }
}
