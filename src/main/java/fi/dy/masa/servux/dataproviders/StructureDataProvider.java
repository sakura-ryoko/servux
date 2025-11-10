package fi.dy.masa.servux.dataproviders;

import java.util.*;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.lucko.fabric.api.permissions.v0.Permissions;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxStructuresHandler;
import fi.dy.masa.servux.network.packet.ServuxStructuresPacket;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.settings.ServuxStringListSetting;
import fi.dy.masa.servux.util.PlayerDimensionPosition;
import fi.dy.masa.servux.util.Timeout;

public class StructureDataProvider extends DataProviderBase
{
    public static final StructureDataProvider INSTANCE = new StructureDataProvider();
	private final static ServuxStructuresHandler<ServuxStructuresPacket.Payload> HANDLER = ServuxStructuresHandler.getInstance();
	private final NbtCompound metadata = new NbtCompound();
    private final ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
    private final ServuxBoolSetting structureBlacklistEnabled = new ServuxBoolSetting(this, "structures_blacklist_enabled", false);
    private final ServuxBoolSetting structureWhitelistEnabled = new ServuxBoolSetting(this, "structures_whitelist_enabled", false);
    private final ServuxStringListSetting structureBlacklist = new ServuxStringListSetting(this, "structures_blacklist", List.of("minecraft:buried_treasure"));
    private final ServuxStringListSetting structureWhitelist = new ServuxStringListSetting(this, "structures_whitelist", List.of());
    private final ServuxIntSetting updateInterval = new ServuxIntSetting(this, "update_interval", 40, 1200, 1);
    private final ServuxIntSetting timeout = new ServuxIntSetting(this, "timeout", 600, 1200, 40);
    private final List<IServuxSetting<?>> settings = List.of(this.permissionLevel, this.structureBlacklistEnabled, this.structureWhitelistEnabled, this.structureBlacklist, this.structureWhitelist, this.updateInterval, this.timeout);

	private final Map<UUID, PlayerDimensionPosition> registeredPlayers = new HashMap<>();
	private final Map<UUID, Map<ChunkPos, Timeout>> timeouts = new HashMap<>();
	private int retainDistance;

	protected StructureDataProvider()
    {
        super("structure_bounding_boxes",
                ServuxStructuresHandler.CHANNEL_ID,
                ServuxStructuresPacket.PROTOCOL_VERSION,
                0, Reference.MOD_ID+ ".provider.structure_bounding_boxes",
                "Structure Bounding Boxes data for structures such as Witch Huts, Ocean Monuments, Nether Fortresses etc.");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);
        this.metadata.putInt("timeout", timeout.getValue());

        this.setTickRate(40);
    }

    @Override
    public List<IServuxSetting<?>> getSettings()
    {
        return settings;
    }

    @Override
    public void registerHandler()
    {
        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);

        if (!this.isRegistered())
        {
            HANDLER.registerPlayPayload(ServuxStructuresPacket.Payload.ID, ServuxStructuresPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
            this.setRegistered(true);
        }

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
    public boolean isPlayerRegistered(ServerPlayerEntity player)
    {
        return this.registeredPlayers.containsKey(player.getUuid());
    }

    @Override
    public boolean shouldTick()
    {
        return this.enabled;
    }

    @Override
    public void tick(MinecraftServer server, int tickCounter, Profiler profiler)
    {
        if (!this.isEnabled()) return;

        if ((tickCounter % this.updateInterval.getValue()) == 0)
        {
            profiler.push(this.getName());
            //Servux.printDebug("=======================\n");
            //Servux.printDebug("tick: %d - %s\n", tickCounter, this.isEnabled());

            List<ServerPlayerEntity> playerList = server.getPlayerManager().getPlayerList();
            this.retainDistance = server.getPlayerManager().getViewDistance() + 2;
            //this.lastTick = tickCounter;

            profiler.swap(this.getName() + "_players");

            for (ServerPlayerEntity player : playerList)
            {
                UUID uuid = player.getUuid();

                if (this.registeredPlayers.containsKey(uuid))
                {
                    if (!this.hasPermission(player))
                    {
                        this.unregister(player);
                    }
                    else
                    {
                        this.checkForDimensionChange(player);
                        this.refreshTrackedChunks(player, tickCounter);
                    }
                }
            }

            this.checkForInvalidPlayers(server);
            profiler.pop();
        }
    }

    public void checkForInvalidPlayers(MinecraftServer server)
    {
        if (!this.registeredPlayers.isEmpty())
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
            this.addChunkTimeoutIfHasReferences(uuid, chunk, player.getCommandSource().getServer().getTicks());
        }
    }

    public boolean register(ServerPlayerEntity player)
    {
        if (!this.isEnabled()) return false;

        // System.out.printf("register\n");
        boolean registered = false;
        MinecraftServer server = player.getCommandSource().getServer();
        UUID uuid = player.getUuid();

        if (!this.hasPermission(player))
        {
            // No Permission
            Servux.debugLog("structure_bounding_boxes: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return registered;
        }

        if (!this.registeredPlayers.containsKey(uuid))
        {
            this.registeredPlayers.put(uuid, new PlayerDimensionPosition(player));
            int tickCounter = server.getTicks();
            ServerPlayNetworkHandler handler = player.networkHandler;

            if (handler != null)
            {
                NbtCompound nbt = new NbtCompound();
                nbt.copyFrom(this.metadata);

                Servux.debugLog("structure_bounding_boxes: sending Metadata to player {}", player.getName().getLiteralString());
                HANDLER.sendPlayPayload(handler, new ServuxStructuresPacket.Payload(new ServuxStructuresPacket(ServuxStructuresPacket.Type.PACKET_S2C_METADATA, nbt)));
                this.initialSyncStructuresToPlayerWithinRange(player, player.getCommandSource().getServer().getPlayerManager().getViewDistance()+2, tickCounter);
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
        Map<Structure, LongSet> references = this.getStructureReferencesWithinRange(player.getEntityWorld(), center, chunkRadius);

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

            //System.out.printf("addChunkTimeoutIfHasReferences: %s\n", pos);
            // Set the timeout so it's already expired and will cause the chunk to be sent on the next update tick
            map.computeIfAbsent(pos, (p) -> new Timeout(tickCounter - timeout.getValue()));
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

            if (timeout.needsUpdate(tickCounter, this.timeout.getValue()))
            {
                positionsToUpdate.add(entry.getKey());
            }
        }

        if (!positionsToUpdate.isEmpty())
        {
            ServerWorld world = player.getEntityWorld();
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

        Chunk chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_REFERENCES, false);

        if (chunk == null)
        {
            return;
        }

        for (Map.Entry<Structure, LongSet> entry : chunk.getStructureReferences().entrySet())
        {
            Structure feature = entry.getKey();
            LongSet startChunks = entry.getValue();

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

        Chunk chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_REFERENCES, false);

        if (chunk == null)
        {
            return false;
        }

        for (Map.Entry<Structure, LongSet> entry : chunk.getStructureReferences().entrySet())
        {
            if (!entry.getValue().isEmpty())
            {
                return true;
            }
        }

        return false;
    }

    protected Map<ChunkPos, StructureStart> getStructureStartsFromReferences(ServerWorld world, Map<Structure, LongSet> references)
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

                Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_REFERENCES, false);

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

    protected Map<Structure, LongSet> getStructureReferencesWithinRange(ServerWorld world, ChunkPos center, int chunkRadius)
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

    protected void sendStructures(ServerPlayerEntity player,
                                  Map<Structure, LongSet> references,
                                  int tickCounter)
    {
        ServerWorld world = player.getEntityWorld();
        Map<ChunkPos, StructureStart> starts = this.getStructureStartsFromReferences(world, references);

        if (!starts.isEmpty())
        {
            this.addOrRefreshTimeouts(player.getUuid(), references, tickCounter);

            NbtList structureList = this.getStructureList(starts, world);
            // System.out.printf("sendStructures: starts: %d -> structureList: %d. refs: %s\n", starts.size(), structureList.size(), references.keySet());

            if (this.registeredPlayers.containsKey(player.getUuid()))
            {
                NbtCompound nbt = new NbtCompound();
                nbt.put("Structures", structureList.copy());
                HANDLER.encodeStructuresPacket(player, new ServuxStructuresPacket(ServuxStructuresPacket.Type.PACKET_S2C_STRUCTURE_DATA_START, nbt));
            }
        }
    }

    protected NbtList getStructureList(Map<ChunkPos, StructureStart> structures, ServerWorld world)
    {
        NbtList list = new NbtList();
        StructureContext ctx = StructureContext.from(world);

        for (Map.Entry<ChunkPos, StructureStart> entry : structures.entrySet())
        {
			Structure structure = entry.getValue().getStructure();
			if (structure == null) continue;          // When using C2ME, this could return NULL
            Identifier structureType = Registries.STRUCTURE_TYPE.getId(structure.getType());

            if (this.shouldSendStructure(structureType))
            {
                ChunkPos pos = entry.getKey();
                list.add(entry.getValue().toNbt(ctx, pos));
            }
        }

        return list;
    }

    protected boolean shouldSendStructure(Identifier identifier)
    {
        if (this.structureWhitelistEnabled.getValue())
        {
            return this.structureWhitelist.getValue().contains(identifier.toString());
        }
        if (this.structureBlacklistEnabled.getValue())
        {
            return !this.structureBlacklist.getValue().contains(identifier.toString());
        }

        return true;
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode, this.permissionLevel.getValue());
    }

    @Override
    public void onTickEndPre()
    {
        // NO-OP
    }

    @Override
    public void onTickEndPost()
    {
        // NO-OP
    }
}
