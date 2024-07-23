package fi.dy.masa.servux.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;

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
            .executes((ctx) -> {

                return 1;
            }));
        return node;
    }
}
