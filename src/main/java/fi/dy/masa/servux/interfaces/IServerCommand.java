package fi.dy.masa.servux.interfaces;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public interface IServerCommand
{
    /**
     * Register a Server Side command
     */
    void register(CommandDispatcher<ServerCommandSource> dispatcher);
}
