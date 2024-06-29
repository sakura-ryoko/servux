package fi.dy.masa.servux.dataproviders;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxEntitiesHandler;
import fi.dy.masa.servux.network.packet.ServuxEntitiesPacket;
import fi.dy.masa.servux.schematic.placement.SchematicPlacement;
import fi.dy.masa.servux.util.JsonUtils;

public class EntitiesDataProvider extends DataProviderBase
{
    public static final EntitiesDataProvider INSTANCE = new EntitiesDataProvider();
    protected final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();
    protected final NbtCompound metadata = new NbtCompound();
    protected int permissionLevel = -1;

    protected EntitiesDataProvider()
    {
        super("entity_data",
                ServuxEntitiesHandler.CHANNEL_ID,
                ServuxEntitiesPacket.PROTOCOL_VERSION,
                0, "servux.provider.entity_data",
                "Entity Data provider for Client Side mods.");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);
    }

    @Override
    public void registerHandler()
    {
        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxEntitiesPacket.Payload.ID, ServuxEntitiesPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
        HANDLER.registerPlayReceiver(ServuxEntitiesPacket.Payload.ID, HANDLER::receivePlayPayload);
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
        if (this.hasPermission(player) == false)
        {
            // No Permission
            Servux.logger.info("entity_data: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        Servux.logger.warn("entityDataChannel: sendMetadata to player {}", player.getName().getLiteralString());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.networkHandler != null)
        {
            HANDLER.sendPlayPayload(player.networkHandler, new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.MetadataResponse(this.metadata)));
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

        Servux.logger.warn("onBlockEntityRequest(): from player {}", player.getName().getLiteralString());

        BlockEntity be = player.getEntityWorld().getBlockEntity(pos);
        NbtCompound nbt = be != null ? be.createNbt(player.getRegistryManager()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(ServerPlayerEntity player, int entityId)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        Servux.logger.warn("onEntityRequest(): from player {}", player.getName().getLiteralString());

        Entity entity = player.getWorld().getEntityById(entityId);
        NbtCompound nbt = entity != null ? entity.writeNbt(new NbtCompound()) : new NbtCompound();
        HANDLER.encodeServerData(player, ServuxEntitiesPacket.SimpleEntityResponse(entityId, nbt));
    }

    public void handleClientNbtRequest(ServerPlayerEntity player, int transactionId, NbtCompound tags)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        Servux.logger.warn("handleBulkEntityData(): from player {}", player.getName().getLiteralString());
        if (tags.getString("Task").equals("LitematicaPaste"))
        {
            long timeStart = System.currentTimeMillis();
            SchematicPlacement placement = SchematicPlacement.createFromNbt(tags);
            placement.pasteTo(player.getServerWorld());
            long timeElapsed = System.currentTimeMillis() - timeStart;
            player.sendMessage(Text.literal("Pasted ").append(placement.getName()).append(" to world ").append(player.getServerWorld().getRegistryKey().getValue().toString()).append(" in ").append(String.valueOf(timeElapsed)).append("ms."), false);
        }
    }

    protected void setPermissionLevel(int level)
    {
        if (!(level < 0 || level > 4))
        {
            this.permissionLevel = level;
        }
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode, this.permissionLevel > -1 ? this.permissionLevel : this.defaultPerm);
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

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasInteger(obj, "permission_level"))
        {
            this.setPermissionLevel(JsonUtils.getInteger(obj, "permission_level"));
        }
    }
}
