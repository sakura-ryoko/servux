package fi.dy.masa.servux.dataproviders;

import java.util.List;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fi.dy.masa.servux.settings.ServuxStringSetting;
import fi.dy.masa.servux.util.i18nLang;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.util.StringUtils;

public class ServuxConfigProvider extends DataProviderBase
{
    public static final ServuxConfigProvider INSTANCE = new ServuxConfigProvider();

    private final ServuxIntSetting basePermissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
    private final ServuxIntSetting adminPermissionLevel = new ServuxIntSetting(this, "permission_level_admin", 3, 4, 0);
    private final ServuxIntSetting easyPlacePermissionLevel = new ServuxIntSetting(this, "permission_level_easy_place", 0, 4, 0);
    private final ServuxStringSetting defaultLanguage = new ServuxStringSetting(this, "default_language", i18nLang.DEFAULT_LANG, List.of("en_us", "zh_cn"), false) {
        @Override
        public void setValueNoCallback(String value)
        {
            i18nLang.tryLoadLanguage(value.toLowerCase());
            super.setValueNoCallback(value.toLowerCase());
        }

        @Override
        public void setValue(String value) throws CommandSyntaxException
        {
            String lowerCase = value.toLowerCase();
            if (i18nLang.tryLoadLanguage(lowerCase))
            {
                var oldValue = this.getValue();
                super.setValueNoCallback(lowerCase);
                this.onValueChanged(oldValue, value);
            }
            else
            {
                throw new SimpleCommandExceptionType(StringUtils.translate("servux.command.config.invalid_language", value)).create();
            }
        }
    };
    private final ServuxBoolSetting debugLog = new ServuxBoolSetting(this, "debug_log", Text.of("Debug Log"), Text.of("Enable debug logging"), false);
    private final List<IServuxSetting<?>> settings = List.of(this.basePermissionLevel, this.adminPermissionLevel, this.easyPlacePermissionLevel, this.defaultLanguage, this.debugLog);

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
        source.sendFeedback(() -> StringUtils.translate("servux.command.config.reloaded"), true);
    }

    public void doSaveConfig(ServerCommandSource source)
    {
        DataProviderManager.INSTANCE.writeToConfig();
        source.sendFeedback(() -> StringUtils.translate("servux.command.config.saved"), true);
    }

    public boolean hasDebugMode()
    {
        return this.debugLog.getValue();
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

    public String getDefaultLanguage()
    {
        return defaultLanguage.getValue();
    }
}
