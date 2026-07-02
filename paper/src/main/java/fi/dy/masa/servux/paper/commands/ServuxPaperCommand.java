package fi.dy.masa.servux.paper.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;

import fi.dy.masa.servux.paper.ServuxPaperConfig;
import fi.dy.masa.servux.paper.ServuxPaperPlugin;
import fi.dy.masa.servux.paper.ServuxPaperReference;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

/**
 * Minimal Paper-side mirror of Servux's {@code /servux} command (Fabric side): only the
 * whole-config {@code reload}/{@code save}/{@code list} subcommands are implemented for now.
 * {@code set}/{@code info}/{@code search} (per-setting introspection/mutation) are deferred until
 * a Paper-side settings-metadata system exists to justify them.
 * <p>
 * Uses Paper's native Brigadier command API - the same {@code com.mojang.brigadier} library
 * Fabric's {@code ServuxCommand} already builds on, registered via the plugin's
 * {@code LifecycleEventManager} rather than a Bukkit {@code CommandExecutor}.
 *
 * @see <a href="../../../../../../../../../src/main/java/fi/dy/masa/servux/commands/ServuxCommand.java">ServuxCommand.java (Fabric reference)</a>
 */
public final class ServuxPaperCommand
{
    private ServuxPaperCommand()
    {
    }

    public static void register(ServuxPaperPlugin plugin)
    {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
        {
            LiteralCommandNode<CommandSourceStack> node = Commands.literal("servux")
                .requires(source -> source.getSender().hasPermission("servux.commands"))
                .then(Commands.literal("reload")
                    .requires(source -> source.getSender().hasPermission("servux.commands.reload"))
                    .executes(ctx ->
                    {
                        plugin.reloadConfig();
                        ServuxPaperConfig.load(plugin.getConfig());
                        ServuxPaperReference.setDebugLogEnabled(ServuxPaperConfig.debugLog());
                        ctx.getSource().getSender().sendPlainMessage("Servux (Paper) config reloaded.");
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(Commands.literal("save")
                    .requires(source -> source.getSender().hasPermission("servux.commands.save"))
                    .executes(ctx ->
                    {
                        plugin.saveConfig();
                        ctx.getSource().getSender().sendPlainMessage("Servux (Paper) config saved.");
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(Commands.literal("list")
                    .requires(source -> source.getSender().hasPermission("servux.commands.list"))
                    .executes(ctx ->
                    {
                        ctx.getSource().getSender().sendPlainMessage(
                            "Servux (Paper) channels: hud_data, structures, entity_data. Edit config.yml and run /servux reload to apply changes."
                        );
                        return Command.SINGLE_SUCCESS;
                    }))
                .build();

            event.registrar().register(node, "Servux Paper management command");
        });
    }
}
