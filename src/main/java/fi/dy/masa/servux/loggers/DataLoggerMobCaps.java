package fi.dy.masa.servux.loggers;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import fi.dy.masa.servux.loggers.data.MobCapData;
import fi.dy.masa.servux.util.MathUtils;

public class DataLoggerMobCaps extends DataLoggerBase<CompoundTag>
{
    public static final Codec<CompoundTag> CODEC = CompoundTag.CODEC;

    public DataLoggerMobCaps(DataLogger type)
    {
        super(type);
    }

    @Override
    public CompoundTag getResult(MinecraftServer server)
    {
        CompoundTag nbt = new CompoundTag();

        for (ServerLevel world : server.getAllLevels())
        {
            String dimKey = world.dimension().identifier().toString();
            MobCapData mobCapData = new MobCapData();
            MobCapData.Cap[] data = MobCapData.createCapArray();
            NaturalSpawner.SpawnState info = world.getChunkSource().getLastSpawnState();

            if (info != null)
            {
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
                    CompoundTag nbtEntry = (CompoundTag) MobCapData.CODEC.encodeStart(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), mobCapData).getPartialOrThrow();
                    nbtEntry.putLong("WorldTick", worldTime);
                    nbt.put(dimKey, nbtEntry);
                }
                catch (Exception ignored) { }

            }
        }

        return nbt;
    }
}
