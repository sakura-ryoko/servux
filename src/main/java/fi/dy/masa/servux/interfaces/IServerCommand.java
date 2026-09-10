package fi.dy.masa.servux.interfaces;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public interface IServerCommand
{
    /**
     * Register a Server Side command
     */
    void register(CommandDispatcher<CommandSourceStack> dispatcher,
                  CommandBuildContext registryAccess,
                  Commands.CommandSelection environment);

    String name();

    String node();
}
