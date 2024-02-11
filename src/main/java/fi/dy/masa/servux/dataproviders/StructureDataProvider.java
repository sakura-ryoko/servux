package fi.dy.masa.servux.dataproviders;

import java.util.*;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.packet.PacketType;
import fi.dy.masa.servux.network.packet.listeners.ServuxStructuresPlayListener;
import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.channel.ServuxStructuresPayload;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
import fi.dy.masa.servux.util.PlayerDimensionPosition;
import fi.dy.masa.servux.util.Timeout;

public class StructureDataProvider extends DataProviderBase
{
    public static final StructureDataProvider INSTANCE = new StructureDataProvider();
    // TODO We should probably make the Player data into a single class in the future,
    //  and then register players across all data providers, but this isn't broken either.
    protected final Map<UUID, PlayerDimensionPosition> registeredPlayers = new HashMap<>();
    protected final Map<UUID, Map<ChunkPos, Timeout>> timeouts = new HashMap<>();
    // For mapping players to STRUCTURES_ACCEPT and STRUCTURES_DECLINED state
    protected final Map<UUID, Boolean> accepted = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected int timeout = 30 * 20;
    protected int updateInterval = 40;
    protected int retainDistance;
    // FIXME --> Move out of structures channel in the future --> MetaDataProvider?
    private BlockPos spawnPos;
    private int spawnChunkRadius = -1;
    private boolean refreshSpawnMetadata;
    // TODO
    public static Identifier getChannel() { return ServuxStructuresPayload.TYPE.id(); }
    protected StructureDataProvider()
    {
        super("structure_bounding_boxes",
              PacketType.Structures.PROTOCOL_VERSION,
              "Structure Bounding Boxes data for structures such as Witch Huts, Ocean Monuments, Nether Fortresses etc.");

        this.metadata.putString("id", this.getNetworkChannel());
        this.metadata.putInt("timeout", this.timeout);
        this.metadata.putInt("version", PacketType.Structures.PROTOCOL_VERSION);
        // FIXME --> Move out of structures channel in the future
        this.metadata.putInt("spawnPosX", this.getSpawnPos().getX());
        this.metadata.putInt("spawnPosY", this.getSpawnPos().getY());
        this.metadata.putInt("spawnPosZ", this.getSpawnPos().getZ());
        this.metadata.putInt("spawnChunkRadius", this.getSpawnChunkRadius());
        // TODO
    }

    @Override
    public String getNetworkChannel() { return getChannel().toString(); }

    @Override
    public boolean shouldTick()
    {
        return true;
    }

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

                // Set Spawn Chunk Radius for Clients
                int radius = this.getSpawnChunkRadius();
                int rule = server.getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS);
                if (radius != rule)
                    this.setSpawnChunkRadius(rule);

                Iterator<UUID> uuidIter = this.registeredPlayers.keySet().iterator();

                while (uuidIter.hasNext())
                {
                    UUID uuid = uuidIter.next();
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);

                    if (player != null)
                    {
                        // Only send packets to players who have sent the STRUCTURES_ACCEPT packet
                        if (this.accepted.getOrDefault(uuid, false)) {
                            this.checkForDimensionChange(player);
                            this.refreshTrackedChunks(player, tickCounter);
                        }
                        if (this.refreshSpawnMetadata())
                            this.refreshSpawnMetadata(player);
                    }
                    else
                    {
                        this.timeouts.remove(uuid);
                        uuidIter.remove();
                    }
                }
                if (this.refreshSpawnMetadata())
                    this.setRefreshSpawnMetadataComplete();
            }
        }
    }

    public void onStartedWatchingChunk(ServerPlayerEntity player, WorldChunk chunk)
    {
        UUID uuid = player.getUuid();

        if (this.registeredPlayers.containsKey(uuid))
        {
            try
            {
                this.addChunkTimeoutIfHasReferences(uuid, chunk, Objects.requireNonNull(player.getServer()).getTicks());
            }
            catch (Exception ignored) {}
        }
    }

    public void register(ServerPlayerEntity player)
    {
        Servux.printDebug("StructureDataProvider#register(): register player: {}", player.getName().getLiteralString());
        UUID uuid = player.getUuid();

        if (!this.registeredPlayers.containsKey(uuid))
        {
            // This packet yeet doesn't always work -- too soon for the receivers to register?
            // --> It works after the client sends a PACKET_C2S_REQUEST_METADATA packet

            /*
            Servux.printDebug("StructureDataProvider#register(): yeet packet for player: {}.", player.getName().getLiteralString());
            NbtCompound nbt = new NbtCompound();
            nbt.copyFrom(this.metadata);
            nbt.putInt("packetType", PacketType.PACKET_S2C_METADATA);
            ((ServuxPayloadHandler) ServuxPayloadHandler.getInstance()).encodeServuxPayload(nbt, player, getChannel());
            */

            this.registeredPlayers.put(uuid, new PlayerDimensionPosition(player));
            this.accepted.put(uuid, false);
            // WAIT FOR CLIENTS TO REQUEST STRUCTURES DATA
            try
            {
                int tickCounter = Objects.requireNonNull(player.getServer()).getTicks();
                this.initialSyncStructuresToPlayerWithinRange(player, player.getServer().getPlayerManager().getViewDistance(), tickCounter);
            }
            catch (Exception ignored)
            {
                try {
                    this.initialSyncStructuresToPlayerWithinRange(player, Objects.requireNonNull(player.getServer()).getPlayerManager().getViewDistance(), 0);
                }
                catch (Exception ignore)
                {
                    this.initialSyncStructuresToPlayerWithinRange(player, 10, 0);
                }
            }
        }
    }

    public void unregister(ServerPlayerEntity player)
    {
        Servux.printDebug("unregister");
        this.registeredPlayers.remove(player.getUuid());
        this.accepted.remove(player.getUuid());
    }

    protected void initialSyncStructuresToPlayerWithinRange(ServerPlayerEntity player, int chunkRadius, int tickCounter)
    {
        UUID uuid = player.getUuid();
        ChunkPos center = player.getWatchedSection().toChunkPos();
        Map<Structure, LongSet> references =
                this.getStructureReferencesWithinRange(player.getServerWorld(), center, chunkRadius);

        this.timeouts.remove(uuid);
        this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);

        //Servux.printDebug("StructureDataProvider#initialSyncStructuresToPlayerWithinRange: references: {}", references.size());
        this.sendStructures(player, references, tickCounter);
    }

    protected void addChunkTimeoutIfHasReferences(final UUID uuid, WorldChunk chunk, final int tickCounter)
    {
        final ChunkPos pos = chunk.getPos();

        if (this.chunkHasStructureReferences(pos.x, pos.z, chunk.getWorld()))
        {
            final Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, (u) -> new HashMap<>());
            final int timeout = this.timeout;

            //Servux.printDebug("StructureDataProvider#addChunkTimeoutIfHasReferences: {}", pos);
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
            Servux.printDebug("StructureDataProvider#refreshTrackedChunks: timeouts: {}", map.size());
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

            Servux.printDebug("StructureDataProvider#sendAndRefreshExpiredStructures: positionsToUpdate: {} -> references: {}, to: {}", positionsToUpdate.size(), references.size(), this.timeout);

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

        Servux.printDebug("StructureDataProvider#getStructureStartsFromReferences: references: {} -> starts: {}", references.size(), starts.size());
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

        Servux.printDebug("StructureDataProvider#getStructureReferencesWithinRange: references: {}", references.size());
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
            Servux.printDebug("StructureDataProvider#sendStructures(): starts: {} -> structureList: {} refs: {}", starts.size(), structureList.size(), references.keySet());

            NbtCompound tag = new NbtCompound();
            tag.put("Structures", structureList);
            tag.putInt("packetType", PacketType.Structures.PACKET_S2C_STRUCTURE_DATA);
            Servux.printDebug("StructureDataProvider#sendStructures(): yeet packet to player: {}.", player.getName().getLiteralString());
            ServuxStructuresPlayListener.INSTANCE.encodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, tag, player);
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
    /**
     * Added a simple "OPT-IN" Boolean system for players to "Accept/Decline" receiving structure packets
     * --> Saving bandwidth, saving kittens, etc.
     */
    public void acceptStructuresFromPlayer(ServerPlayerEntity player)
    {
        // Player requested for us to either ALLOW or DISALLOW sending them Structures packets
        UUID uuid = player.getUuid();
        if (this.accepted.containsKey(uuid))
        {
            this.accepted.replace(uuid, true);
        }
        else
        {
            this.accepted.put(uuid, true);
        }
        if (this.registeredPlayers.containsKey(uuid))
        {
            this.registeredPlayers.replace(uuid, new PlayerDimensionPosition(player));
        }
        else
        {
            this.registeredPlayers.put(uuid, new PlayerDimensionPosition(player));
        }
        Servux.printDebug("StructureDataProvider#acceptStructuresFromPlayer(): start initialSyncStructuresToPlayerWithinRange() for player {}", player.getName().getLiteralString());
        try
        {
            int tickCounter = Objects.requireNonNull(player.getServer()).getTicks();
            this.initialSyncStructuresToPlayerWithinRange(player, player.getServer().getPlayerManager().getViewDistance(), tickCounter);
        }
        catch (Exception ignored)
        {
            try {
                this.initialSyncStructuresToPlayerWithinRange(player, Objects.requireNonNull(player.getServer()).getPlayerManager().getViewDistance(), 0);
            }
            catch (Exception ignore)
            {
                this.initialSyncStructuresToPlayerWithinRange(player, 10, 0);
            }
        }
    }
    public void declineStructuresFromPlayer(ServerPlayerEntity player)
    {
        // Player requested for us to either ALLOW or DISALLOW sending them Structures packets
        UUID uuid = player.getUuid();
        if (this.accepted.containsKey(uuid))
        {
            this.accepted.replace(uuid, false);
        }
        else
        {
            this.accepted.put(uuid, false);
        }
        // The Player needs to send a REQUEST_METADATA packet to resume sending Structures.
        Servux.printDebug("StructureDataProvider#declineStructuresFromPlayer(): player {} is declining to receive structure data packets.", player.getName().getLiteralString());
    }

    public void refreshMetadata(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        if (this.registeredPlayers.containsKey(uuid))
        {
            NbtCompound nbt = new NbtCompound();
            nbt.copyFrom(this.metadata);
            nbt.putInt("packetType", PacketType.Structures.PACKET_S2C_METADATA);
            ServuxStructuresPlayListener.INSTANCE.encodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, nbt, player);
        }
        else {
            register(player);
        }
    }
    // FIXME --> Move out of structures channel in the future
    public void refreshSpawnMetadata(ServerPlayerEntity player)
    {
        // Only replies to players who request it, or if the values have changed
        NbtCompound nbt = new NbtCompound();
        BlockPos spawnPos = StructureDataProvider.INSTANCE.getSpawnPos();
        nbt.putInt("packetType", PacketType.Structures.PACKET_S2C_SPAWN_METADATA);
        nbt.putString("id", getNetworkChannel());
        nbt.putInt("spawnPosX", spawnPos.getX());
        nbt.putInt("spawnPosY", spawnPos.getY());
        nbt.putInt("spawnPosZ", spawnPos.getZ());
        nbt.putInt("spawnChunkRadius", StructureDataProvider.INSTANCE.getSpawnChunkRadius());
        ServuxStructuresPlayListener.INSTANCE.encodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, nbt, player);
    }

    @Override
    public BlockPos getSpawnPos()
    {
        if (this.spawnPos == null)
            this.setSpawnPos(new BlockPos(0, 0,0));
        return this.spawnPos;
    }

    @Override
    public void setSpawnPos(BlockPos spawnPos)
    {
        if (this.spawnPos != spawnPos)
            this.refreshSpawnMetadata = true;
        this.spawnPos = spawnPos;
    }

    @Override
    public int getSpawnChunkRadius()
    {
        if (this.spawnChunkRadius < 0)
            this.spawnChunkRadius = 2;
        return this.spawnChunkRadius;
    }

    @Override
    public void setSpawnChunkRadius(int radius)
    {
        if (this.spawnChunkRadius != radius)
            this.refreshSpawnMetadata = true;
        this.spawnChunkRadius = radius;
    }
    @Override
    public boolean refreshSpawnMetadata() { return this.refreshSpawnMetadata; }
    @Override
    public void setRefreshSpawnMetadataComplete() { this.refreshSpawnMetadata = false; }
    // TODO
}
