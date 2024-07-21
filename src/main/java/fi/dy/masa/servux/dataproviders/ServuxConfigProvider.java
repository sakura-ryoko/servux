package fi.dy.masa.servux.dataproviders;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.util.JsonUtils;

public class ServuxConfigProvider extends DataProviderBase
{
    public static final ServuxConfigProvider INSTANCE = new ServuxConfigProvider();
    private int basePermissionLevel = -1;
    private int adminPermissionLevel = -1;
    private int easyPlacePermissionLevel = -1;
    private int defaultAdminPermissions = 3;
    private boolean debugLog = false;

    protected ServuxConfigProvider()
    {
        super("servux_main",
                Identifier.of("servux:main"),
                1, 0, Reference.MOD_ID+".main",
                "The Servux Main configuration data provider");
    }

    @Override
    public void registerHandler()
    {
        // NO-OP
    }

    @Override
    public void unregisterHandler()
    {
        // NO-OP
    }

    @Override
    public IPluginServerPlayHandler<?> getPacketHandler()
    {
        return null;
    }

    public void doReloadConfig(ServerCommandSource source)
    {
        DataProviderManager.INSTANCE.readFromConfig();
        source.sendFeedback(() ->
        {
            return Text.of("Reloading config!");
        }, true);
    }

    public void doSaveConfig(ServerCommandSource source)
    {
        DataProviderManager.INSTANCE.writeToConfig();
        source.sendFeedback(() ->
        {
            return Text.of("Saving config!");
        }, true);
    }

    public boolean hasDebugMode()
    {
        return this.debugLog;
    }

    public void setDebugMode(boolean toggle)
    {
        this.debugLog = toggle;
    }

    protected void setBasePermissionLevel(int level)
    {
        if (!(level < 0 || level > 4))
        {
            this.basePermissionLevel = level;
        }
    }

    protected void setAdminPermissionLevel(int level)
    {
        if (!(level < 0 || level > 4))
        {
            this.adminPermissionLevel = level;
        }
    }

    protected void setPermissionLevel_EasyPlace(int level)
    {
        if (!(level < 0 || level > 4))
        {
            this.easyPlacePermissionLevel = level;
        }
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, this.permNode+".admin", this.adminPermissionLevel > -1 ? this.adminPermissionLevel : this.defaultAdminPermissions);
    }

    public boolean hasBasePermission_Node(ServerPlayerEntity player, String node)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, this.permNode+"."+node, this.basePermissionLevel > -1 ? this.basePermissionLevel : node.equals("reload") ? this.defaultAdminPermissions : this.defaultPerm);
    }

    public boolean hasPermission_EasyPlace(ServerPlayerEntity player)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, this.permNode+".easy_place", this.easyPlacePermissionLevel > -1 ? this.easyPlacePermissionLevel : this.defaultPerm);
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

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.basePermissionLevel > -1)
        {
            obj.add("permission_level", new JsonPrimitive(this.basePermissionLevel));
        }
        else
        {
            obj.add("permission_level", new JsonPrimitive(this.defaultPerm));
        }

        if (this.adminPermissionLevel > -1)
        {
            obj.add("permission_level_admin", new JsonPrimitive(this.adminPermissionLevel));
        }
        else
        {
            obj.add("permission_level_admin", new JsonPrimitive(this.defaultAdminPermissions));
        }

        if (this.easyPlacePermissionLevel > -1)
        {
            obj.add("permission_level_easy_place", new JsonPrimitive(this.easyPlacePermissionLevel));
        }
        else
        {
            obj.add("permission_level_easy_place", new JsonPrimitive(this.defaultPerm));
        }

        obj.add("debug_log", new JsonPrimitive(this.debugLog));

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasInteger(obj, "permission_level"))
        {
            this.setBasePermissionLevel(JsonUtils.getInteger(obj, "permission_level"));
        }
        if (JsonUtils.hasInteger(obj, "permission_level_admin"))
        {
            this.setAdminPermissionLevel(JsonUtils.getInteger(obj, "permission_level_admin"));
        }
        if (JsonUtils.hasInteger(obj, "permission_level_easy_place"))
        {
            this.setPermissionLevel_EasyPlace(JsonUtils.getInteger(obj, "permission_level_easy_place"));
        }
        if (JsonUtils.hasBoolean(obj, "debug_log"))
        {
            this.setDebugMode(JsonUtils.getBoolean(obj, "debug_log"));
        }
    }
}
