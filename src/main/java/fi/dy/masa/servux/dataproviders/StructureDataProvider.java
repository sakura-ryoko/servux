package fi.dy.masa.servux.dataproviders;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.PacketSplitter;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxStructuresHandler;
import fi.dy.masa.servux.network.packet.ServuxStructuresPacket;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.settings.ServuxStringListSetting;
import fi.dy.masa.servux.util.PermissionsUtil;
import fi.dy.masa.servux.util.StringUtils;
import fi.dy.masa.servux.util.Timeout;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.ListData;
import fi.dy.masa.servux.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.servux.util.PlayerDimensionPosition;

public class StructureDataProvider extends DataProviderBase
{
	public static final StructureDataProvider INSTANCE = new StructureDataProvider();
	private final static ServuxStructuresHandler<ServuxStructuresPacket.Payload> HANDLER = ServuxStructuresHandler.getInstance();
	private final CompoundData metadata = new CompoundData();
	private final ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
	private final ServuxBoolSetting structureBlacklistEnabled = new ServuxBoolSetting(this, "structures_blacklist_enabled", false);
	private final ServuxBoolSetting structureWhitelistEnabled = new ServuxBoolSetting(this, "structures_whitelist_enabled", false);
	private final ServuxStringListSetting structureBlacklist = new ServuxStringListSetting(this, "structures_blacklist", List.of("minecraft:buried_treasure"));
	private final ServuxStringListSetting structureWhitelist = new ServuxStringListSetting(this, "structures_whitelist", List.of());
	private final ServuxIntSetting updateInterval = new ServuxIntSetting(this, "update_interval", 40, 1200, 1);
	private final ServuxIntSetting timeout = new ServuxIntSetting(this, "timeout", 600, 1200, 40);
	private final List<IServuxSetting<?>> settings = List.of(this.permissionLevel, this.structureBlacklistEnabled, this.structureWhitelistEnabled, this.structureBlacklist, this.structureWhitelist, this.updateInterval, this.timeout);

	private final List<UUID> registeredPlayers = new ArrayList<>();
	private final List<UUID> invalidPlayers = new ArrayList<>();
	private final ConcurrentHashMap<UUID, PlayerDimensionPosition> playerPositons = new ConcurrentHashMap<>(16, 0.9f, 2);
	private final ConcurrentHashMap<UUID, Map<ChunkPos, Timeout>> timeouts = new ConcurrentHashMap<>(16, 0.9f, 2);
	private final ConcurrentHashMap<UUID, Integer> maxPacketSize = new ConcurrentHashMap<>(16, 0.9f, 2);
	private int retainDistance;

	protected StructureDataProvider()
	{
		super("structure_bounding_boxes",
		      ServuxStructuresHandler.CHANNEL_ID,
		      ServuxStructuresPacket.PROTOCOL_VERSION,
		      0, Reference.MOD_ID + ".provider.structure_bounding_boxes",
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
	public boolean isPlayerRegistered(ServerPlayer player)
	{
		return this.registeredPlayers.contains(player.getUUID()) && !this.isPlayerInvalid(player);
	}

	@Override
	public boolean shouldTick()
	{
		return this.enabled;
	}

	@Override
	public void tick(MinecraftServer server, int tickCounter, ProfilerFiller profiler)
	{
		if (!this.isEnabled()) { return; }
		if ((tickCounter % this.updateInterval.getValue()) == 0)
		{
			profiler.push(this.getName());
			//Servux.printDebug("=======================\n");
			//Servux.printDebug("tick: %d - %s\n", tickCounter, this.isEnabled());

			PlayerList playerList = server.getPlayerList();
			List<ServerPlayer> players = playerList.getPlayers();
//			final long now = System.currentTimeMillis();

			this.retainDistance = playerList.getViewDistance() + 2;
			//this.lastTick = tickCounter;
			profiler.popPush(this.getName() + "_players");

			for (ServerPlayer player : players)
			{
				if (this.isPlayerRegistered(player))
				{
					if (!this.hasPermission(player))
					{
						this.removePlayer(player);
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
		if (!this.playerPositons.isEmpty())
		{
			Set<UUID> keys = this.playerPositons.keySet();

			for (UUID uuid : keys)
			{
				if (server.getPlayerList().getPlayer(uuid) == null)
				{
					this.playerPositons.remove(uuid);
					this.timeouts.remove(uuid);
					this.maxPacketSize.remove(uuid);
					this.registeredPlayers.remove(uuid);
				}
			}
		}
	}

	public void onStartedWatchingChunk(ServerPlayer player, LevelChunk chunk)
	{
		UUID uuid = player.getUUID();

		if (this.playerPositons.containsKey(uuid))
		{
			this.addChunkTimeoutIfHasReferences(uuid, chunk, player.createCommandSourceStack().getServer().getTickCount());
		}
	}

	@Override
	public void register(ServerPlayer player, CompoundData tags)
	{
		if (!this.isEnabled()) { return; }
		UUID uuid = player.getUUID();

		if (tags == null || tags.getIntOrDefault("version", -1) < this.getProtocolVersion())
		{
			Servux.LOGGER.warn("structure_bounding_boxes: Denying access for player {}, Insufficient Protocol Version; This Server Requires: Version {}", player.getName().tryCollapseToString(), this.getProtocolVersion());
			player.sendSystemMessage(StringUtils.translate("servux.general.error.protocol_version_too_low", this.getName()));
			HANDLER.tickFailures(player);
			return;
		}

		if (!this.hasPermission(player))
		{
			// No Permission
			Servux.debugLog("structure_bounding_boxes: Denying access for player {}, Insufficient Permissions", player.getName().tryCollapseToString());
			return;
		}

		this.removeInvalidPlayer(player);

		MinecraftServer server = player.createCommandSourceStack().getServer();
		int tickCounter = server.getTickCount();
		final int maxPacketSize = tags.getIntOrDefault("max_receive_s2c", PacketSplitter.DEFAULT_MAX_RECEIVE_SIZE_S2C);
		CompoundData nbt = new CompoundData();

		this.registeredPlayers.add(uuid);
		this.playerPositons.put(uuid, new PlayerDimensionPosition(player));
		this.maxPacketSize.put(uuid, maxPacketSize);
		nbt.combine(this.metadata);

		Servux.debugLog("structure_bounding_boxes: sending Metadata to player {}", player.getName().tryCollapseToString());
		if (player.connection != null)
		{
			HANDLER.sendPlayPayload(player.connection, new ServuxStructuresPacket.Payload(ServuxStructuresPacket.MetadataReply(nbt)));
		}
		else
		{
			HANDLER.sendPlayPayload(player, new ServuxStructuresPacket.Payload(ServuxStructuresPacket.MetadataReply(nbt)));
		}

		this.initialSyncStructuresToPlayerWithinRange(player, server.getPlayerList().getViewDistance() + 2, tickCounter);
	}

	@Override
	public void unregister(ServerPlayer player, CompoundData tags)
	{
		if (this.isEnabled())
		{
			Servux.debugLog("structure_bounding_boxes: Unregistered player {}", player.getName().tryCollapseToString());
		}

		UUID uuid = player.getUUID();

		HANDLER.resetFailures(this.getNetworkChannel(), player);
		this.registeredPlayers.remove(uuid);
		this.timeouts.remove(uuid);
		this.maxPacketSize.remove(uuid);
		this.playerPositons.remove(uuid);
	}

	@Override
	public void onPacketFailure(ServerPlayer player)
	{
		UUID uuid = player.getUUID();
		this.setPlayerInvalid(player);
		this.registeredPlayers.remove(uuid);
		this.playerPositons.remove(uuid);
		this.timeouts.remove(uuid);
		this.maxPacketSize.remove(uuid);
	}

	@Override
	public void removePlayer(ServerPlayer player)
	{
		UUID uuid = player.getUUID();
		this.removeInvalidPlayer(player);
		this.registeredPlayers.remove(uuid);
		this.playerPositons.remove(uuid);
		this.timeouts.remove(uuid);
		this.maxPacketSize.remove(uuid);
		HANDLER.resetFailures(this.getNetworkChannel(), player);
	}

	private void setPlayerInvalid(ServerPlayer player)
	{
		UUID uuid = player.getUUID();

		if (!this.invalidPlayers.contains(uuid))
		{
			this.invalidPlayers.add(uuid);
		}
	}

	private boolean isPlayerInvalid(ServerPlayer player)
	{
		return this.invalidPlayers.contains(player.getUUID());
	}

	private void removeInvalidPlayer(ServerPlayer player)
	{
		this.invalidPlayers.remove(player.getUUID());
	}

	protected void initialSyncStructuresToPlayerWithinRange(ServerPlayer player, int chunkRadius, int tickCounter)
	{
		if (!this.isEnabled() || !this.isPlayerRegistered(player))
		{
			return;
		}

		UUID uuid = player.getUUID();
		ChunkPos center = player.getLastSectionPos().chunk();
		Map<Structure, LongSet> references = this.getStructureReferencesWithinRange(player.level(), center, chunkRadius);

		this.timeouts.remove(uuid);
		this.playerPositons.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);

//        System.out.printf("initialSyncStructuresToPlayerWithinRange: references: %d\n", references.size());
		this.sendStructures(player, references, tickCounter);
	}

	protected void addChunkTimeoutIfHasReferences(final UUID uuid, LevelChunk chunk, final int tickCounter)
	{
		final ChunkPos pos = chunk.getPos();

		if (this.chunkHasStructureReferences(pos.x, pos.z, chunk.getLevel()))
		{
			final Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, (u) -> new HashMap<>());

//            System.out.printf("addChunkTimeoutIfHasReferences: %s\n", pos);
			// Set the timeout so it's already expired and will cause the chunk to be sent on the next update tick
			map.computeIfAbsent(pos, (p) -> new Timeout(tickCounter - timeout.getValue()));
		}
	}

	protected void checkForDimensionChange(ServerPlayer player)
	{
		if (!this.isEnabled() || !this.isPlayerRegistered(player))
		{
			return;
		}

		UUID uuid = player.getUUID();
		PlayerDimensionPosition playerPos = this.playerPositons.get(uuid);

		if (playerPos == null || playerPos.dimensionChanged(player))
		{
			this.timeouts.remove(uuid);
			this.playerPositons.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);
		}
	}

	protected void addOrRefreshTimeouts(final UUID uuid,
	                                    final Map<Structure, LongSet> references,
	                                    final int tickCounter)
	{
//        System.out.printf("addOrRefreshTimeouts: references: %d\n", references.size());
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

	protected void refreshTrackedChunks(ServerPlayer player, int tickCounter)
	{
		UUID uuid = player.getUUID();
		Map<ChunkPos, Timeout> map = this.timeouts.get(uuid);

		if (map != null)
		{
//            System.out.printf("refreshTrackedChunks: timeouts: %d\n", map.size());
			this.sendAndRefreshExpiredStructures(player, map, tickCounter);
		}
	}

	protected boolean isOutOfRange(ChunkPos pos, ChunkPos center)
	{
		int chunkRadius = this.retainDistance;

		return  Math.abs(pos.x - center.x) > chunkRadius ||
				Math.abs(pos.z - center.z) > chunkRadius;
	}

	protected void sendAndRefreshExpiredStructures(ServerPlayer player, Map<ChunkPos, Timeout> map, int tickCounter)
	{
		if (!this.isEnabled() || !this.isPlayerRegistered(player))
		{
			return;
		}

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
			ServerLevel world = player.level();
			ChunkPos center = player.getLastSectionPos().chunk();
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

//            System.out.printf("sendAndRefreshExpiredStructures: positionsToUpdate: %d -> references: %d, to: %d\n", positionsToUpdate.size(), references.size(), this.timeout.getValue());

			if (!references.isEmpty())
			{
				this.sendStructures(player, references, tickCounter);
			}
		}
	}

	protected void getStructureReferencesFromChunk(int chunkX, int chunkZ, Level world, Map<Structure, LongSet> references)
	{
		if (!world.hasChunk(chunkX, chunkZ)) { return; }
		ChunkAccess chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_REFERENCES, false);

		if (chunk == null) { return; }

		for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet())
		{
			Structure feature = entry.getKey();
			LongSet startChunks = entry.getValue();

			if (!startChunks.isEmpty())
			{
				references.merge(feature, startChunks, (oldSet, entrySet) ->
				{
					LongOpenHashSet newSet = new LongOpenHashSet(oldSet);
					newSet.addAll(entrySet);
					return newSet;
				});
			}
		}
	}

	protected boolean chunkHasStructureReferences(int chunkX, int chunkZ, Level world)
	{
		if (!world.hasChunk(chunkX, chunkZ)) { return false; }

		ChunkAccess chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_REFERENCES, false);

		if (chunk == null) { return false; }

		for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet())
		{
			if (!entry.getValue().isEmpty()) { return true; }
		}

		return false;
	}

	protected Map<ChunkPos, StructureStart> getStructureStartsFromReferences(ServerLevel world, Map<Structure, LongSet> references)
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

				if (!world.hasChunk(pos.x, pos.z))
				{
					continue;
				}

				ChunkAccess chunk = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_REFERENCES, false);

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

//        System.out.printf("getStructureStartsFromReferences: references: %d -> starts: %d\n", references.size(), starts.size());
		return starts;
	}

	protected Map<Structure, LongSet> getStructureReferencesWithinRange(ServerLevel world, ChunkPos center, int chunkRadius)
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

	protected void sendStructures(ServerPlayer player,
	                              Map<Structure, LongSet> references,
	                              int tickCounter)
	{
		if (!this.isEnabled() || !this.isPlayerRegistered(player))
		{
			return;
		}

		if (!this.hasPermission(player))
		{
			Servux.debugLog("structure_bounding_boxes: Denying sendStructures to player {}, Insufficient Permissions.", player.getName().getString());
			return;
		}

		ServerLevel world = player.level();
		UUID uuid = player.getUUID();
		Map<ChunkPos, StructureStart> starts = this.getStructureStartsFromReferences(world, references);

		if (!starts.isEmpty())
		{
			this.addOrRefreshTimeouts(uuid, references, tickCounter);

			ListData structureList = this.getStructureList(starts, world);
//            System.out.printf("sendStructures: starts: %d -> structureList: %d. refs: %s\n", starts.size(), structureList.size(), references.keySet());

			if (this.isPlayerRegistered(player) && this.playerPositons.containsKey(uuid))
			{
				CompoundData nbt = new CompoundData();
				final int maxSize = this.maxPacketSize.getOrDefault(uuid, PacketSplitter.DEFAULT_MAX_RECEIVE_SIZE_C2S);
				final int padding = 4096;

				if ((structureList.sizeInBytes() + padding) <= maxSize)
				{
					nbt.put("Structures", structureList.copy());
					HANDLER.encodeServerData(player, ServuxStructuresPacket.StructuresS2CStart(nbt));
				}
				else
				{
					// This won't happen too often ...  But just to be safe?
					ListData sendList = new ListData();
					final int total = structureList.size();

					for (int i = 0; i < total; i++)
					{
						CompoundData entry = structureList.getCompoundAt(i);
						if (entry.isEmpty()) { continue; }
						int currentSize = sendList.sizeInBytes() + padding;

						// Check size
						if (!sendList.isEmpty() && (currentSize + entry.sizeInBytes()) >= maxSize)
						{
							// Release.
							nbt.put("Structures", sendList.copy());
							HANDLER.encodeServerData(player, ServuxStructuresPacket.StructuresS2CStart(nbt));
							sendList.clear();
							nbt.remove("Structures");
						}

						sendList.add(entry.copy());
					}

					if (!sendList.isEmpty())
					{
						// Release.
						nbt.put("Structures", sendList.copy());
						HANDLER.encodeServerData(player, ServuxStructuresPacket.StructuresS2CStart(nbt));
					}
				}
			}
		}
	}

	protected ListData getStructureList(Map<ChunkPos, StructureStart> structures, ServerLevel world)
	{
		ListData list = new ListData();
		StructurePieceSerializationContext ctx = StructurePieceSerializationContext.fromLevel(world);

		for (Map.Entry<ChunkPos, StructureStart> entry : structures.entrySet())
		{
			StructureStart start = entry.getValue();
			Structure structure = start.getStructure();
			if (structure == null)
			{
				continue;          // When using C2ME, this could return NULL ...
			}
			Identifier structureType = BuiltInRegistries.STRUCTURE_TYPE.getKey(structure.type());
			boolean expandBox = structure.terrainAdaptation() != TerrainAdjustment.NONE;

			if (structureType != null && this.shouldSendStructure(structureType))
			{
				ChunkPos pos = entry.getKey();
				CompoundData nbt = DataConverterNbt.fromVanillaCompound(start.createTag(ctx, pos));

				// Should expand BB by 12
				// This is Needed for things like Pillager Outposts
				nbt.putBoolean("ExpandBox", expandBox);
				list.add(nbt);
			}
//            else
//            {
//                System.out.print("getStructureList: type = NULL\n");
//            }
		}

		return list;
	}

	protected boolean shouldSendStructure(Identifier identifier)
	{
//        System.out.printf("shouldSendStructure: [%s]\n", identifier.toString());

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
	public boolean hasPermission(ServerPlayer player)
	{
		return PermissionsUtil.check(player, this.permNode, this.permissionLevel.getValue());
	}
}
