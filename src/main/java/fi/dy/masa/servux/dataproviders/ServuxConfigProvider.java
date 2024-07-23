package fi.dy.masa.servux.dataproviders;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;

import java.util.List;

public class ServuxConfigProvider extends DataProviderBase
{
    public static final ServuxConfigProvider INSTANCE = new ServuxConfigProvider();
    ServuxConfig config = new ServuxConfig();

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
        DataProviderManager.INSTANCE.writeToConfig();
        source.sendFeedback(() -> Text.of("Reloaded config!"), true);
    }

    public void doSaveConfig(ServerCommandSource source)
    {
        DataProviderManager.INSTANCE.writeToConfig();
        source.sendFeedback(() -> Text.of("Saved config!"), true);
    }

    public boolean hasDebugMode()
    {
        return config.debugLog;
    }

    public void setDebugMode(boolean debugLog)
    {
        config.debugLog = debugLog;
    }

    protected void setBasePermissionLevel(int level)
    {
        if (!(level < 0 || level > 4))
        {
            config.basePermissionLevel = level;
        }
    }

    protected void setAdminPermissionLevel(int level)
    {
        if (!(level < 0 || level > 4))
        {
            config.adminPermissionLevel = level;
        }
    }

    protected void setPermissionLevel_EasyPlace(int level)
    {
        if (!(level < 0 || level > 4))
        {
            config.easyPlacePermissionLevel = level;
        }
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, Reference.MOD_ID+".main.admin", config.adminPermissionLevel);
    }

    public boolean hasPermission_EasyPlace(ServerPlayerEntity player)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, Reference.MOD_ID+".main.easy_place", config.easyPlacePermissionLevel);
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
        return (JsonObject) new Gson().toJsonTree(config);
    }

    public static final class ServuxConfig
    {
        @SerializedName("permission_level")
        private int basePermissionLevel = 0;
        @SerializedName("permission_level_admin")
        private int adminPermissionLevel = 3;
        @SerializedName("permission_level_easy_place")
        private int easyPlacePermissionLevel = 0;
        @SerializedName("debug_log")
        private boolean debugLog = false;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        config = new Gson().fromJson(obj, ServuxConfig.class);
    }
}
