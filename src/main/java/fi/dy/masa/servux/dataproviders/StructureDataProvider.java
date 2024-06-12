package fi.dy.masa.servux.dataproviders;

import javax.annotation.Nullable;
import java.util.*;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.network.packet.ServuxStructuresHandler;
import fi.dy.masa.servux.network.packet.ServuxStructuresPacket;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.util.PlayerDimensionPosition;
import fi.dy.masa.servux.util.Timeout;

public class StructureDataProvider extends DataProviderBase
{
    public static final StructureDataProvider INSTANCE = new StructureDataProvider();

    protected final static ServuxStructuresHandler<ServuxStructuresPacket.Payload> HANDLER = ServuxStructuresHandler.getInstance();
    protected final Map<UUID, PlayerDimensionPosition> registeredPlayers = new HashMap<>();
    protected final Map<UUID, Map<ChunkPos, Timeout>> timeouts = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected int timeout = 30 * 20;
    protected int updateInterval = 40;
    protected int retainDistance;

    // FIXME --> Move out of structures channel in the future
    private BlockPos spawnPos = BlockPos.ORIGIN;
    private int spawnChunkRadius = -1;
    private boolean refreshSpawnMetadata;

    protected StructureDataProvider()
    {
        super("structure_bounding_boxes",
                ServuxStructuresHandler.CHANNEL_ID,
                ServuxStructuresPacket.PROTOCOL_VERSION,
    "Structure Bounding Boxes data for structures such as Witch Huts, Ocean Monuments, Nether Fortresses etc.");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);
        this.metadata.putInt("timeout", this.timeout);

        // TODO --> Move out of structures channel in the future
        this.metadata.putInt("spawnPosX", this.getSpawnPos().getX());
        this.metadata.putInt("spawnPosY", this.getSpawnPos().getY());
        this.metadata.putInt("spawnPosZ", this.getSpawnPos().getZ());
        this.metadata.putInt("spawnChunkRadius", this.getSpawnChunkRadius());
    }

    @Override
    public void setEnabled(boolean toggle)
    {
        this.enabled = toggle;
    }

    @Override
    public void registerHandler()
    {
        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxStructuresPacket.Payload.ID, ServuxStructuresPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
        HANDLER.registerPlayReceiver(ServuxStructuresPacket.Payload.ID, HANDLER::receivePlayPayload);
    }

    @Override
    public void unregisterHandler()
    {
        HANDLER.unregisterPlayReceiver();
        ServerPlayHandler.getInstance().unregisterServerPlayHandler(HANDLER);
    }

    @Override
    public IPluginServerPlayHandler<ServuxStructuresPacket.Payload> getPacketHandler()
    {
        return HANDLER;
    }

    @Override
    public boolean shouldTick()
    {
        return this.enabled;
    }

    @Override
    public void tick(MinecraftServer server, int tickCounter)
    {
        if ((tickCounter % this.updateInterval) == 0)
        {
            //Servux.printDebug("=======================\n");
            //Servux.printDebug("tick: %d - %s\n", tickCounter, this.isEnabled());

            List<ServerPlayerEntity> playerList = server.getPlayerManager().getPlayerList();
            this.retainDistance = server.getPlayerManager().getViewDistance() + 2;

            int radius = this.getSpawnChunkRadius();
            int rule = server.getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS);
            if (radius != rule)
            {
                this.setSpawnChunkRadius(rule);
            }

            for (ServerPlayerEntity player : playerList)
            {
                UUID uuid = player.getUuid();

                if (this.refreshSpawnMetadata())
                {
                    this.refreshSpawnMetadata(player, null);
                }
                if (this.registeredPlayers.containsKey(uuid))
                {
                    this.checkForDimensionChange(player);
                    this.refreshTrackedChunks(player, tickCounter);
                }
            }

            this.checkForInvalidPlayers(server);
            if (this.refreshSpawnMetadata())
            {
                this.setRefreshSpawnMetadataComplete();
            }
        }
    }

    public void checkForInvalidPlayers(MinecraftServer server)
    {
        if (this.registeredPlayers.isEmpty() == false)
        {
            Iterator<UUID> iter = this.registeredPlayers.keySet().iterator();

            while (iter.hasNext())
            {
                UUID uuid = iter.next();

                if (server.getPlayerManager().getPlayer(uuid) == null)
                {
                    this.timeouts.remove(uuid);
                    iter.remove();
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
        // System.out.printf("register\n");
        boolean registered = false;
        MinecraftServer server = player.getServer();
        UUID uuid = player.getUuid();

        if (this.registeredPlayers.containsKey(uuid) == false)
        {
            this.registeredPlayers.put(uuid, new PlayerDimensionPosition(player));
            int tickCounter = server.getTicks();
            ServerPlayNetworkHandler handler = player.networkHandler;

            if (handler != null)
            {
                NbtCompound nbt = new NbtCompound();
                nbt.copyFrom(this.metadata);

                HANDLER.sendPlayPayload(handler, new ServuxStructuresPacket.Payload(new ServuxStructuresPacket(ServuxStructuresPacket.Type.PACKET_S2C_METADATA, nbt)));
                this.initialSyncStructuresToPlayerWithinRange(player, player.getServer().getPlayerManager().getViewDistance()+2, tickCounter);
            }

            registered = true;
        }

        return registered;
    }

    public boolean unregister(ServerPlayerEntity player)
    {
        // System.out.printf("unregister\n");
        HANDLER.resetFailures(this.getNetworkChannel(), player);

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
        this.sendStructures(player, references, tickCounter, false);
    }

    protected void addChunkTimeoutIfHasReferences(final UUID uuid, WorldChunk chunk, final int tickCounter)
    {
        final ChunkPos pos = chunk.getPos();

        if (this.chunkHasStructureReferences(pos.x, pos.z, chunk.getWorld()))
        {
            final Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, (u) -> new HashMap<>());
            final int timeout = this.timeout;

            //System.out.printf("addChunkTimeoutIfHasReferences: %s\n", pos);
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
            // System.out.printf("refreshTrackedChunks: timeouts: %d\n", map.size());
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

        if (positionsToUpdate.isEmpty() == false)
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

            // System.out.printf("sendAndRefreshExpiredStructures: positionsToUpdate: %d -> references: %d, to: %d\n", positionsToUpdate.size(), references.size(), this.timeout);

            if (references.isEmpty() == false)
            {
                this.sendStructures(player, references, tickCounter, true);
            }
        }
    }

    protected void getStructureReferencesFromChunk(int chunkX, int chunkZ, World world, Map<Structure, LongSet> references)
    {
        if (world.isChunkLoaded(chunkX, chunkZ) == false)
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
            if (startChunks.isEmpty() == false)
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
        if (world.isChunkLoaded(chunkX, chunkZ) == false)
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
            if (entry.getValue().isEmpty() == false)
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

                if (world.isChunkLoaded(pos.x, pos.z) == false)
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

        // System.out.printf("getStructureStartsFromReferences: references: %d -> starts: %d\n", references.size(), starts.size());
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

        // System.out.printf("getStructureReferencesWithinRange: references: %d\n", references.size());
        return references;
    }

    /**
     * New method splits Structure Packets based upon MAX_STRUCTURE_SIZE
     */
    protected void sendStructures(ServerPlayerEntity player,
                                  Map<Structure, LongSet> references,
                                  int tickCounter,
                                  boolean useApi)
    {
        ServerWorld world = player.getServerWorld();
        Map<ChunkPos, StructureStart> starts = this.getStructureStartsFromReferences(world, references);

        if (starts.isEmpty() == false)
        {
            this.addOrRefreshTimeouts(player.getUuid(), references, tickCounter);

            NbtList structureList = this.getStructureList(starts, world);
            // System.out.printf("sendStructures: starts: %d -> structureList: %d. refs: %s\n", starts.size(), structureList.size(), references.keySet());

            if (this.registeredPlayers.containsKey(player.getUuid()))
            {
                NbtCompound test = new NbtCompound();
                test.put("Structures", structureList.copy());
                HANDLER.encodeStructuresPacket(player, new ServuxStructuresPacket(ServuxStructuresPacket.Type.PACKET_S2C_STRUCTURE_DATA_START, test));
            }
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

    // TODO --> Move out of structures channel in the future (Some Metadata channel, perhaps)
    public void refreshSpawnMetadata(ServerPlayerEntity player, @Nullable NbtCompound data)
    {
        NbtCompound nbt = new NbtCompound();
        BlockPos spawnPos = StructureDataProvider.INSTANCE.getSpawnPos();

        nbt.putString("id", getNetworkChannel().toString());
        nbt.putString("servux", Reference.MOD_STRING);
        nbt.putInt("spawnPosX", spawnPos.getX());
        nbt.putInt("spawnPosY", spawnPos.getY());
        nbt.putInt("spawnPosZ", spawnPos.getZ());
        nbt.putInt("spawnChunkRadius", StructureDataProvider.INSTANCE.getSpawnChunkRadius());

        HANDLER.encodeStructuresPacket(player, new ServuxStructuresPacket(ServuxStructuresPacket.Type.PACKET_S2C_SPAWN_METADATA, nbt));
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

            //Servux.logger.info("setSpawnPos(): updating World Spawn [{}] -> [{}]", this.spawnPos.toShortString(), spawnPos.toShortString());
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

            //Servux.logger.info("setSpawnPos(): updating Spawn Chunk Radius [{}] -> [{}]", this.spawnChunkRadius, radius);
        }

        this.spawnChunkRadius = radius;
    }

    public boolean refreshSpawnMetadata() { return this.refreshSpawnMetadata; }
    public void setRefreshSpawnMetadataComplete() { this.refreshSpawnMetadata = false; }
}
