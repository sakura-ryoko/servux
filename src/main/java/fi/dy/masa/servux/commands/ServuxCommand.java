package fi.dy.masa.servux.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.util.StringUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ServuxCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register(CommandManager
            .literal(Reference.MOD_ID).requires(Permissions.require(Reference.MOD_ID + ".commands", 4))
            .then(CommandManager.literal("reload").requires(Permissions.require(Reference.MOD_ID + ".commands.reload", 4))
                .executes((ctx) ->
                {
                    ServuxConfigProvider.INSTANCE.doReloadConfig(ctx.getSource());
                    return 1;
                }))
            .then(CommandManager.literal("save").requires(Permissions.require(Reference.MOD_ID + ".commands.save", 4))
                .executes((ctx) ->
                {
                    ServuxConfigProvider.INSTANCE.doSaveConfig(ctx.getSource());
                    return 1;
                }))
            .then(CommandManager.literal("set")
                .requires(Permissions.require(Reference.MOD_ID + ".commands.set", 4))
                .then(settingsNode().then(CommandManager.argument("value", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            Identifier settingId = ctx.getArgument("setting", Identifier.class);
                            String settingName = StringUtils.removeDefaultMinecraftNamespace(settingId);
                            var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
                            if (setting != null)
                            {
                                return CommandSource.suggestMatching(setting.examples(), builder);
                            }
                            return builder.buildFuture();
                        })
                        .executes((ctx) -> {
                            Identifier settingId = ctx.getArgument("setting", Identifier.class);
                            String settingName = StringUtils.removeDefaultMinecraftNamespace(settingId);
                            var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
                            if (setting == null)
                            {
                                throw new SimpleCommandExceptionType(Text.of("Unknown setting")).create();
                            }
                            String value = ctx.getArgument("value", String.class);
                            if (!setting.validateString(value))
                            {
                                throw new SimpleCommandExceptionType(Text.of("Invalid value")).create();
                            }
                            setting.setValueFromString(value);
                            ctx.getSource().sendFeedback(() -> Text.translatable("Set %s to %s", setting.shortDisplayName(), value), true);
                            return 1;
                        }))))
            .then(CommandManager.literal("info")
                .requires(Permissions.require(Reference.MOD_ID + ".commands.info", 4))
                .then(settingsNode().executes((ctx) -> {
                    Identifier settingId = ctx.getArgument("setting", Identifier.class);
                    String settingName = StringUtils.removeDefaultMinecraftNamespace(settingId);
                    var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
                    if (setting == null)
                    {
                        throw new SimpleCommandExceptionType(Text.of("Unknown setting")).create();
                    }
                    ctx.getSource().sendFeedback(() -> setting.prettyName().copy().append(" (" + setting.qualifiedName() + ")"), false);
                    ctx.getSource().sendFeedback(() -> setting.comment(), false);
                    ctx.getSource().sendFeedback(() -> Text.literal("Current value: " + setting.valutToString(setting.getValue())), false);
                    ctx.getSource().sendFeedback(() -> Text.literal("Default value: " + setting.valutToString(setting.getDefaultValue())), false);

                    // todo
                    return 1;
                })))
        );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> settingsNode() {
        var node = CommandManager.argument("setting", IdentifierArgumentType.identifier());
        node.suggests((ctx, builder) -> {
            if (builder.getRemainingLowerCase().contains(":"))
            {
                String providerName = builder.getRemaining().split(":")[0];
                DataProviderManager.INSTANCE.getProviderByName(providerName).ifPresent(iDataProvider ->
                    iDataProvider.getSettings().forEach(iServuxSetting ->
                    {
                        builder.suggest(providerName + ":" + iServuxSetting.name(), iServuxSetting.prettyName());
                    }));
            }
            else
            {
                DataProviderManager.INSTANCE.getAllProviders().stream()
                    .flatMap(iDataProvider -> iDataProvider.getSettings().stream())
                    .forEach(iServuxSetting -> builder.suggest(iServuxSetting.name(), iServuxSetting.prettyName()));

                DataProviderManager.INSTANCE.getAllProviders().forEach(iDataProvider -> builder.suggest(iDataProvider.getName()));
            }
            return builder.buildFuture();
        });
        return node;
    }
}
