package fi.dy.masa.servux.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CommandProvider
{
    private static final CommandProvider INSTANCE = new CommandProvider();
    public static CommandProvider getInstance() { return INSTANCE; }

    /**
     * The idea here is to eventually create a more Robust Command Provider, using an interface
     */
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                 CommandRegistryAccess registryAccess,
                                 CommandManager.RegistrationEnvironment environment)
    {
        ServuxCommand.register(dispatcher);
    }
}
