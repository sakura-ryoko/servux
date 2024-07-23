package fi.dy.masa.servux.dataproviders;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
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

    private final ServuxIntSetting basePermissionLevel = new ServuxIntSetting(this, "permission_level", Text.of("Base Permission Level"), Text.of("The base permission level for the mod"), 0, 4, 0);
    private final ServuxIntSetting adminPermissionLevel = new ServuxIntSetting(this, "permission_level_admin", Text.of("Admin Permission Level"), Text.of("The admin permission level for the mod"), 3, 4, 0);
    private final ServuxIntSetting easyPlacePermissionLevel = new ServuxIntSetting(this, "permission_level_easy_place", Text.of("Easy Place Permission Level"), Text.of("The permission level for the Easy Place feature"), 0, 4, 0);
    private final ServuxBoolSetting debugLog = new ServuxBoolSetting(this, "debug_log", Text.of("Debug Log"), Text.of("Enable debug logging"), false);
    private final List<IServuxSetting<?>> settings = List.of(this.basePermissionLevel, this.adminPermissionLevel, this.easyPlacePermissionLevel, this.debugLog);

    protected ServuxConfigProvider()
    {
        super("servux_main",
                Identifier.of("servux:main"),
                1, 0, Reference.MOD_ID+".main",
                "The Servux Main configuration data provider");
    }

    @Override
    public List<IServuxSetting<?>> getSettings()
    {
        return settings;
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
        source.sendFeedback(() -> Text.of("Reloaded config!"), true);
    }

    public void doSaveConfig(ServerCommandSource source)
    {
        DataProviderManager.INSTANCE.writeToConfig();
        source.sendFeedback(() -> Text.of("Saved config!"), true);
    }

    public boolean hasDebugMode()
    {
        return this.debugLog.getValue();
    }

    public void setDebugMode(boolean debugLog)
    {
        this.debugLog.setValue(debugLog);
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, Reference.MOD_ID+".main.admin", adminPermissionLevel.getValue());
    }

    public boolean hasPermission_EasyPlace(ServerPlayerEntity player)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, Reference.MOD_ID+".main.easy_place", easyPlacePermissionLevel.getValue());
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
