package fi.dy.masa.servux.paper.provider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fi.dy.masa.servux.paper.ServuxPaperConfig;
import fi.dy.masa.servux.paper.ServuxPaperReference;
import fi.dy.masa.servux.paper.network.PacketSplitter;
import fi.dy.masa.servux.paper.network.ServuxStructuresPacket;
import fi.dy.masa.servux.paper.util.Timeout;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Minimal Paper-side mirror of Servux's {@code StructureDataProvider} (Fabric side): tracks which
 * players want structure bounding-box sync, sends an initial burst of nearby structures on
 * registration, and periodically re-sends structures for chunks whose tracked data has expired -
 * including immediately queuing a refresh as soon as a player is sent a chunk that has structure
 * references (the Paper-native replacement for Fabric's {@code MixinServerChunkLoadingManager},
 * driven by {@code StructuresChannel}'s {@code PlayerChunkLoadEvent} handler).
 * <p>
 * Simplifications vs. Fabric: no structure blacklist/whitelist entries are pre-populated by
 * default (matches Fabric's default-disabled blacklist).
 *
 * @see <a href="../../../../../../../../../../src/main/java/fi/dy/masa/servux/dataproviders/StructureDataProvider.java">StructureDataProvider.java (Fabric reference)</a>
 */
public class StructureDataProvider
{
    public static final StructureDataProvider INSTANCE = new StructureDataProvider();

    public static final String CHANNEL_ID = "servux:structures";
    public static final int PROTOCOL_VERSION = 2;

    private final Map<UUID, World> registeredWorld = new HashMap<>();
    private final Map<UUID, Map<ChunkPos, Timeout>> timeouts = new HashMap<>();

    private Plugin plugin;

    private StructureDataProvider()
    {
    }

    public void init(Plugin plugin)
    {
        this.plugin = plugin;
    }

    public boolean isRegistered(Player player)
    {
        return this.registeredWorld.containsKey(player.getUniqueId());
    }

    public void registerFresh(Player player)
    {
        this.unregister(player);

        UUID uuid = player.getUniqueId();
        this.registeredWorld.put(uuid, player.getWorld());

        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", "structure_bounding_boxes");
        nbt.putString("id", CHANNEL_ID);
        nbt.putInt("version", PROTOCOL_VERSION);
        nbt.putString("servux", ServuxPaperReference.modString());
        nbt.putInt("timeout", ServuxPaperConfig.structuresTimeout());

        player.sendPluginMessage(this.plugin, CHANNEL_ID, ServuxStructuresPacket.Metadata(nbt).toBytes());

        int radius = Bukkit.getViewDistance() + 2;
        this.initialSync(player, radius);
    }

    public void unregister(Player player)
    {
        UUID uuid = player.getUniqueId();
        this.registeredWorld.remove(uuid);
        this.timeouts.remove(uuid);
    }

    /**
     * Called from {@code StructuresChannel}'s {@code PlayerChunkLoadEvent} handler - queues an
     * immediate refresh for the chunk if it has structure references, mirroring Fabric's
     * {@code onStartedWatchingChunk}.
     */
    public void onStartedWatchingChunk(Player player, Chunk chunk)
    {
        UUID uuid = player.getUniqueId();

        if (!this.registeredWorld.containsKey(uuid))
        {
            return;
        }

        ServerLevel level = level(chunk.getWorld());
        ChunkPos pos = new ChunkPos(chunk.getX(), chunk.getZ());

        if (this.chunkHasStructureReferences(pos.x(), pos.z(), level))
        {
            Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, u -> new HashMap<>());
            int tick = currentTick();

            // Already-expired timeout so the next periodic tick will refresh/send this chunk.
            map.computeIfAbsent(pos, p -> new Timeout(tick - ServuxPaperConfig.structuresTimeout()));
        }
    }

    /** Periodic refresh pass - schedule via Bukkit scheduler every {@link #UPDATE_INTERVAL_TICKS}. */
    public void tick()
    {
        if (this.registeredWorld.isEmpty())
        {
            return;
        }

        int tick = currentTick();
        Iterator<UUID> iter = this.registeredWorld.keySet().iterator();

        while (iter.hasNext())
        {
            UUID uuid = iter.next();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline())
            {
                iter.remove();
                this.timeouts.remove(uuid);
                continue;
            }

            World lastWorld = this.registeredWorld.get(uuid);

            if (!lastWorld.equals(player.getWorld()))
            {
                this.timeouts.remove(uuid);
                this.registeredWorld.put(uuid, player.getWorld());
            }

            this.refreshTrackedChunks(player, tick);
        }
    }

    private void initialSync(Player player, int radius)
    {
        ServerLevel level = level(player.getWorld());
        ChunkPos center = toChunkPos(player);
        Map<Structure, LongSet> references = new HashMap<>();

        for (int cx = center.x() - radius; cx <= center.x() + radius; cx++)
        {
            for (int cz = center.z() - radius; cz <= center.z() + radius; cz++)
            {
                this.getStructureReferencesFromChunk(cx, cz, level, references);
            }
        }

        this.refreshTimeouts(player.getUniqueId(), references, currentTick());
        this.sendStructures(player, level, references);
    }

    private void refreshTrackedChunks(Player player, int tick)
    {
        Map<ChunkPos, Timeout> map = this.timeouts.get(player.getUniqueId());

        if (map == null || map.isEmpty())
        {
            return;
        }

        ServerLevel level = level(player.getWorld());
        ChunkPos center = toChunkPos(player);
        int retainDistance = Bukkit.getViewDistance() + 2;
        Map<Structure, LongSet> references = new HashMap<>();
        boolean any = false;

        Iterator<Map.Entry<ChunkPos, Timeout>> iter = map.entrySet().iterator();

        while (iter.hasNext())
        {
            Map.Entry<ChunkPos, Timeout> entry = iter.next();
            ChunkPos pos = entry.getKey();

            if (this.isOutOfRange(pos, center, retainDistance))
            {
                iter.remove();
                continue;
            }

            Timeout timeout = entry.getValue();

            if (timeout.needsUpdate(tick, ServuxPaperConfig.structuresTimeout()))
            {
                this.getStructureReferencesFromChunk(pos.x(), pos.z(), level, references);
                timeout.setLastSync(tick);
                any = true;
            }
        }

        if (any && !references.isEmpty())
        {
            this.sendStructures(player, level, references);
        }
    }

    private void refreshTimeouts(UUID uuid, Map<Structure, LongSet> references, int tick)
    {
        Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, u -> new HashMap<>());

        for (LongSet chunks : references.values())
        {
            LongIterator iter = chunks.iterator();

            while (iter.hasNext())
            {
                ChunkPos pos = ChunkPos.unpack(iter.nextLong());
                map.computeIfAbsent(pos, p -> new Timeout(tick)).setLastSync(tick);
            }
        }
    }

    private void sendStructures(Player player, ServerLevel level, Map<Structure, LongSet> references)
    {
        Map<ChunkPos, StructureStart> starts = this.getStructureStartsFromReferences(level, references);

        if (starts.isEmpty())
        {
            return;
        }

        ListTag list = this.getStructureList(starts, level);

        if (list.isEmpty())
        {
            return;
        }

        CompoundTag nbt = new CompoundTag();
        nbt.put("Structures", list);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeNbt(nbt);

        PacketSplitter.send(CHANNEL_ID, this.plugin, player, ByteBufUtil.getBytes(buf),
                             bytes -> ServuxStructuresPacket.StructureDataFragment(bytes).toBytes());
    }

    private ListTag getStructureList(Map<ChunkPos, StructureStart> structures, ServerLevel level)
    {
        ListTag list = new ListTag();
        StructurePieceSerializationContext ctx = StructurePieceSerializationContext.fromLevel(level);

        for (Map.Entry<ChunkPos, StructureStart> entry : structures.entrySet())
        {
            StructureStart start = entry.getValue();
            Structure structure = start.getStructure();

            if (structure == null)
            {
                continue;
            }

            Identifier structureType = BuiltInRegistries.STRUCTURE_TYPE.getKey(structure.type());

            if (structureType == null)
            {
                continue;
            }

            boolean expandBox = structure.terrainAdaptation() != TerrainAdjustment.NONE;
            ChunkPos pos = entry.getKey();

            if (!this.shouldSendStructure(structureType))
            {
                continue;
            }

            CompoundTag nbt = start.createTag(ctx, pos);
            nbt.putBoolean("ExpandBox", expandBox);
            list.add(nbt);
        }

        return list;
    }

    /** Mirrors Fabric's {@code shouldSendStructure} - blacklist/whitelist filtering from config. */
    private boolean shouldSendStructure(Identifier identifier)
    {
        if (ServuxPaperConfig.structuresWhitelistEnabled())
        {
            return ServuxPaperConfig.structuresWhitelist().contains(identifier.toString());
        }

        if (ServuxPaperConfig.structuresBlacklistEnabled())
        {
            return !ServuxPaperConfig.structuresBlacklist().contains(identifier.toString());
        }

        return true;
    }

    private Map<ChunkPos, StructureStart> getStructureStartsFromReferences(ServerLevel level, Map<Structure, LongSet> references)
    {
        Map<ChunkPos, StructureStart> starts = new HashMap<>();

        for (Map.Entry<Structure, LongSet> entry : references.entrySet())
        {
            Structure structure = entry.getKey();
            LongIterator iter = entry.getValue().iterator();

            while (iter.hasNext())
            {
                ChunkPos pos = ChunkPos.unpack(iter.nextLong());

                if (!level.hasChunk(pos.x(), pos.z()))
                {
                    continue;
                }

                ChunkAccess chunk = level.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_REFERENCES, false);

                if (chunk == null)
                {
                    continue;
                }

                StructureStart start = chunk.getStartForStructure(structure);

                if (start != null)
                {
                    starts.put(pos, start);
                }
            }
        }

        return starts;
    }

    private void getStructureReferencesFromChunk(int chunkX, int chunkZ, ServerLevel level, Map<Structure, LongSet> references)
    {
        if (!level.hasChunk(chunkX, chunkZ))
        {
            return;
        }

        ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_REFERENCES, false);

        if (chunk == null)
        {
            return;
        }

        for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet())
        {
            LongSet startChunks = entry.getValue();

            if (startChunks.isEmpty())
            {
                continue;
            }

            references.merge(entry.getKey(), startChunks, (oldSet, newSet) -> {
                LongOpenHashSet merged = new LongOpenHashSet(oldSet);
                merged.addAll(newSet);
                return merged;
            });
        }
    }

    private boolean chunkHasStructureReferences(int chunkX, int chunkZ, ServerLevel level)
    {
        if (!level.hasChunk(chunkX, chunkZ))
        {
            return false;
        }

        ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_REFERENCES, false);

        if (chunk == null)
        {
            return false;
        }

        for (LongSet set : chunk.getAllReferences().values())
        {
            if (!set.isEmpty())
            {
                return true;
            }
        }

        return false;
    }

    private boolean isOutOfRange(ChunkPos pos, ChunkPos center, int radius)
    {
        return Math.abs(pos.x() - center.x()) > radius || Math.abs(pos.z() - center.z()) > radius;
    }

    private ChunkPos toChunkPos(Player player)
    {
        Chunk chunk = player.getLocation().getChunk();
        return new ChunkPos(chunk.getX(), chunk.getZ());
    }

    private static ServerLevel level(World world)
    {
        return ((CraftWorld) world).getHandle();
    }

    private static int currentTick()
    {
        return level(Bukkit.getWorlds().get(0)).getServer().getTickCount();
    }
}
