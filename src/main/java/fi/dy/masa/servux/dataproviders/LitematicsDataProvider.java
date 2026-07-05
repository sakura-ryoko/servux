package fi.dy.masa.servux.dataproviders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.lucko.fabric.api.permissions.v0.Permissions;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxLitematicaHandler;
import fi.dy.masa.servux.network.packet.ServuxLitematicaPacket;
import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.schematic.placement.SchematicPlacement;
import fi.dy.masa.servux.schematic.transmit.SchematicBufferManager;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.util.*;
import fi.dy.masa.servux.util.nbt.NbtUtils;
import fi.dy.masa.servux.util.nbt.NbtView;
import fi.dy.masa.servux.util.position.PositionUtils;

public class LitematicsDataProvider extends DataProviderBase
{
    public static final LitematicsDataProvider INSTANCE = new LitematicsDataProvider();
	private final static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> HANDLER = ServuxLitematicaHandler.getInstance();
	private final CompoundTag metadata = new CompoundTag();
	private final ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
    private final ServuxIntSetting pastePermissionLevel = new ServuxIntSetting(this, "permission_level_paste", 0, 4, 0);
    public ServuxBoolSetting fixRaiLRotations = new ServuxBoolSetting(this, "fix_rail_rotations", true);
    public ServuxBoolSetting fixStairMirror = new ServuxBoolSetting(this, "fix_stairs_mirror", true);
    public ServuxBoolSetting fixChestMirror = new ServuxBoolSetting(this, "fix_chest_mirror", true);
    private final List<IServuxSetting<?>> settings = List.of(this.permissionLevel, this.pastePermissionLevel, this.fixRaiLRotations, this.fixStairMirror, this.fixChestMirror);

    private final List<UUID> registeredPlayers = new ArrayList<>();
    private final List<UUID> invalidPlayers = new ArrayList<>();
    private final SchematicBufferManager bufferManager = new SchematicBufferManager();
    private final Path transmitDir;

    protected LitematicsDataProvider()
    {
        super("litematic_data",
                ServuxLitematicaHandler.CHANNEL_ID,
                ServuxLitematicaPacket.PROTOCOL_VERSION,
                0, Reference.MOD_ID+ ".provider.litematic_data",
                "Litematics Data provider.");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);
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

    public void registerPlayer(ServerPlayer player)
    {
        if (!this.isEnabled()) return;

        if (!this.hasPermission(player))
        {
            // No Permission
            Servux.debugLog("litematic_data: Denying access for player {}, Insufficient Permissions", player.getName().tryCollapseToString());
            return;
        }

        Servux.debugLog("litematic_data: sendMetadata to player {}", player.getName().tryCollapseToString());

        this.registeredPlayers.add(player.getUUID());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.connection != null)
        {
            HANDLER.sendPlayPayload(player.connection, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.MetadataResponse(this.metadata)));
        }
    }

    public void onPacketFailure(ServerPlayer player)
    {
        this.setPlayerInvalid(player);
        this.registeredPlayers.remove(player.getUUID());
    }

    public void removePlayer(ServerPlayer player)
    {
        this.removeInvalidPlayer(player);
        this.registeredPlayers.remove(player.getUUID());
    }

    private void setPlayerInvalid(ServerPlayer player)
    {
        if (!this.invalidPlayers.contains(player.getUUID()))
        {
            this.invalidPlayers.add(player.getUUID());
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

    public void onBlockEntityRequest(ServerPlayer player, BlockPos pos)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("LitematicsDataProvider#onBlockEntityRequest(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.level().getBlockEntity(pos);
        CompoundTag nbt = be != null ? be.saveWithFullMetadata(player.registryAccess()) : new CompoundTag();
        HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayer player, int entityId)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("LitematicsDataProvider#onEntityRequest(): from player {} // entityId [{}]", player.getName().getLiteralString(), entityId);
        Entity entity = player.level().getEntity(entityId);

        if (entity != null)
        {
            NbtView view = NbtView.getWriter(player.level().registryAccess());
            Identifier id = EntityType.getKey(entity.getType());

            entity.saveWithoutId(view.getWriter());
            CompoundTag nbt = view.readNbt();

            if (nbt != null && id != null)
            {
                nbt.putString("id", id.toString());
                HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleEntityResponse(entityId, nbt));
            }
        }
    }

    public void onBulkEntityRequest(ServerPlayer player, ChunkPos chunkPos, CompoundTag req)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            Servux.LOGGER.warn("litematic_data: Denying Litematic onBulkEntityRequest from player {}, Insufficient Permissions.", player.getName().getString());
            player.sendSystemMessage(StringUtils.translate("servux.litematics.error.bulk_request.insufficent"));
            return;
        }
        if (req == null || req.isEmpty())
        {
//            Servux.LOGGER.warn("litematic_data: Litematic onBulkEntityRequest from player {}, request is empty.", player.getName().getString());
            return;
        }

        ServerLevel world = player.level();
        LevelChunk chunk = world != null ? world.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) : null;

        if (chunk == null)
        {
            player.sendSystemMessage(StringUtils.translate("servux.litematics.error.bulk_request.chunk_not_loaded", chunkPos.toString()));
            return;
        }

        // TODO --> Split out the task this way (I should have done this under 0.3.0),
        //  So we need to check if the "Task" is not included for now... (Wait for the updates to bake in)
        if ((req.contains("Task") && req.getStringOr("Task", "").equals("BulkEntityRequest")) ||
            !req.contains("Task"))
        {
            Servux.debugLog("litematic_data: Sending Bulk NBT Data for ChunkPos {} to player {}", chunkPos.toString(), player.getName().tryCollapseToString());

            long timeStart = System.currentTimeMillis();
            ListTag tileList = new ListTag();
            ListTag entityList = new ListTag();
            int minY = req.getIntOr("minY", -64);
            int maxY = req.getIntOr("maxY", 319);
            BlockPos pos1 = new BlockPos(chunkPos.getMinBlockX(), minY, chunkPos.getMinBlockZ());
            BlockPos pos2 = new BlockPos(chunkPos.getMaxBlockX(), maxY, chunkPos.getMaxBlockZ());
            net.minecraft.world.phys.AABB bb = PositionUtils.createEnclosingAABB(pos1, pos2);
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
                CompoundTag beTag = be != null ? be.saveWithFullMetadata(player.registryAccess()) : new CompoundTag();
                tileList.add(beTag);
            }

            for (Entity entity : entities)
            {
                NbtView view = NbtView.getWriter(player.level().registryAccess());
                Identifier id = EntityType.getKey(entity.getType());

                entity.saveWithoutId(view.getWriter());
                CompoundTag entTag = view.readNbt();

                if (entTag != null && id != null)
                {
                    Vec3 posVec = new Vec3(entity.getX() - pos1.getX(), entity.getY() - pos1.getY(), entity.getZ() - pos1.getZ());
                    entTag.putString("id", id.toString());

                    NbtUtils.writeEntityPositionToTag(posVec, entTag);
                    entTag.putInt("entityId", entity.getId());
                    entityList.add(entTag);
                }
            }

            CompoundTag output = new CompoundTag();
            output.putString("Task", "BulkEntityReply");
            output.put("TileEntities", tileList);
            output.put("Entities", entityList);
            output.putInt("chunkX", chunkPos.x);
            output.putInt("chunkZ", chunkPos.z);
            long timeElapsed = System.currentTimeMillis() - timeStart;

            HANDLER.encodeServerData(player, ServuxLitematicaPacket.ResponseS2CStart(output));
            player.sendSystemMessage(
                    StringUtils.translate("servux.litematics.feedback.bulk_request.acknowledge",
                                          world.dimension().identifier().toString(), chunkPos.toString(),
                                          tileList.size(), entityList.size(),
                                          timeElapsed), false
            );
        }
    }

    public void handleClientPasteRequest(ServerPlayer player, int transactionId, CompoundTag tags)
    {
        if (!this.isPlayerRegistered(player) || !this.isEnabled())
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

        if (tags.getStringOr("Task", "").equals("LitematicaPaste"))
        {
            Servux.debugLog("litematic_data: Servux Paste request from player {}", player.getName().tryCollapseToString());

            long timeStart = System.currentTimeMillis();
            SchematicPlacement placement = SchematicPlacement.createFromNbt(tags);
            ReplaceBehavior replaceMode = ReplaceBehavior.fromStringStatic(tags.getStringOr("ReplaceMode", ReplaceBehavior.NONE.name()));
            PasteLayerBehavior layerBehavior = PasteLayerBehavior.fromStringStatic(tags.getStringOr("PasteLayerBehavior", PasteLayerBehavior.ALL.name()));
            LayerRange layerRange = tags.read("RenderLayerRange", LayerRange.CODEC).orElse(null);
            placement.pasteTo(player.level(), replaceMode, layerBehavior, layerRange);
            long timeElapsed = System.currentTimeMillis() - timeStart;
            //player.sendMessage(Text.of("Pasted §b"+placement.getName()+"§r to world §d"+player.getServerWorld().getRegistryKey().getValue().toString()+"§r in §a"+timeElapsed+"§rms."), false);
            player.displayClientMessage(StringUtils.translate("servux.litematics.success.pasted", placement.getName(), player.level().dimension().identifier().toString(), timeElapsed), false);
        }
    }

    public void handleClientPasteRequestPair(ServerPlayer player, int transactionId, Pair<LitematicaSchematic, CompoundTag> schemPair)
    {
        if (!this.isPlayerRegistered(player) || !this.isEnabled())
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

        if (schemPair.getLeft() != null)
        {
            Servux.debugLog("litematic_data: Servux Paste (Pair) request from player {}", player.getName().tryCollapseToString());

            long timeStart = System.currentTimeMillis();
            CompoundTag tags = schemPair.getRight();
            SchematicPlacement placement = SchematicPlacement.createFromNbt(schemPair.getLeft(), tags);
            ReplaceBehavior replaceMode = ReplaceBehavior.fromStringStatic(tags.getStringOr("ReplaceMode", ReplaceBehavior.NONE.name()));
            PasteLayerBehavior layerBehavior = PasteLayerBehavior.fromStringStatic(tags.getStringOr("PasteLayerBehavior", PasteLayerBehavior.ALL.name()));
            LayerRange layerRange = tags.read("RenderLayerRange", LayerRange.CODEC).orElse(null);
            placement.pasteTo(player.level(), replaceMode, layerBehavior, layerRange);
            long timeElapsed = System.currentTimeMillis() - timeStart;
            //player.sendMessage(Text.of("Pasted §b"+placement.getName()+"§r to world §d"+player.getServerWorld().getRegistryKey().getValue().toString()+"§r in §a"+timeElapsed+"§rms."), false);
            player.displayClientMessage(StringUtils.translate("servux.litematics.success.pasted", placement.getName(), player.level().dimension().identifier().toString(), timeElapsed), false);
        }
    }

    @Override
    public boolean hasPermission(ServerPlayer player)
    {
        return Permissions.check(player, this.permNode, this.permissionLevel.getValue());
    }

	public boolean hasPermissionsForPaste(ServerPlayer player)
	{
		return this.hasPermission(player) && Permissions.check(player, this.permNode + ".paste", this.pastePermissionLevel.getValue());
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
