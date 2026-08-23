package fi.dy.masa.servux.dataproviders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxLitematicaHandler;
import fi.dy.masa.servux.network.packet.ServuxLitematicaPacket;
import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.scheduler.TaskScheduler;
import fi.dy.masa.servux.scheduler.tasks.TaskDeleteArea;
import fi.dy.masa.servux.scheduler.tasks.TaskFillArea;
import fi.dy.masa.servux.scheduler.tasks.TaskPasteSchematicPerChunkBase;
import fi.dy.masa.servux.scheduler.tasks.TaskPasteSchematicPerChunkDirect;
import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.schematic.placement.SchematicPlacement;
import fi.dy.masa.servux.schematic.selection.Box;
import fi.dy.masa.servux.schematic.transmit.SchematicBufferManager;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.util.PasteLayerBehavior;
import fi.dy.masa.servux.util.PermissionsUtil;
import fi.dy.masa.servux.util.ReplaceBehavior;
import fi.dy.masa.servux.util.StringUtils;
import fi.dy.masa.servux.util.data.Constants;
import fi.dy.masa.servux.util.data.tag.BaseData;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.ListData;
import fi.dy.masa.servux.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.servux.util.data.tag.util.DataOps;
import fi.dy.masa.servux.util.data.tag.util.DataTypeUtils;
import fi.dy.masa.servux.util.game.EntityUtils;
import fi.dy.masa.servux.util.nbt.NbtView;
import fi.dy.masa.servux.util.position.LayerRange;
import fi.dy.masa.servux.util.position.PositionUtils;

public class LitematicsDataProvider extends DataProviderBase
{
	public static final LitematicsDataProvider INSTANCE = new LitematicsDataProvider();
	private final static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> HANDLER = ServuxLitematicaHandler.getInstance();
	private final CompoundData metadata = new CompoundData();
	private final ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
	private final ServuxIntSetting pastePermissionLevel = new ServuxIntSetting(this, "permission_level_paste", 0, 4, 0);
	private final ServuxIntSetting taskPermissionLevel = new ServuxIntSetting(this, "permission_level_tasks", 0, 4, 0);
	private final ServuxBoolSetting playerTaskFeedback = new ServuxBoolSetting(this, "player_task_feedback", false);
	public final ServuxBoolSetting fixRaiLRotations = new ServuxBoolSetting(this, "fix_rail_rotations", true);
	public final ServuxBoolSetting fixStairMirror = new ServuxBoolSetting(this, "fix_stairs_mirror", true);
	public final ServuxBoolSetting fixChestMirror = new ServuxBoolSetting(this, "fix_chest_mirror", true);
	private final List<IServuxSetting<?>> settings = List.of(
			this.permissionLevel,
			this.pastePermissionLevel,
			this.taskPermissionLevel,
			this.playerTaskFeedback,
			this.fixRaiLRotations,
			this.fixStairMirror,
			this.fixChestMirror
	);

	private final List<UUID> registeredPlayers = new ArrayList<>();
	private final List<UUID> invalidPlayers = new ArrayList<>();
	private final SchematicBufferManager bufferManager = new SchematicBufferManager();
	private final Path transmitDir;

	protected LitematicsDataProvider()
	{
		super("litematic_data",
		      ServuxLitematicaHandler.CHANNEL_ID,
		      ServuxLitematicaPacket.PROTOCOL_VERSION,
		      0, Reference.MOD_ID + ".provider.litematic_data",
		      "Litematics Data provider.");

		this.metadata.putString("name", this.getName());
		this.metadata.putString("id", this.getNetworkChannel().toString());
		this.metadata.putInt("version", this.getProtocolVersion());
		this.metadata.putString("servux", Reference.MOD_STRING);

		// Litematic-Transmit Dir
		this.transmitDir = this.getTransmitDir();
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
			HANDLER.registerPlayPayload(ServuxLitematicaPacket.Payload.ID, ServuxLitematicaPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
			this.setRegistered(true);
		}

		HANDLER.registerPlayReceiver(ServuxLitematicaPacket.Payload.ID, HANDLER::receivePlayPayload);
	}

	@Override
	public void unregisterHandler()
	{
		HANDLER.unregisterPlayReceiver();
		ServerPlayHandler.getInstance().unregisterServerPlayHandler(HANDLER);
	}

	@Override
	public IPluginServerPlayHandler<?> getPacketHandler()
	{
		return HANDLER;
	}

	public SchematicBufferManager getBufferManager()
	{
		return this.bufferManager;
	}

	public Path getTransmitDir()
	{
		Path dir = this.transmitDir != null ? this.transmitDir : Reference.DEFAULT_RUN_DIR.resolve("schematics").normalize();

		if (!Files.exists(dir) || !Files.isDirectory(dir))
		{
			try
			{
				if (Files.exists(dir))
				{
					Files.delete(dir);
				}

				Files.createDirectory(dir);
				Servux.LOGGER.warn("getTransmitDir(): Created schematic transmit directory '{}'", dir.toAbsolutePath().toString());
			}
			catch (IOException err)
			{
				Servux.LOGGER.error("getTransmitDir(): Fatal exception creating schematic transmit dir '{}'; {}", dir.toAbsolutePath().toString(), err.getLocalizedMessage());
				throw new RuntimeException(err);
			}
		}

		if (!Files.isWritable(dir))
		{
			Servux.LOGGER.error("Schematic transmit directory '{}'; is not writeable.", dir.toAbsolutePath().toString());
		}

		Servux.debugLog("getTransmitDir(): Schematic transmit directory debug '{}'", dir.toAbsolutePath().toString());

		return dir;
	}

	@Override
	public boolean isPlayerRegistered(ServerPlayer player)
	{
		return this.registeredPlayers.contains(player.getUUID()) && !this.isPlayerInvalid(player);
	}

	@Override
	public void register(ServerPlayer player, CompoundData tags)
	{
		if (!this.isEnabled()) { return; }
		UUID uuid = player.getUUID();

		if (tags == null || tags.getIntOrDefault("version", -1) < this.getProtocolVersion())
		{
			Servux.LOGGER.warn("litematic_data: Denying access for player {}, Insufficient Protocol Version; This Server Requires: Version {}", player.getName().tryCollapseToString(), this.getProtocolVersion());
			player.sendSystemMessage(StringUtils.translate("servux.general.error.protocol_version_too_low", this.getName()));
			HANDLER.tickFailures(player);
			return;
		}

		if (!this.hasPermission(player))
		{
			// No Permission
			Servux.debugLog("litematic_data: Denying access for player {}, Insufficient Permissions", player.getName().tryCollapseToString());
			return;
		}

		CompoundData nbt = new CompoundData();
		nbt.combine(this.metadata);

		Servux.debugLog("litematic_data: sendMetadata to player {}", player.getName().tryCollapseToString());
		this.registeredPlayers.add(uuid);

		// Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
		if (player.connection != null)
		{
			HANDLER.sendPlayPayload(player.connection, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.MetadataResponse(nbt)));
		}
		else
		{
			HANDLER.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.MetadataResponse(nbt)));
		}
	}

	@Override
	public void unregister(ServerPlayer player, @Nullable CompoundData tags)
	{
		if (this.isEnabled())
		{
			Servux.debugLog("litematic_data: Unregistered player {}", player.getName().tryCollapseToString());
		}

		UUID uuid = player.getUUID();

		HANDLER.resetFailures(this.getNetworkChannel(), player);
		this.getBufferManager().removePlayer(player);
		this.registeredPlayers.remove(uuid);
	}

	@Override
	public void onPacketFailure(ServerPlayer player)
	{
		UUID uuid = player.getUUID();
		this.setPlayerInvalid(player);
		this.registeredPlayers.remove(uuid);
	}

	@Override
	public void removePlayer(ServerPlayer player)
	{
		UUID uuid = player.getUUID();
		this.removeInvalidPlayer(player);
		this.registeredPlayers.remove(uuid);
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

	@ApiStatus.Experimental
	public void onTaskRequest(ServerPlayer player, CompoundData tags)
	{
		if (!this.isPlayerRegistered(player) || !this.isEnabled() || tags == null || tags.isEmpty())
		{
			return;
		}

		if (!this.hasPermission(player))
		{
			Servux.debugLog("litematic_data: Denying onTaskRequest from player {}, Insufficient Permissions.", player.getName().getString());
			player.sendSystemMessage(StringUtils.translate("servux.litematics.error.insufficent_for_tasks"));

			return;
		}

		final String taskType = tags.getStringOrDefault("Task", "");
		final long timeStart = System.currentTimeMillis();
		ServerLevel level = player.level();
		Servux.debugLog("litematic_data: Received TaskRequest from player {} of type: [{}]", player.getName().getString(), taskType);

		switch (taskType)
		{
			case "Fill" ->
			{
				if (!this.hasPermissionsForTask(player, "fill"))
				{
					Servux.debugLog("litematic_data: Denying onTaskRequest from player {}, Insufficient Permissions for Fill Task.", player.getName().getString());
					player.sendSystemMessage(StringUtils.translate("servux.litematics.error.insufficent_for_tasks"));

					return;
				}

				if (!player.isCreative())
				{
					Servux.debugLog("litematic_data: Denying Litematic Task Request for player {}, Player is not in Creative Mode.", player.getName().tryCollapseToString());
					player.sendSystemMessage(StringUtils.translate("servux.litematics.error.creative_required_for_task"));
					return;
				}

				ListData list = tags.getListOrDefault("Boxes", Constants.NBT.TAG_COMPOUND, new ListData());
				List<Box> boxes = new ArrayList<>();

				for (int i = 0; i < list.size(); i++)
				{
					BaseData entry = list.get(i);

					if (entry != null && !entry.isEmpty())
					{
						Box.CODEC.parse(DataOps.INSTANCE, entry).resultOrPartial().ifPresent(boxes::add);
					}
				}

				BlockState fillState = tags.getCodec("FillState", BlockState.CODEC).orElse(null);

				if (fillState == null)
				{
					if (this.shouldSendPlayerTaskFeedback())
					{
						player.sendSystemMessage(StringUtils.translate("servux.litematics.task.fill_area.no_fill_state"));
					}

					return;
				}

				if (boxes.isEmpty())
				{
					if (this.shouldSendPlayerTaskFeedback())
					{
						player.sendSystemMessage(StringUtils.translate("servux.litematics.task.fill_area.no_boxes"));
					}

					return;
				}

				final BlockState replaceState = tags.getCodec("ReplaceState", BlockState.CODEC).orElse(null);
				final boolean removeEntities = tags.getBooleanOrDefault("RemoveEntities", false);
				final int interval = tags.getIntOrDefault("Interval", 1);
				TaskContext ctx = new TaskContext(level.getServer(), level, player, "Fill", timeStart);
				TaskFillArea task = new TaskFillArea(ctx, boxes, fillState, replaceState, removeEntities);
				TaskScheduler.getInstance().scheduleTask(task, interval);
			}
			case "Delete" ->
			{
				if (!this.hasPermissionsForTask(player, "delete"))
				{
					Servux.debugLog("litematic_data: Denying onTaskRequest from player {}, Insufficient Permissions for Delete Task", player.getName().getString());
					player.sendSystemMessage(StringUtils.translate("servux.litematics.error.insufficent_for_tasks"));

					return;
				}

				if (!player.isCreative())
				{
					Servux.debugLog("litematic_data: Denying Litematic Task Request for player {}, Player is not in Creative Mode.", player.getName().tryCollapseToString());
					player.sendSystemMessage(StringUtils.translate("servux.litematics.error.creative_required_for_task"));
					return;
				}

				ListData list = tags.getListOrDefault("Boxes", Constants.NBT.TAG_COMPOUND, new ListData());
				List<Box> boxes = new ArrayList<>();

				for (int i = 0; i < list.size(); i++)
				{
					BaseData entry = list.get(i);

					if (entry != null && !entry.isEmpty())
					{
						Box.CODEC.parse(DataOps.INSTANCE, entry).resultOrPartial().ifPresent(boxes::add);
					}
				}

				if (boxes.isEmpty())
				{
					if (this.shouldSendPlayerTaskFeedback())
					{
						player.sendSystemMessage(StringUtils.translate("servux.litematics.task.fill_area.no_boxes"));
					}

					return;
				}

				final boolean removeEntities = tags.getBooleanOrDefault("RemoveEntities", false);
				final int interval = tags.getIntOrDefault("Interval", 1);
				TaskContext ctx = new TaskContext(level.getServer(), level, player, "Delete", timeStart);
				TaskDeleteArea task = new TaskDeleteArea(ctx, boxes, removeEntities);
				TaskScheduler.getInstance().scheduleTask(task, interval);
			}
			// TODO (Ensure Safe Transmit)
//			case "Save" ->
//			{
//				AreaSelection selection = tags.getCodec("AreaSelection", AreaSelection.CODEC).orElse(null);
//				final boolean visibleOnly = tags.getBooleanOrDefault("VisibleOnly", false);
//				final boolean ignoreEntities = tags.getBooleanOrDefault("IgnoreEntities", false);
//
//				if (selection == null)
//				{
//					if (this.shouldSendPlayerTaskFeedback())
//					{
//						player.sendSystemMessage(StringUtils.translate("servux.litematics.task.save.no_area_selection"));
//					}
//
//					return;
//				}
//
//				final String fileName = UUID.randomUUID().toString() + ".litematic";
//				final LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(visibleOnly, ignoreEntities);
//				LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(selection, player.getName().getString());
//
//				Runnable whenDone = () ->
//				{
//				};
//
//				TaskContext ctx = new TaskContext(level.getServer(), level, player, "Save", timeStart, whenDone);
//				TaskSaveSchematic task = new TaskSaveSchematic(ctx, this.transmitDir, fileName, schematic, selection, info, false);
//				TaskScheduler.getInstance().scheduleTask(task, 1);
//			}
			default ->
			{
				if (this.shouldSendPlayerTaskFeedback())
				{
					player.sendSystemMessage(StringUtils.translate("servux.litematics.task.invalid"));
				}
			}
		}
	}

	public void onTaskStatusSync(ServerPlayer player, CompoundData tags)
	{
		if (!this.isPlayerRegistered(player) || !this.isEnabled() ||
			tags == null || tags.isEmpty())
		{
			return;
		}

		if (!this.hasPermission(player))
		{
			Servux.debugLog("litematic_data: Denying onTaskStatusSync to player {}, Insufficient Permissions.", player.getName().getString());
			return;
		}

		HANDLER.encodeServerData(player, ServuxLitematicaPacket.TaskStatusSync(tags));
	}

	@ApiStatus.Experimental
	public void onTaskCancel(ServerPlayer player, CompoundData tags)
	{
		if (!this.isPlayerRegistered(player) || !this.isEnabled() || tags == null || tags.isEmpty())
		{
			return;
		}

		if (!this.hasPermission(player))
		{
			Servux.debugLog("litematic_data: Denying onTaskCancel from player {}, Insufficient Permissions.", player.getName().getString());
			return;
		}

		// TODO (For things like Delete, Fill, etc)
	}

	public void onBlockEntityRequest(ServerPlayer player, BlockPos pos, @Nullable CompoundData tags)
	{
		if (!this.isPlayerRegistered(player) || !this.isEnabled())
		{
			return;
		}

		if (!this.hasPermission(player))
		{
			Servux.debugLog("litematic_data: Denying onBlockEntityRequest from player {}, Insufficient Permissions.", player.getName().getString());
			return;
		}

		//Servux.logger.warn("LitematicsDataProvider#onBlockEntityRequest(): from player {}", player.getName().getLiteralString());
		BlockEntity be = player.level().getBlockEntity(pos);

		if (be != null)
		{
			CompoundData nbt = DataConverterNbt.fromVanillaCompound(be.saveWithFullMetadata(player.registryAccess()));
			HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleBlockResponse(pos, nbt));
		}
	}

	public void onEntityRequest(ServerPlayer player, int entityId, @Nullable CompoundData tags)
	{
		if (!this.isPlayerRegistered(player) || !this.isEnabled())
		{
			return;
		}

		if (!this.hasPermission(player))
		{
			Servux.debugLog("litematic_data: Denying onEntityRequest from player {}, Insufficient Permissions.", player.getName().getString());
			return;
		}

		//Servux.logger.warn("LitematicsDataProvider#onEntityRequest(): from player {} // entityId [{}]", player.getName().getLiteralString(), entityId);
		Entity entity = player.level().getEntity(entityId);

		if (entity != null)
		{
			NbtView view = NbtView.getWriter(player.level().registryAccess());
			Identifier id = EntityType.getKey(entity.getType());

			entity.saveWithoutId(view.getWriter());
			CompoundData nbt = view.readData();

			if (nbt != null && id != null)
			{
				if (entity.getType() == EntityType.PLAYER && !entity.getUUID().equals(player.getUUID()))
				{
					if (!EntitiesDataProvider.INSTANCE.hasPlayerInventoryPermission(player))
					{
						nbt.remove("Inventory");
						nbt.put("Inventory", new ListData());
					}
					if (!EntitiesDataProvider.INSTANCE.hasPlayerEnderItemsPermission(player))
					{
						nbt.remove("EnderItems");
						nbt.put("EnderItems", new ListData());
					}
				}

				nbt.putString("id", id.toString());
				HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleEntityResponse(entityId, nbt));
			}
		}
	}

	public void onBulkEntityRequest(ServerPlayer player, ChunkPos chunkPos, CompoundData req)
	{
		if (!this.isPlayerRegistered(player) || !this.isEnabled() || req == null || req.isEmpty())
		{
			return;
		}

		if (!this.hasPermission(player))
		{
			Servux.LOGGER.warn("litematic_data: Denying Litematic onBulkEntityRequest from player {}, Insufficient Permissions.", player.getName().getString());
			player.sendSystemMessage(StringUtils.translate("servux.litematics.error.bulk_request.insufficent"));
			return;
		}

		UUID uuid = player.getUUID();
		ServerLevel world = player.level();
		LevelChunk chunk = world != null ? world.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z()) : null;

		if (chunk == null)
		{
			if (this.shouldSendPlayerTaskFeedback())
			{
				player.sendSystemMessage(StringUtils.translate("servux.litematics.error.bulk_request.chunk_not_loaded", chunkPos.toString()));
			}

			return;
		}

		if ((req.contains("Task", Constants.NBT.TAG_STRING) &&
			req.getStringOrDefault("Task", "").equals("BulkEntityRequest")))
		{
			Servux.debugLog("litematic_data: Sending Bulk NBT Data for ChunkPos {} to player {}", chunkPos.toString(), player.getName().tryCollapseToString());
			final long timeStart = System.currentTimeMillis();
			ListData tileList = new ListData();
			ListData entityList = new ListData();
			final int minY = req.getIntOrDefault("minY", world.getMinY());
			final int maxY = req.getIntOrDefault("maxY", world.getMaxY());
			final BlockPos pos1 = new BlockPos(chunkPos.getMinBlockX(), minY, chunkPos.getMinBlockZ());
			final BlockPos pos2 = new BlockPos(chunkPos.getMaxBlockX(), maxY, chunkPos.getMaxBlockZ());
			AABB bb = PositionUtils.createEnclosingAABB(pos1, pos2);
			Set<BlockPos> teSet = chunk.getBlockEntitiesPos();
			List<Entity> entities = world.getEntities((Entity) null, bb, EntityUtils.NOT_PLAYER);

			for (BlockPos tePos : teSet)
			{
				if ((tePos.getX() < chunkPos.getMinBlockX() || tePos.getX() > chunkPos.getMaxBlockX()) ||
					(tePos.getZ() < chunkPos.getMinBlockZ() || tePos.getZ() > chunkPos.getMaxBlockZ()) ||
					(tePos.getY() < minY || tePos.getY() > maxY))
				{
					continue;
				}

				BlockEntity be = world.getBlockEntity(tePos);

				if (be != null)
				{
					CompoundData beTag = DataConverterNbt.fromVanillaCompound(be.saveWithFullMetadata(player.registryAccess()));
					tileList.add(beTag);
				}
			}

			for (Entity entity : entities)
			{
				NbtView view = NbtView.getWriter(player.level().registryAccess());
				Identifier id = EntityType.getKey(entity.getType());

				entity.saveWithoutId(view.getWriter());
				CompoundData entTag = view.readData();

				if (entTag != null && id != null)
				{
					Vec3 posVec = new Vec3(entity.getX() - pos1.getX(), entity.getY() - pos1.getY(), entity.getZ() - pos1.getZ());
					entTag.putString("id", id.toString());

//					NbtUtils.writeEntityPositionToTag(posVec, entTag);
					DataTypeUtils.writeVec3dToListTag(entTag, posVec);
					entTag.putInt("entityId", entity.getId());
					entityList.add(entTag);
				}
			}

			CompoundData output = new CompoundData();

			output.putString("Task", "BulkEntityReply");
			output.put("TileEntities", tileList.copy());
			output.put("Entities", entityList.copy());
			output.putInt("chunkX", chunkPos.x());
			output.putInt("chunkZ", chunkPos.z());

			HANDLER.encodeServerData(player, ServuxLitematicaPacket.ResponseS2CStart(output));

			if (this.shouldSendPlayerTaskFeedback())
			{
				final long timeElapsed = System.currentTimeMillis() - timeStart;
				player.sendSystemMessage(
						StringUtils.translate("servux.litematics.feedback.bulk_request.acknowledge",
						                      world.dimension().identifier().toString(), chunkPos.toString(),
						                      tileList.size(), entityList.size(),
						                      timeElapsed), false
				);
			}
		}
	}

	public void handleClientPasteRequest(ServerPlayer player, CompoundData tags)
	{
		if (!this.isPlayerRegistered(player) || !this.isEnabled() || tags == null || tags.isEmpty())
		{
			return;
		}

		if (!this.hasPermission(player) || !this.hasPermissionsForPaste(player))
		{
			Servux.debugLog("litematic_data: Denying Litematic Paste for player {}, Insufficient Permissions.", player.getName().tryCollapseToString());
			player.sendSystemMessage(StringUtils.translate("servux.litematics.error.insufficent_for_paste"));
			return;
		}
		if (!player.isCreative())
		{
			Servux.debugLog("litematic_data: Denying Litematic Paste for player {}, Player is not in Creative Mode.", player.getName().tryCollapseToString());
			player.sendSystemMessage(StringUtils.translate("servux.litematics.error.creative_required"));
			return;
		}

		if (tags.getStringOrDefault("Task", "").equals("LitematicaPaste"))
		{
			Servux.debugLog("litematic_data: Servux Paste request from player {}", player.getName().tryCollapseToString());
			final long timeStart = System.currentTimeMillis();
			SchematicPlacement placement = SchematicPlacement.createFromData(tags);
			ReplaceBehavior replaceMode = ReplaceBehavior.fromStringStatic(tags.getStringOrDefault("ReplaceMode", ReplaceBehavior.NONE.name()));
			PasteLayerBehavior layerBehavior = PasteLayerBehavior.fromStringStatic(tags.getStringOrDefault("PasteLayerBehavior", PasteLayerBehavior.ALL.name()));
			LayerRange layerRange = tags.getCodec("RenderLayerRange", LayerRange.CODEC).orElse(null);
			final boolean changedBlocksOnly = tags.getBooleanOrDefault("ChangedBlocksOnly", false);
			final boolean ignoreBlocks = tags.getBooleanOrDefault("IgnoreBlocks", false);
			final boolean ignoreEntities = tags.getBooleanOrDefault("IgnoreEntities", false);
			final int interval = tags.getIntOrDefault("Interval", 1);
			ServerLevel level = player.level();

			// New Task Scheduler Paste
			TaskContext ctx = new TaskContext(level.getServer(), level, player, placement.getName(), timeStart);
			TaskPasteSchematicPerChunkBase task = new TaskPasteSchematicPerChunkDirect(ctx, Collections.singletonList(placement), layerRange, replaceMode, layerBehavior, changedBlocksOnly, ignoreBlocks, ignoreEntities);
			TaskScheduler.getInstance().scheduleTask(task, interval);
//				placement.pasteTo(level, replaceMode, layerBehavior, layerRange);

//			if (this.shouldSendPlayerTaskFeedback())
//			{
//				final long timeElapsed = System.currentTimeMillis() - timeStart;
//				player.sendSystemMessage(StringUtils.translate("servux.litematics.success.pasted", placement.getName(), player.level().dimension().identifier().toString(), timeElapsed), false);
//			}
		}
	}

	public void handleClientPasteRequestPair(ServerPlayer player, Pair<LitematicaSchematic, CompoundData> schemPair)
	{
		if (!this.isPlayerRegistered(player) || !this.isEnabled() ||
			schemPair == null || schemPair.getLeft() == null ||
			schemPair.getRight() == null || schemPair.getRight().isEmpty())
		{
			return;
		}

		if (!this.hasPermission(player) || !this.hasPermissionsForPaste(player))
		{
			Servux.debugLog("litematic_data: Denying Litematic Paste for player {}, Insufficient Permissions.", player.getName().tryCollapseToString());
			player.sendSystemMessage(StringUtils.translate("servux.litematics.error.insufficent_for_paste"));
			return;
		}
		if (!player.isCreative())
		{
			Servux.debugLog("litematic_data: Denying Litematic Paste for player {}, Player is not in Creative Mode.", player.getName().tryCollapseToString());
			player.sendSystemMessage(StringUtils.translate("servux.litematics.error.creative_required"));
			return;
		}

		CompoundData tags = schemPair.getRight();

		if (schemPair.getLeft() != null)
		{
			Servux.debugLog("litematic_data: Servux Paste (Pair) request from player {}", player.getName().tryCollapseToString());
			final long timeStart = System.currentTimeMillis();
			SchematicPlacement placement = SchematicPlacement.createFromData(schemPair.getLeft(), tags);
			ReplaceBehavior replaceMode = ReplaceBehavior.fromStringStatic(tags.getStringOrDefault("ReplaceMode", ReplaceBehavior.NONE.name()));
			PasteLayerBehavior layerBehavior = PasteLayerBehavior.fromStringStatic(tags.getStringOrDefault("PasteLayerBehavior", PasteLayerBehavior.ALL.name()));
			LayerRange layerRange = tags.getCodec("RenderLayerRange", LayerRange.CODEC).orElse(null);
			final boolean changedBlocksOnly = tags.getBooleanOrDefault("ChangedBlocksOnly", false);
			final boolean ignoreBlocks = tags.getBooleanOrDefault("IgnoreBlocks", false);
			final boolean ignoreEntities = tags.getBooleanOrDefault("IgnoreEntities", false);
			final int interval = tags.getIntOrDefault("Interval", 1);
			ServerLevel level = player.level();

			// New Task Scheduler Paste
			TaskContext ctx = new TaskContext(level.getServer(), level, player, placement.getName(), timeStart);
			TaskPasteSchematicPerChunkBase task = new TaskPasteSchematicPerChunkDirect(ctx, Collections.singletonList(placement), layerRange, replaceMode, layerBehavior, changedBlocksOnly, ignoreBlocks, ignoreEntities);
			TaskScheduler.getInstance().scheduleTask(task, interval);
//			placement.pasteTo(level, replaceMode, layerBehavior, layerRange);

//			if (this.shouldSendPlayerTaskFeedback())
//			{
//				final long timeElapsed = System.currentTimeMillis() - timeStart;
//				player.sendSystemMessage(StringUtils.translate("servux.litematics.success.pasted", placement.getName(), player.level().dimension().identifier().toString(), timeElapsed), false);
//			}
		}
		else
		{
			// LitematicaSchematic == null could also be sus ?
			Servux.LOGGER.warn("handleClientPasteRequestPair: Error; Litematic provided by '{}' was null.", player.getName().tryCollapseToString());

			if (this.shouldSendPlayerTaskFeedback())
			{
				player.sendSystemMessage(StringUtils.translate("servux.litematics.error.pasting"), false);
			}
		}
	}

	@Override
	public boolean hasPermission(ServerPlayer player)
	{
		return PermissionsUtil.check(player, this.permNode, this.permissionLevel.getValue());
	}

	public boolean hasPermissionsForPaste(ServerPlayer player)
	{
		return this.hasPermission(player) && PermissionsUtil.check(player, this.permNode + ".paste", this.pastePermissionLevel.getValue());
	}

	public boolean hasPermissionsForTask(ServerPlayer player, String task)
	{
		return this.hasPermission(player) && PermissionsUtil.check(player, this.permNode + ".task." + task, this.taskPermissionLevel.getValue());
	}

	public boolean shouldSendPlayerTaskFeedback()
	{
		return this.playerTaskFeedback.getValue();
	}
}
