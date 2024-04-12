package fi.dy.masa.servux.dataproviders.data;

import java.net.SocketAddress;
import java.util.*;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.channel.ServuxStructuresPayload;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.dataproviders.DataProviderBase;
import fi.dy.masa.servux.dataproviders.client.StructureClient;
import fi.dy.masa.servux.network.PacketType;
import fi.dy.masa.servux.network.listeners.ServuxStructuresPlayListener;
import fi.dy.masa.servux.util.PlayerDimensionPosition;
import fi.dy.masa.servux.util.Timeout;

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

public class StructureDataProvider extends DataProviderBase
{
    public static final StructureDataProvider INSTANCE = new StructureDataProvider();
    protected final Map<UUID, StructureClient> CLIENTS = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected final int protocolVersion = PacketType.Structures.PROTOCOL_VERSION;
    protected final Map<UUID, Map<ChunkPos, Timeout>> timeouts = new HashMap<>();
    protected int timeout = 30 * 20;
    protected int updateInterval = 40;
    protected int retainDistance;
    private final int DEFAULT_CHUNK_RANGE = 10;

    // FIXME --> Move out of structures channel in the future
    private BlockPos spawnPos;
    private int spawnChunkRadius = -1;
    private boolean refreshSpawnMetadata;

    protected StructureDataProvider()
    {
        super("structure_bounding_boxes",
              PacketType.Structures.PROTOCOL_VERSION,
    "Structure Bounding Boxes data for structures such as Witch Huts, Ocean Monuments, Nether Fortresses etc.");

        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("timeout", this.timeout);
        this.metadata.putInt("version", this.protocolVersion);
        this.metadata.putString("servux", ServuxReference.MOD_STRING);

        // TODO --> Move out of structures channel in the future
        this.metadata.putInt("spawnPosX", this.getSpawnPos().getX());
        this.metadata.putInt("spawnPosY", this.getSpawnPos().getY());
        this.metadata.putInt("spawnPosZ", this.getSpawnPos().getZ());
        this.metadata.putInt("spawnChunkRadius", this.getSpawnChunkRadius());
    }

    @Override
    public PayloadType getNetworkChannel() { return PayloadType.SERVUX_STRUCTURES; }

    @Override
    public int getProtocolVersion() { return this.protocolVersion; }

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
            if (!this.CLIENTS.isEmpty())
            {
                //Servux.printDebug("=======================\n");
                //Servux.printDebug("tick: %d - %s\n", tickCounter, this.isEnabled());
                this.retainDistance = server.getPlayerManager().getViewDistance() + 2;

                // Set Spawn Chunk Radius for Clients
                int radius = this.getSpawnChunkRadius();
                int rule = server.getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS);
                if (radius != rule)
                    this.setSpawnChunkRadius(rule);

                Iterator<UUID> uuidIter = this.CLIENTS.keySet().iterator();

                while (uuidIter.hasNext())
                {
                    UUID uuid = uuidIter.next();
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);

                    if (player != null)
                    {
                        StructureClient client = CLIENTS.get(uuid);
                        if (client.isStructuresClient() && client.isStructuresEnabled())
                        {
                            // Only send packets to players who have sent the STRUCTURES_ACCEPT packet
                            this.checkForDimensionChange(player);
                            this.refreshTrackedChunks(player, tickCounter);
                        }
                        if (this.refreshSpawnMetadata())
                            this.refreshSpawnMetadata(player, null);
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

        if (this.CLIENTS.containsKey(uuid))
        {
            try
            {
                this.addChunkTimeoutIfHasReferences(uuid, chunk, Objects.requireNonNull(player.getServer()).getTicks());
            }
            catch (Exception ignored) {}
        }
    }

    public void register(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();

        if (!this.CLIENTS.containsKey(uuid))
        {
            StructureClient newClient = new StructureClient(player.getName().getLiteralString(), uuid, null);

            // registerClient handles the new PlayerDimensionPosition call and store's it.
            newClient.registerClient(addr, profile, player);
            newClient.structuresDisableClient();
            this.CLIENTS.put(uuid, newClient);
            Servux.printDebug("registering StructureClient for player {}", player.getName().getLiteralString());

            if (ServuxReference.isDedicated() || ServuxReference.isOpenToLan())
            {
                // If this fails, we can still use the Fabric Network API method later after they send us a METADATA_REQUEST
                ServerPlayNetworkHandler handler = player.networkHandler;
                if (handler != null)
                {
                    NbtCompound nbt = new NbtCompound();
                    nbt.copyFrom(this.metadata);
                    nbt.putInt("packetType", PacketType.Structures.PACKET_S2C_METADATA);
                    ServuxStructuresPayload payload = new ServuxStructuresPayload(nbt);

                    ServuxStructuresPlayListener.getInstance().sendS2CPlayPayload(PayloadType.SERVUX_STRUCTURES, payload, handler);
                }
            }
            // Initial sync (MOVED AFTER STRUCTURES_ACCEPT Packet)
            /*
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
                    this.initialSyncStructuresToPlayerWithinRange(player, DEFAULT_CHUNK_RANGE, 0);
                }
            }
             */
        }
    }

    public void unregister(ServerPlayerEntity player)
    {
        UUID id = player.getUuid();

        Servux.printDebug("unregistering StructureClient for player {}", player.getName().getLiteralString());
        StructureClient oldClient = this.CLIENTS.get(id);
        oldClient.structuresDisableClient();
        oldClient.unregisterClient();
        this.CLIENTS.remove(id);
    }

    protected void initialSyncStructuresToPlayerWithinRange(ServerPlayerEntity player, int chunkRadius, int tickCounter)
    {
        UUID uuid = player.getUuid();
        ChunkPos center = player.getWatchedSection().toChunkPos();
        Map<Structure, LongSet> references =
                this.getStructureReferencesWithinRange(player.getServerWorld(), center, chunkRadius);

        this.timeouts.remove(uuid);

        StructureClient client = this.CLIENTS.get(uuid);
        PlayerDimensionPosition newDim = new PlayerDimensionPosition(player);
        newDim.setPosition(player);
        client.setClientDimension(newDim);
        this.CLIENTS.replace(uuid, client);

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

            // Set the timeout so it's already expired and will cause the chunk to be sent on the next update tick
            map.computeIfAbsent(pos, (p) -> new Timeout(tickCounter - timeout));
        }
    }

    protected void checkForDimensionChange(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        StructureClient client = this.CLIENTS.get(uuid);
        PlayerDimensionPosition playerPos = client.getClientDimension();

        if (playerPos == null || playerPos.dimensionChanged(player))
        {
            this.timeouts.remove(uuid);

            //Servux.printDebug("checkForDimensionChange(): StructureClient update player dim for {}", player.getName().getLiteralString());
            PlayerDimensionPosition newDim = new PlayerDimensionPosition(player);
            newDim.setPosition(player);
            client.setClientDimension(newDim);
            this.CLIENTS.replace(uuid, client);
        }
    }

    protected void addOrRefreshTimeouts(final UUID uuid,
                                        final Map<Structure, LongSet> references,
                                        final int tickCounter)
    {
        //Servux.printDebug("StructureDataProvider#addOrRefreshTimeouts: references: {}", references.size());
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
            //Servux.printDebug("StructureDataProvider#refreshTrackedChunks: timeouts: {}", map.size());
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

            //Servux.printDebug("StructureDataProvider#sendAndRefreshExpiredStructures: positionsToUpdate: {} -> references: {}, to: {}", positionsToUpdate.size(), references.size(), this.timeout);

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
            // TODO add an option entry.getKey() != StructureFeature.MINESHAFT
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

        //Servux.printDebug("StructureDataProvider#getStructureStartsFromReferences: references: {} -> starts: {}", references.size(), starts.size());
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

        //Servux.printDebug("StructureDataProvider#getStructureReferencesWithinRange: references: {}", references.size());
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
            //Servux.printDebug("StructureDataProvider#sendStructures(): starts: {} -> structureList: {} refs: {}", starts.size(), structureList.size(), references.keySet());

            UUID uuid = player.getUuid();
            if (this.CLIENTS.containsKey(uuid))
            {
                //StructureClient client = this.CLIENTS.get(uuid);

                NbtCompound tag = new NbtCompound();
                tag.put("Structures", structureList);
                tag.putInt("packetType", PacketType.Structures.PACKET_S2C_STRUCTURE_DATA);

                ServuxStructuresPlayListener.getInstance().encodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, tag, player);
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

    /**
     * Added a simple "OPT-IN" Boolean system for players to "Accept/Decline" receiving structure packets
     * --> Saving bandwidth, saving kittens, etc.
     */
    public void acceptStructuresFromPlayer(ServerPlayerEntity player, NbtCompound data)
    {
        UUID uuid = player.getUuid();

        if (this.CLIENTS.containsKey(uuid))
        {
            StructureClient client = this.CLIENTS.get(uuid);
            if (client.getVersion().isEmpty() && data.contains("version"))
            {
                String ver = data.getString("version");
                if (ver.isEmpty())
                {
                    Servux.printDebug("acceptStructures from player {}", player.getName().getLiteralString());
                }
                else
                {
                    client.setClientVersion(ver);
                    Servux.printDebug("acceptStructures from player {} with client version: {}", player.getName().getLiteralString(), ver);
                }
            }
            else
            {
                if (client.getVersion().isEmpty())
                {
                    Servux.printDebug("acceptStructures from player {}", player.getName().getLiteralString());
                }
                else
                {
                    Servux.printDebug("acceptStructures from player {} with client version: {}", player.getName().getLiteralString(), client.getVersion());
                }
            }
            client.structuresEnableClient();
            client.setClientDimension(new PlayerDimensionPosition(player));
            this.CLIENTS.replace(uuid, client);
        }
        //Servux.printDebug("StructureDataProvider#acceptStructuresFromPlayer(): start initialSyncStructuresToPlayerWithinRange() for player {}", player.getName().getLiteralString());

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
                this.initialSyncStructuresToPlayerWithinRange(player, DEFAULT_CHUNK_RANGE, 0);
            }
        }
    }

    public void declineStructuresFromPlayer(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();

        if (this.CLIENTS.containsKey(uuid))
        {
            StructureClient client = this.CLIENTS.get(uuid);

            client.structuresDisableClient();
            //client.setClientDimension(new PlayerDimensionPosition(player));
            if (client.getVersion().isEmpty())
            {
                Servux.printDebug("structuresDeclined from player {}", player.getName().getLiteralString());
            }
            else
            {
                Servux.printDebug("structuresDeclined from player {} with client version {}", player.getName().getLiteralString(), client.getVersion());
            }
            this.CLIENTS.replace(uuid, client);
        }
    }

    public void refreshMetadata(ServerPlayerEntity player, NbtCompound data)
    {
        UUID uuid = player.getUuid();

        if (this.CLIENTS.containsKey(uuid))
        {
            StructureClient client = this.CLIENTS.get(uuid);

            if (client.getVersion().isEmpty() && data.contains("version"))
            {
                String ver = data.getString("version");
                if (!ver.isEmpty())
                {
                    client.setClientVersion(ver);
                    Servux.printDebug("refreshMetadata(): Client version: {}", ver);
                    this.CLIENTS.replace(uuid, client);
                }
            }

            NbtCompound nbt = new NbtCompound();
            nbt.copyFrom(this.metadata);
            nbt.putInt("packetType", PacketType.Structures.PACKET_S2C_METADATA);
            ServuxStructuresPlayListener.getInstance().encodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, nbt, player);
        }
        else
        {
            register(player.networkHandler.getConnectionAddress(), player.getGameProfile(), player);
        }
    }

    /**
     *  This allows ServUX to only send those that are toggled ON, on a per-client basis, need be.
     */
    public NbtList filterStructureList(StructureClient client, NbtList oldList)
    {
        NbtList newList = new NbtList();

        for (int i = 0; i < oldList.size(); i++)
        {
            NbtCompound oldStructure = oldList.getCompound(i);
            // TODO if you want this feature (I removed the rest of the code)
            //String structureId = oldStructure.getString("id");

            //if (isStructureToggled(client, structureId))
            //{
                //Servux.printDebug("filterStructureList(): id {} sending to client {}", structureId, client.getName());
                newList.add(oldStructure);
            //}
        }

        return newList;
    }

    // TODO --> Move out of structures channel in the future (Server Metadata channel, perhaps)
    public void refreshSpawnMetadata(ServerPlayerEntity player, NbtCompound data)
    {
        UUID uuid = player.getUuid();

        if (this.CLIENTS.containsKey(uuid))
        {
            StructureClient client = this.CLIENTS.get(uuid);

            if (client.getVersion().isEmpty() && data != null && data.contains("version"))
            {
                String ver = data.getString("version");

                if (!ver.isEmpty())
                {
                    client.setClientVersion(ver);
                    Servux.printDebug("refreshSpawnMetadata request from player {} with client version: {}", player.getName().getLiteralString(), ver);
                    this.CLIENTS.replace(uuid, client);
                }
            }
        }

        // Only replies to players who request it, or if the values have changed
        if (ServuxReference.isDedicated() || ServuxReference.isOpenToLan())
        {
            NbtCompound nbt = new NbtCompound();
            BlockPos spawnPos = StructureDataProvider.INSTANCE.getSpawnPos();

            nbt.putInt("packetType", PacketType.Structures.PACKET_S2C_SPAWN_METADATA);
            nbt.putString("id", getNetworkChannel().toString());
            nbt.putString("servux", ServuxReference.MOD_STRING);
            nbt.putInt("spawnPosX", spawnPos.getX());
            nbt.putInt("spawnPosY", spawnPos.getY());
            nbt.putInt("spawnPosZ", spawnPos.getZ());
            nbt.putInt("spawnChunkRadius", StructureDataProvider.INSTANCE.getSpawnChunkRadius());

            ServuxStructuresPlayListener.getInstance().encodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, nbt, player);
        }
    }

    public BlockPos getSpawnPos()
    {
        if (this.spawnPos == null)
            this.setSpawnPos(new BlockPos(0, 0,0));
        return this.spawnPos;
    }

    public void setSpawnPos(BlockPos spawnPos)
    {
        if (this.spawnPos != spawnPos)
        {
            // Update DataProvider's metadata to be refreshed
            this.metadata.remove("spawnPosX");
            this.metadata.remove("spawnPosY");
            this.metadata.remove("spawnPosZ");
            this.metadata.putInt("spawnPosX", spawnPos.getX());
            this.metadata.putInt("spawnPosY", spawnPos.getY());
            this.metadata.putInt("spawnPosZ", spawnPos.getZ());
            this.refreshSpawnMetadata = true;
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
            this.refreshSpawnMetadata = true;
        }
        this.spawnChunkRadius = radius;
    }

    public boolean refreshSpawnMetadata() { return this.refreshSpawnMetadata; }
    public void setRefreshSpawnMetadataComplete() { this.refreshSpawnMetadata = false; }
}
