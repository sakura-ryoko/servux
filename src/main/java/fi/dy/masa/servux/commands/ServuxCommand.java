package fi.dy.masa.servux.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;

public class ServuxCommand
{
    @SuppressWarnings("unchecked")
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder) CommandManager
            .literal(Reference.MOD_ID).requires((source) ->
                ServuxConfigProvider.INSTANCE.hasPermission(source.getPlayer()) || source.hasPermissionLevel(4)))
            .then(CommandManager.literal("reload").requires((source) ->
                ServuxConfigProvider.INSTANCE.hasBasePermission_Node(source.getPlayer(), "reload") || source.hasPermissionLevel(4))
                .executes((ctx) ->
                {
                    ServuxConfigProvider.INSTANCE.doReloadConfig(ctx.getSource());
                    return 1;
                }))
            .then(CommandManager.literal("save").requires((source) ->
                ServuxConfigProvider.INSTANCE.hasBasePermission_Node(source.getPlayer(), "save") || source.hasPermissionLevel(4))
                .executes((ctx) ->
                {
                    ServuxConfigProvider.INSTANCE.doSaveConfig(ctx.getSource());
                    return 1;
                }))
            );
    }
}
