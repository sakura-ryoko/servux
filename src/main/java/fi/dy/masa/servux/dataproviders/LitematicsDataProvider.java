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

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

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
	private final NbtCompound metadata = new NbtCompound();
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
    public boolean isPlayerRegistered(ServerPlayerEntity player)
    {
        return this.registeredPlayers.contains(player.getUuid()) && !this.isPlayerInvalid(player);
    }

    public void registerPlayer(ServerPlayerEntity player)
    {
        if (!this.isEnabled()) return;

        if (!this.hasPermission(player))
        {
            // No Permission
            Servux.debugLog("litematic_data: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        Servux.debugLog("litematic_data: sendMetadata to player {}", player.getName().getLiteralString());

        this.registeredPlayers.add(player.getUuid());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.networkHandler != null)
        {
            HANDLER.sendPlayPayload(player.networkHandler, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.MetadataResponse(this.metadata)));
        }
    }

    public void onPacketFailure(ServerPlayerEntity player)
    {
        this.setPlayerInvalid(player);
        this.registeredPlayers.remove(player.getUuid());
    }

    public void removePlayer(ServerPlayerEntity player)
    {
        this.removeInvalidPlayer(player);
        this.registeredPlayers.remove(player.getUuid());
    }

    private void setPlayerInvalid(ServerPlayerEntity player)
    {
        if (!this.invalidPlayers.contains(player.getUuid()))
        {
            this.invalidPlayers.add(player.getUuid());
        }
    }

    private boolean isPlayerInvalid(ServerPlayerEntity player)
    {
        return this.invalidPlayers.contains(player.getUuid());
    }

    private void removeInvalidPlayer(ServerPlayerEntity player)
    {
        this.invalidPlayers.remove(player.getUuid());
    }

    public void onBlockEntityRequest(ServerPlayerEntity player, BlockPos pos)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("LitematicsDataProvider#onBlockEntityRequest(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.getEntityWorld().getBlockEntity(pos);
        NbtCompound nbt = be != null ? be.createNbtWithIdentifyingData(player.getRegistryManager()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayerEntity player, int entityId)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        //Servux.logger.warn("LitematicsDataProvider#onEntityRequest(): from player {} // entityId [{}]", player.getName().getLiteralString(), entityId);
        Entity entity = player.getEntityWorld().getEntityById(entityId);

        if (entity != null)
        {
            NbtView view = NbtView.getWriter(player.getEntityWorld().getRegistryManager());
            Identifier id = EntityType.getId(entity.getType());

            entity.writeData(view.getWriter());
            NbtCompound nbt = view.readNbt();

            if (nbt != null && id != null)
            {
                nbt.putString("id", id.toString());
                HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleEntityResponse(entityId, nbt));
            }
        }
    }

    public void onBulkEntityRequest(ServerPlayerEntity player, ChunkPos chunkPos, NbtCompound req)
    {
        if (!this.hasPermission(player) || !this.isPlayerRegistered(player) || !this.isEnabled())
        {
            Servux.LOGGER.warn("litematic_data: Denying Litematic onBulkEntityRequest from player {}, Insufficient Permissions.", player.getName().getString());
            player.sendMessage(StringUtils.translate("servux.litematics.error.bulk_request.insufficent"));
            return;
        }
        if (req == null || req.isEmpty())
        {
//            Servux.LOGGER.warn("litematic_data: Litematic onBulkEntityRequest from player {}, request is empty.", player.getName().getString());
            return;
        }

        ServerWorld world = player.getEntityWorld();
        Chunk chunk = world != null ? world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) : null;

        if (chunk == null)
        {
            player.sendMessage(StringUtils.translate("servux.litematics.error.bulk_request.chunk_not_loaded", chunkPos.toString()));
            return;
        }

        // TODO --> Split out the task this way (I should have done this under 0.3.0),
        //  So we need to check if the "Task" is not included for now... (Wait for the updates to bake in)
        if ((req.contains("Task") && req.getString("Task", "").equals("BulkEntityRequest")) ||
            !req.contains("Task"))
        {
            Servux.debugLog("litematic_data: Sending Bulk NBT Data for ChunkPos {} to player {}", chunkPos.toString(), player.getName().getString());

            long timeStart = System.currentTimeMillis();
            NbtList tileList = new NbtList();
            NbtList entityList = new NbtList();
            int minY = req.getInt("minY", -64);
            int maxY = req.getInt("maxY", 319);
            BlockPos pos1 = new BlockPos(chunkPos.getStartX(), minY, chunkPos.getStartZ());
            BlockPos pos2 = new BlockPos(chunkPos.getEndX(), maxY, chunkPos.getEndZ());
            net.minecraft.util.math.Box bb = PositionUtils.createEnclosingAABB(pos1, pos2);
            Set<BlockPos> teSet = chunk.getBlockEntityPositions();
            List<Entity> entities = world.getOtherEntities(null, bb, EntityUtils.NOT_PLAYER);

            for (BlockPos tePos : teSet)
            {
                if ((tePos.getX() < chunkPos.getStartX() || tePos.getX() > chunkPos.getEndX()) ||
                    (tePos.getZ() < chunkPos.getStartZ() || tePos.getZ() > chunkPos.getEndZ()) ||
                    (tePos.getY() < minY || tePos.getY() > maxY))
                {
                    continue;
                }

                BlockEntity be = world.getBlockEntity(tePos);
                NbtCompound beTag = be != null ? be.createNbtWithIdentifyingData(player.getRegistryManager()) : new NbtCompound();
                tileList.add(beTag);
            }

            for (Entity entity : entities)
            {
                NbtView view = NbtView.getWriter(player.getEntityWorld().getRegistryManager());
                Identifier id = EntityType.getId(entity.getType());

                entity.writeData(view.getWriter());
                NbtCompound entTag = view.readNbt();

                if (entTag != null && id != null)
                {
                    Vec3d posVec = new Vec3d(entity.getX() - pos1.getX(), entity.getY() - pos1.getY(), entity.getZ() - pos1.getZ());
                    entTag.putString("id", id.toString());

                    NbtUtils.writeEntityPositionToTag(posVec, entTag);
                    entTag.putInt("entityId", entity.getId());
                    entityList.add(entTag);
                }
            }

            NbtCompound output = new NbtCompound();
            output.putString("Task", "BulkEntityReply");
            output.put("TileEntities", tileList);
            output.put("Entities", entityList);
            output.putInt("chunkX", chunkPos.x);
            output.putInt("chunkZ", chunkPos.z);
            long timeElapsed = System.currentTimeMillis() - timeStart;

            HANDLER.encodeServerData(player, ServuxLitematicaPacket.ResponseS2CStart(output));
            player.sendMessage(
                    StringUtils.translate("servux.litematics.feedback.bulk_request.acknowledge",
                                          world.getDimensionEntry().getIdAsString(), chunkPos.toString(),
                                          tileList.size(), entityList.size(),
                                          timeElapsed), false
            );
        }
    }

    public void handleClientPasteRequest(ServerPlayerEntity player, int transactionId, NbtCompound tags)
    {
        if (!this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        if (!this.hasPermission(player) || !this.hasPermissionsForPaste(player))
        {
            Servux.debugLog("litematic_data: Denying Litematic Paste for player {}, Insufficient Permissions.", player.getName().getLiteralString());
            player.sendMessage(StringUtils.translate("servux.litematics.error.insufficent_for_paste"));
            return;
        }
        if (!player.isCreative())
        {
            Servux.debugLog("litematic_data: Denying Litematic Paste for player {}, Player is not in Creative Mode.", player.getName().getLiteralString());
            player.sendMessage(StringUtils.translate("servux.litematics.error.creative_required"));
            return;
        }

        if (tags.getString("Task", "").equals("LitematicaPaste"))
        {
            Servux.debugLog("litematic_data: Servux Paste request from player {}", player.getName().getLiteralString());

            long timeStart = System.currentTimeMillis();
            SchematicPlacement placement = SchematicPlacement.createFromNbt(tags);
            ReplaceBehavior replaceMode = ReplaceBehavior.fromStringStatic(tags.getString("ReplaceMode", ReplaceBehavior.NONE.name()));
            PasteLayerBehavior layerBehavior = PasteLayerBehavior.fromStringStatic(tags.getString("PasteLayerBehavior", PasteLayerBehavior.ALL.name()));
            LayerRange layerRange = tags.get("RenderLayerRange", LayerRange.CODEC).orElse(null);
            placement.pasteTo(player.getEntityWorld(), replaceMode, layerBehavior, layerRange);
            long timeElapsed = System.currentTimeMillis() - timeStart;
            //player.sendMessage(Text.of("Pasted §b"+placement.getName()+"§r to world §d"+player.getServerWorld().getRegistryKey().getValue().toString()+"§r in §a"+timeElapsed+"§rms."), false);
            player.sendMessage(StringUtils.translate("servux.litematics.success.pasted", placement.getName(), player.getEntityWorld().getRegistryKey().getValue().toString(), timeElapsed), false);
        }
    }

    public void handleClientPasteRequestPair(ServerPlayerEntity player, int transactionId, Pair<LitematicaSchematic, NbtCompound> schemPair)
    {
        if (!this.isPlayerRegistered(player) || !this.isEnabled())
        {
            return;
        }

        if (!this.hasPermission(player) || !this.hasPermissionsForPaste(player))
        {
            Servux.debugLog("litematic_data: Denying Litematic Paste for player {}, Insufficient Permissions.", player.getName().getLiteralString());
            player.sendMessage(StringUtils.translate("servux.litematics.error.insufficent_for_paste"));
            return;
        }
        if (!player.isCreative())
        {
            Servux.debugLog("litematic_data: Denying Litematic Paste for player {}, Player is not in Creative Mode.", player.getName().getLiteralString());
            player.sendMessage(StringUtils.translate("servux.litematics.error.creative_required"));
            return;
        }

        if (schemPair.getLeft() != null)
        {
            Servux.debugLog("litematic_data: Servux Paste (Pair) request from player {}", player.getName().getLiteralString());

            long timeStart = System.currentTimeMillis();
            NbtCompound tags = schemPair.getRight();
            SchematicPlacement placement = SchematicPlacement.createFromNbt(schemPair.getLeft(), tags);
            ReplaceBehavior replaceMode = ReplaceBehavior.fromStringStatic(tags.getString("ReplaceMode", ReplaceBehavior.NONE.name()));
            PasteLayerBehavior layerBehavior = PasteLayerBehavior.fromStringStatic(tags.getString("PasteLayerBehavior", PasteLayerBehavior.ALL.name()));
            LayerRange layerRange = tags.get("RenderLayerRange", LayerRange.CODEC).orElse(null);
            placement.pasteTo(player.getEntityWorld(), replaceMode, layerBehavior, layerRange);
            long timeElapsed = System.currentTimeMillis() - timeStart;
            //player.sendMessage(Text.of("Pasted §b"+placement.getName()+"§r to world §d"+player.getServerWorld().getRegistryKey().getValue().toString()+"§r in §a"+timeElapsed+"§rms."), false);
            player.sendMessage(StringUtils.translate("servux.litematics.success.pasted", placement.getName(), player.getEntityWorld().getRegistryKey().getValue().toString(), timeElapsed), false);
        }
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode, this.permissionLevel.getValue());
    }

	public boolean hasPermissionsForPaste(ServerPlayerEntity player)
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
