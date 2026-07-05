package fi.dy.masa.servux.dataproviders;

import java.util.List;
import me.lucko.fabric.api.permissions.v0.Permissions;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.settings.ServuxStringSetting;
import fi.dy.masa.servux.util.StringUtils;
import fi.dy.masa.servux.util.i18n.i18nManager;

public class ServuxConfigProvider extends DataProviderBase
{
    public static final ServuxConfigProvider INSTANCE = new ServuxConfigProvider();
    public static final i18nManager LANG = i18nManager.create(Reference.MOD_ID);

    private final ServuxIntSetting basePermissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
    private final ServuxIntSetting adminPermissionLevel = new ServuxIntSetting(this, "permission_level_admin", 3, 4, 0);
    private final ServuxIntSetting easyPlacePermissionLevel = new ServuxIntSetting(this, "permission_level_easy_place", 0, 4, 0);
    private final ServuxBoolSetting easyPlaceValidatorEnabled = new ServuxBoolSetting(this, "easy_place_validator_enabled", true);
    private final ServuxStringSetting defaultLanguage = new ServuxStringSetting(this, "default_language",
                                                                                i18nManager.DEFAULT_LANG,
                                                                                List.of(i18nManager.DEFAULT_LANG), false)
    {
        @Override
        public void setValueNoCallback(String value)
        {
            LANG.setLang(value);
            super.setValueNoCallback(value.toLowerCase());
        }

        @Override
        public void setValue(String value) throws CommandSyntaxException
        {
            String lowerCase = value.toLowerCase();
            if (LANG.getLanguageKeys().contains(lowerCase))
            {
                LANG.setLang(lowerCase);
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
    private final List<IServuxSetting<?>> settings = List.of(
            this.basePermissionLevel, this.adminPermissionLevel,
            this.easyPlacePermissionLevel, this.easyPlaceValidatorEnabled,
            this.defaultLanguage, this.debugLog
    );

    protected ServuxConfigProvider()
    {
        super("servux_main",
                Identifier.of("servux", "main"),
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
        if (LANG != null)
        {
            this.defaultLanguage.updateExamples(LANG.getLanguageKeys());
        }
    }

    @Override
    public void unregisterHandler()
    {
        // NO-OP
    }

    @Override
    public boolean isPlayerRegistered(ServerPlayerEntity player)
    {
        return true;
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
        return this.debugLog.getValue() || Reference.DEV_DEBUG;
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, Reference.MOD_ID+".main.admin", this.adminPermissionLevel.getValue());
    }

    public boolean hasPermission_EasyPlace(ServerPlayerEntity player)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, Reference.MOD_ID+".main.easy_place", this.easyPlacePermissionLevel.getValue());
    }

    public boolean isEasyPlaceValidatorEnabled()
    {
        return this.easyPlaceValidatorEnabled.getValue();
    }

	public String getDefaultLanguage()
	{
		return this.defaultLanguage.getValue();
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
