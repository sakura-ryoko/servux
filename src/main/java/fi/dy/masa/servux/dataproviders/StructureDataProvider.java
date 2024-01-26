package fi.dy.masa.servux.dataproviders;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.event.ServuxPayloadHandler;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
import fi.dy.masa.servux.network.legacy.StructureDataPacketHandler;
import fi.dy.masa.servux.util.PlayerDimensionPosition;
import fi.dy.masa.servux.util.Timeout;

public class StructureDataProvider extends DataProviderBase
{
    public static final StructureDataProvider INSTANCE = new StructureDataProvider();

    protected final Map<UUID, PlayerDimensionPosition> registeredPlayers = new HashMap<>();
    protected final Map<UUID, Map<ChunkPos, Timeout>> timeouts = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected int timeout = 30 * 20;
    protected int updateInterval = 40;
    protected int retainDistance;

    protected StructureDataProvider()
    {
        super("structure_bounding_boxes",
              "", StructureDataPacketHandler.PROTOCOL_VERSION,
              "Structure Bounding Boxes data for structures such as Witch Huts, Ocean Monuments, Nether Fortresses etc.");

        this.metadata.putString("id", "<>");
        this.metadata.putInt("timeout", this.timeout);
        this.metadata.putInt("version", StructureDataPacketHandler.PROTOCOL_VERSION);
    }

    @Override
    public boolean shouldTick()
    {
        return true;
    }

    /*
    @Override
    public IPluginChannelHandler getPacketHandler()
    {
        return StructureDataPacketHandler.INSTANCE;
    }
     */

    @Override
    public void tick(MinecraftServer server, int tickCounter)
    {
        if ((tickCounter % this.updateInterval) == 0)
        {
            if (!this.registeredPlayers.isEmpty())
            {
                //Servux.printDebug("=======================\n");
                //Servux.printDebug("tick: %d - %s\n", tickCounter, this.isEnabled());
                this.retainDistance = server.getPlayerManager().getViewDistance() + 2;
                Iterator<UUID> uuidIter = this.registeredPlayers.keySet().iterator();

                while (uuidIter.hasNext())
                {
                    UUID uuid = uuidIter.next();
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);

                    if (player != null)
                    {
                        this.checkForDimensionChange(player);
                        this.refreshTrackedChunks(player, tickCounter);
                    }
                    else
                    {
                        this.timeouts.remove(uuid);
                        uuidIter.remove();
                    }
                }
            }
        }
    }

    public void onStartedWatchingChunk(ServerPlayerEntity player, WorldChunk chunk)
    {
        UUID uuid = player.getUuid();

        if (this.registeredPlayers.containsKey(uuid))
        {
            this.addChunkTimeoutIfHasReferences(uuid, chunk, player.getServer().getTicks());
        }
    }

    public boolean register(ServerPlayerEntity player)
    {
        Servux.printDebug("register");
        boolean registered = false;
        UUID uuid = player.getUuid();

        if (!this.registeredPlayers.containsKey(uuid))
        {
            // #FIXME ?
            Servux.printDebug("StructureDataProvider#register(): yeeting packet.");
            ((ServuxPayloadHandler) ServuxPayloadHandler.getInstance()).encodeServuxPayloadWithType(StructureDataPacketHandler.PACKET_S2C_METADATA, this.metadata, player);

            this.registeredPlayers.put(uuid, new PlayerDimensionPosition(player));
            int tickCounter = player.getServer().getTicks();
            this.initialSyncStructuresToPlayerWithinRange(player, player.getServer().getPlayerManager().getViewDistance(), tickCounter);

            registered = true;
        }

        return registered;
    }

    public boolean unregister(ServerPlayerEntity player)
    {
        Servux.printDebug("unregister");
        return this.registeredPlayers.remove(player.getUuid()) != null;
    }

    protected void initialSyncStructuresToPlayerWithinRange(ServerPlayerEntity player, int chunkRadius, int tickCounter)
    {
        UUID uuid = player.getUuid();
        ChunkPos center = player.getWatchedSection().toChunkPos();
        Map<Structure, LongSet> references =
                this.getStructureReferencesWithinRange(player.getServerWorld(), center, chunkRadius);

        this.timeouts.remove(uuid);
        this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);

        // System.out.printf("initialSyncStructuresToPlayerWithinRange: references: %d\n", references.size());
        this.sendStructures(player, references, tickCounter);
    }

    protected void addChunkTimeoutIfHasReferences(final UUID uuid, WorldChunk chunk, final int tickCounter)
    {
        final ChunkPos pos = chunk.getPos();

        if (this.chunkHasStructureReferences(pos.x, pos.z, chunk.getWorld()))
        {
            final Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, (u) -> new HashMap<>());
            final int timeout = this.timeout;

            Servux.printDebug("addChunkTimeoutIfHasReferences: %s", pos);
            // Set the timeout so it's already expired and will cause the chunk to be sent on the next update tick
            map.computeIfAbsent(pos, (p) -> new Timeout(tickCounter - timeout));
        }
    }

    protected void checkForDimensionChange(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        PlayerDimensionPosition playerPos = this.registeredPlayers.get(uuid);

        if (playerPos == null || playerPos.dimensionChanged(player))
        {
            this.timeouts.remove(uuid);
            this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);
        }
    }

    protected void addOrRefreshTimeouts(final UUID uuid,
                                        final Map<Structure, LongSet> references,
                                        final int tickCounter)
    {
        // System.out.printf("addOrRefreshTimeouts: references: %d\n", references.size());
        Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, (u) -> new HashMap<>());

        for (LongSet chunks : references.values())
        {
            for (Long chunkPosLong : chunks)
            {
                final ChunkPos pos = new ChunkPos(chunkPosLong);
                map.computeIfAbsent(pos, (p) -> new Timeout(tickCounter)).setLastSync(tickCounter);
            }
        }
    }

    protected void refreshTrackedChunks(ServerPlayerEntity player, int tickCounter)
    {
        UUID uuid = player.getUuid();
        Map<ChunkPos, Timeout> map = this.timeouts.get(uuid);

        if (map != null)
        {
            Servux.printDebug("refreshTrackedChunks: timeouts: %d", map.size());
            this.sendAndRefreshExpiredStructures(player, map, tickCounter);
        }
    }

    protected boolean isOutOfRange(ChunkPos pos, ChunkPos center)
    {
        int chunkRadius = this.retainDistance;

        return Math.abs(pos.x - center.x) > chunkRadius ||
               Math.abs(pos.z - center.z) > chunkRadius;
    }

    protected void sendAndRefreshExpiredStructures(ServerPlayerEntity player, Map<ChunkPos, Timeout> map, int tickCounter)
    {
        Set<ChunkPos> positionsToUpdate = new HashSet<>();

        for (Map.Entry<ChunkPos, Timeout> entry : map.entrySet())
        {
            Timeout timeout = entry.getValue();

            if (timeout.needsUpdate(tickCounter, this.timeout))
            {
                positionsToUpdate.add(entry.getKey());
            }
        }

        if (!positionsToUpdate.isEmpty())
        {
            ServerWorld world = player.getServerWorld();
            ChunkPos center = player.getWatchedSection().toChunkPos();
            Map<Structure, LongSet> references = new HashMap<>();

            for (ChunkPos pos : positionsToUpdate)
            {
                if (this.isOutOfRange(pos, center))
                {
                    map.remove(pos);
                }
                else
                {
                    this.getStructureReferencesFromChunk(pos.x, pos.z, world, references);

                    Timeout timeout = map.get(pos);

                    if (timeout != null)
                    {
                        timeout.setLastSync(tickCounter);
                    }
                }
            }

            Servux.printDebug("sendAndRefreshExpiredStructures: positionsToUpdate: %d -> references: %d, to: %d", positionsToUpdate.size(), references.size(), this.timeout);

            if (!references.isEmpty())
            {
                this.sendStructures(player, references, tickCounter);
            }
        }
    }

    protected void getStructureReferencesFromChunk(int chunkX, int chunkZ, World world, Map<Structure, LongSet> references)
    {
        if (!world.isChunkLoaded(chunkX, chunkZ))
        {
            return;
        }

        Chunk chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS, false);

        if (chunk == null)
        {
            return;
        }

        for (Map.Entry<Structure, LongSet> entry : chunk.getStructureReferences().entrySet())
        {
            Structure feature = entry.getKey();
            LongSet startChunks = entry.getValue();

            // TODO add an option && feature != StructureFeature.MINESHAFT
            if (!startChunks.isEmpty())
            {
                references.merge(feature, startChunks, (oldSet, entrySet) -> {
                    LongOpenHashSet newSet = new LongOpenHashSet(oldSet);
                    newSet.addAll(entrySet);
                    return newSet;
                });
            }
        }
    }

    protected boolean chunkHasStructureReferences(int chunkX, int chunkZ, World world)
    {
        if (!world.isChunkLoaded(chunkX, chunkZ))
        {
            return false;
        }

        Chunk chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS, false);

        if (chunk == null)
        {
            return false;
        }

        for (Map.Entry<Structure, LongSet> entry : chunk.getStructureReferences().entrySet())
        {
            // TODO add an option entry.getKey() != StructureFeature.MINESHAFT && 
            if (!entry.getValue().isEmpty())
            {
                return true;
            }
        }

        return false;
    }

    protected Map<ChunkPos, StructureStart>
    getStructureStartsFromReferences(ServerWorld world, Map<Structure, LongSet> references)
    {
        Map<ChunkPos, StructureStart> starts = new HashMap<>();

        for (Map.Entry<Structure, LongSet> entry : references.entrySet())
        {
            Structure structure = entry.getKey();
            LongSet startChunks = entry.getValue();
            LongIterator iter = startChunks.iterator();

            while (iter.hasNext())
            {
                ChunkPos pos = new ChunkPos(iter.nextLong());

                if (!world.isChunkLoaded(pos.x, pos.z))
                {
                    continue;
                }

                Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS, false);

                if (chunk == null)
                {
                    continue;
                }

                StructureStart start = chunk.getStructureStart(structure);

                if (start != null)
                {
                    starts.put(pos, start);
                }
            }
        }

        Servux.printDebug("getStructureStartsFromReferences: references: %d -> starts: %d", references.size(), starts.size());
        return starts;
    }

    protected Map<Structure, LongSet>
    getStructureReferencesWithinRange(ServerWorld world, ChunkPos center, int chunkRadius)
    {
        Map<Structure, LongSet> references = new HashMap<>();

        for (int cx = center.x - chunkRadius; cx <= center.x + chunkRadius; ++cx)
        {
            for (int cz = center.z - chunkRadius; cz <= center.z + chunkRadius; ++cz)
            {
                this.getStructureReferencesFromChunk(cx, cz, world, references);
            }
        }

        Servux.printDebug("getStructureReferencesWithinRange: references: %d", references.size());
        return references;
    }

    protected void sendStructures(ServerPlayerEntity player,
                                  Map<Structure, LongSet> references,
                                  int tickCounter)
    {
        ServerWorld world = player.getServerWorld();
        Map<ChunkPos, StructureStart> starts = this.getStructureStartsFromReferences(world, references);

        if (!starts.isEmpty())
        {
            this.addOrRefreshTimeouts(player.getUuid(), references, tickCounter);

            NbtList structureList = this.getStructureList(starts, world);
            Servux.printDebug("StructureDataProvider#sendStructures(): starts: %d -> structureList: %d. refs: %s", starts.size(), structureList.size(), references.keySet());

            NbtCompound tag = new NbtCompound();
            tag.put("Structures", structureList);

            // #FIXME ?
            Servux.printDebug("StructureDataProvider#sendStructures(): yeeting packet.");
            ((ServuxPayloadHandler) ServuxPayloadHandler.getInstance()).encodeServuxPayloadWithType(StructureDataPacketHandler.PACKET_S2C_STRUCTURE_DATA, tag, player);
        }
    }

    protected NbtList getStructureList(Map<ChunkPos, StructureStart> structures, ServerWorld world)
    {
        NbtList list = new NbtList();
        StructureContext ctx = StructureContext.from(world);

        for (Map.Entry<ChunkPos, StructureStart> entry : structures.entrySet())
        {
            ChunkPos pos = entry.getKey();
            list.add(entry.getValue().toNbt(ctx, pos));
        }

        return list;
    }
}
