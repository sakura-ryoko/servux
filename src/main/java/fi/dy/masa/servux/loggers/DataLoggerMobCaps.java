package fi.dy.masa.servux.loggers;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import com.mojang.serialization.Codec;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;

import fi.dy.masa.servux.loggers.data.MobCapData;

public class DataLoggerMobCaps extends DataLoggerBase<NbtCompound>
{
    public static final Codec<NbtCompound> CODEC = NbtCompound.CODEC;

    public DataLoggerMobCaps(DataLogger type)
    {
        super(type);
    }

    @Override
    public NbtCompound getResult(MinecraftServer server)
    {
        NbtCompound nbt = new NbtCompound();

        for (ServerWorld world : server.getWorlds())
        {
            String dimKey = world.getRegistryKey().getValue().toString();
            MobCapData mobCapData = new MobCapData();
            MobCapData.Cap[] data = MobCapData.createCapArray();
            SpawnHelper.Info info = world.getChunkManager().getSpawnInfo();

            if (info != null)
            {
                int spawnableChunks = world.getChunkManager().chunkLoadingManager.getTicketManager().getTickedChunkCount();
                int divisor = 17 * 17;
                long worldTime = world.getTime();

                if (spawnableChunks <= 0)
                {
//                    Servux.debugLog("DataLoggerMobCaps#getResult(): Skipping Dimension: [{}] (No loaded chunks)", dimKey);
                    continue;       // Not loaded
                }

                for (Object2IntMap.Entry<SpawnGroup> entry : info.getGroupToCount().object2IntEntrySet())
                {
                    MobCapData.EntityCategory category = MobCapData.EntityCategory.fromVanillaCategory(entry.getKey());

                    int current = entry.getIntValue();
                    int capacity = entry.getKey().getCapacity() * spawnableChunks / divisor;
                    data[category.ordinal()].setCurrentAndCap(current, capacity);

                    for (MobCapData.EntityCategory type : MobCapData.EntityCategory.values())
                    {
                        MobCapData.Cap cap = data[type.ordinal()];
                        mobCapData.setCurrentAndCapValues(type, cap.getCurrent(), cap.getCap(), worldTime);
                    }
                }

                try
                {
                    NbtCompound nbtEntry = (NbtCompound) MobCapData.CODEC.encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), mobCapData).getPartialOrThrow();
                    nbtEntry.putLong("WorldTick", worldTime);
                    nbt.put(dimKey, nbtEntry);
                }
                catch (Exception ignored) { }

            }
        }

        return nbt;
    }
}
