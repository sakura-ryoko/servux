package fi.dy.masa.servux.dataproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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
import fi.dy.masa.servux.schematic.selection.Box;
import fi.dy.masa.servux.util.EntityUtils;
import fi.dy.masa.servux.util.JsonUtils;
import fi.dy.masa.servux.util.NBTUtils;
import fi.dy.masa.servux.util.PositionUtils;

public class LitematicsDataProvider extends DataProviderBase
{
    public static final LitematicsDataProvider INSTANCE = new LitematicsDataProvider();
    protected final static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> HANDLER = ServuxLitematicaHandler.getInstance();
    protected final NbtCompound metadata = new NbtCompound();
    protected int permissionLevel = -1;
    protected int pastePermissionLevel = -1;

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
    }

    @Override
    public void registerHandler()
    {
        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxLitematicaPacket.Payload.ID, ServuxLitematicaPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
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

    public void sendMetadata(ServerPlayerEntity player)
    {
        if (!this.hasPermission(player))
        {
            // No Permission
            Servux.logger.warn("litematic_data: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        //Servux.logger.warn("LitematicsDataProvider#sendMetadata: sendMetadata to player {}", player.getName().getLiteralString());

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
        // Do something when packets fail, if required
    }

    public void onBlockEntityRequest(ServerPlayerEntity player, BlockPos pos)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        //Servux.logger.warn("LitematicsDataProvider#onBlockEntityRequest(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.getEntityWorld().getBlockEntity(pos);
        NbtCompound nbt = be != null ? be.createNbt(player.getRegistryManager()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayerEntity player, int entityId)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        //Servux.logger.warn("LitematicsDataProvider#onEntityRequest(): from player {}", player.getName().getLiteralString());

        Entity entity = player.getWorld().getEntityById(entityId);
        NbtCompound nbt = entity != null ? entity.writeNbt(new NbtCompound()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxLitematicaPacket.SimpleEntityResponse(entityId, nbt));
    }

    public void onBulkEntityRequest(ServerPlayerEntity player, ChunkPos chunkPos, NbtCompound req)
    {
        if (this.hasPermission(player) == false)
        {
            Servux.logger.warn("litematic_data: Denying Litematic onBulkEntityRequest from player {}, Insufficient Permissions.", player.getName().getLiteralString());
            return;
        }
        if (req == null || req.isEmpty())
        {
            return;
        }

        ServerWorld world = player.getServerWorld();
        Chunk chunk = world != null ? world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) : null;

        if (chunk == null)
        {
            return;
        }

        long timeStart = System.currentTimeMillis();
        NbtList tileList = new NbtList();
        NbtList entityList = new NbtList();
        int minY = req.getInt("minY");
        int maxY = req.getInt("maxY");
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
            NbtCompound entTag = new NbtCompound();

            if (entity.saveNbt(entTag))
            {
                Vec3d posVec = new Vec3d(entity.getX() - pos1.getX(), entity.getY() - pos1.getY(), entity.getZ() - pos1.getZ());
                NBTUtils.writeEntityPositionToTag(posVec, entTag);
                entTag.putInt("entityId", entity.getId());
                entityList.add(entTag);
            }
        }

        NbtCompound output = new NbtCompound();
        output.put("TileEntities", tileList);
        output.put("Entities", entityList);
        output.putInt("chunkX", chunkPos.x);
        output.putInt("chunkZ", chunkPos.z);
        long timeElapsed = System.currentTimeMillis() - timeStart;

        HANDLER.encodeServerData(player, ServuxLitematicaPacket.ResponseS2CStart(output));
        //player.sendMessage(Text.of("ChunkPos "+chunkPos.toString()+" --> Read TE: §a"+tileList.size()+"§r, E: §b"+entityList.size()+"§r from server world §d"+player.getServerWorld().getRegistryKey().getValue().toString()+"§r in §a"+timeElapsed+"§rms."), false);
    }

    public void handleClientPasteRequest(ServerPlayerEntity player, int transactionId, NbtCompound tags)
    {
        if (this.hasPermission(player) == false || this.hasPermissionsForPaste(player) == false)
        {
            Servux.logger.warn("litematic_data: Denying Litematic Paste for player {}, Insufficient Permissions.", player.getName().getLiteralString());
            player.sendMessage(Text.literal("§cInsufficient Permissions for the Litematic paste operation.§r"));
            return;
        }
        if (player.isCreative() == false)
        {
            Servux.logger.warn("litematic_data: Denying Litematic Paste for player {}, Player is not in Creative Mode.", player.getName().getLiteralString());
            player.sendMessage(Text.literal("§cCreative Mode is required for the Litematic paste operation.§r"));
            return;
        }

        //Servux.logger.warn("LitematicsDataProvider#handleClientPasteRequest(): from player {}", player.getName().getLiteralString());
        if (tags.getString("Task").equals("LitematicaPaste"))
        {
            long timeStart = System.currentTimeMillis();
            SchematicPlacement placement = SchematicPlacement.createFromNbt(tags);
            placement.pasteTo(player.getServerWorld());
            long timeElapsed = System.currentTimeMillis() - timeStart;
            player.sendMessage(Text.of("Pasted §b"+placement.getName()+"§r to world §d"+player.getServerWorld().getRegistryKey().getValue().toString()+"§r in §a"+timeElapsed+"§rms."), false);
        }
    }

    protected void setPermissionLevel(int level)
    {
        if (!(level < 0 || level > 4))
        {
            this.permissionLevel = level;
        }
    }

    protected void setPastePermissionLevel(int level)
    {
        if (!(level < 0 || level > 4))
        {
            this.pastePermissionLevel = level;
        }
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode, this.permissionLevel > -1 ? this.permissionLevel : this.defaultPerm);
    }

    public boolean hasPermissionsForPaste(ServerPlayerEntity player)
    {
        return (this.hasPermission(player) && Permissions.check(player, this.permNode + ".paste", this.pastePermissionLevel > -1 ? this.pastePermissionLevel : this.defaultPerm));
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.permissionLevel > -1)
        {
            obj.add("permission_level", new JsonPrimitive(this.permissionLevel));
        }
        else
        {
            obj.add("permission_level", new JsonPrimitive(this.defaultPerm));
        }
        if (this.pastePermissionLevel > -1)
        {
            obj.add("permission_level_paste", new JsonPrimitive(this.pastePermissionLevel));
        }
        else
        {
            obj.add("permission_level_paste", new JsonPrimitive(this.defaultPerm));
        }

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasInteger(obj, "permission_level"))
        {
            this.setPermissionLevel(JsonUtils.getInteger(obj, "permission_level"));
        }
        if (JsonUtils.hasInteger(obj, "permission_level_paste"))
        {
            this.setPastePermissionLevel(JsonUtils.getInteger(obj, "permission_level_paste"));
        }
    }
}
