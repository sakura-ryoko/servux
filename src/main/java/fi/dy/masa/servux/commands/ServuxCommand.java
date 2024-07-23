package fi.dy.masa.servux.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import net.minecraft.text.Text;

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
                .then(updateSettingsNode()))
        );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> updateSettingsNode() {
        var node = CommandManager.argument("setting", StringArgumentType.string());
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
        node.then(CommandManager.argument("value", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> {
                    String settingName = ctx.getArgument("setting", String.class);
                    var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
                    if (setting != null)
                    {
                        return CommandSource.suggestMatching(setting.right().examples(), builder);
                    }
                    return builder.buildFuture();
                })
            .executes((ctx) -> {
                String settingName = ctx.getArgument("setting", String.class);
                var pair = DataProviderManager.INSTANCE.getSettingByName(settingName);
                if (pair == null)
                {
                    throw new SimpleCommandExceptionType(Text.of("Unknown setting")).create();
                }
                var setting = pair.right();
                String value = ctx.getArgument("value", String.class);
                if (!setting.validateString(value))
                {
                    throw new SimpleCommandExceptionType(Text.of("Invalid value")).create();
                }
                setting.setValueFromString(value);
                String qualifiedName = pair.left().getName() + ":" + pair.right().name();
                ctx.getSource().sendFeedback(() -> Text.of("Set " + qualifiedName + " to " + value), true);
                return 1;
            }));
        return node;
    }
}
