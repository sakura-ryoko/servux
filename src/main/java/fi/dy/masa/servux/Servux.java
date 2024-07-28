package fi.dy.masa.servux;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fi.dy.masa.servux.commands.CommandProvider;
import fi.dy.masa.servux.commands.ServuxCommand;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import fi.dy.masa.servux.event.ServerInitHandler;
import fi.dy.masa.servux.servux.ServuxInitHandler;

public class Servux implements ModInitializer
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitialize()
    {
        ServerInitHandler.getInstance().registerServerInitHandler(new ServuxInitHandler());
        CommandProvider.getInstance().registerCommand(new ServuxCommand());
        // Command Manager gets called before the Init Manager onServerInit()
    }

    public static void debugLog(String msg, Object... args)
    {
        if (ServuxConfigProvider.INSTANCE.hasDebugMode())
        {
            logger.info(msg, args);
        }
    }
}
