package fi.dy.masa.servux.commands;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import fi.dy.masa.servux.interfaces.IServerCommand;

public class CommandProvider implements ICommandProvider
{
    private static final CommandProvider INSTANCE = new CommandProvider();
    private final List<IServerCommand> commands = new ArrayList<>();
    public static ICommandProvider getInstance() { return INSTANCE; }

    @Override
    public void registerCommand(IServerCommand command)
    {
        if (!this.commands.contains(command))
        {
            this.commands.add(command);
        }
    }

    @Override
    public void unregisterCommand(IServerCommand command)
    {
        this.commands.remove(command);
    }

    @ApiStatus.Internal
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                 CommandRegistryAccess registryAccess,
                                 CommandManager.RegistrationEnvironment environment)
    {
        this.commands.forEach((command) -> command.register(dispatcher, registryAccess, environment));
    }
}
