package fi.dy.masa.servux;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fi.dy.masa.servux.commands.CommandProvider;
import fi.dy.masa.servux.commands.ServuxCommand;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import fi.dy.masa.servux.event.ServerInitHandler;
import fi.dy.masa.servux.servux.ServuxInitHandler;
import fi.dy.masa.servux.util.log.AnsiLogger;

public class Servux implements ModInitializer
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
    private static final AnsiLogger ANSI_LOGGER = new AnsiLogger(Servux.class);

    @Override
    public void onInitialize()
    {
        if (Reference.DEBUG_MODE) { ANSI_LOGGER.debug("DEBUG_MODE: Active"); }
        ServerInitHandler.getInstance().registerServerInitHandler(new ServuxInitHandler());
        CommandProvider.getInstance().registerCommand(new ServuxCommand());
        // Command Manager gets called before the Init Manager onServerInit()
    }

    public static void debugLog(String msg, Object... args)
    {
        if (ServuxConfigProvider.INSTANCE.hasDebugMode())
        {
            LOGGER.info(msg, args);
        }
    }

    public static void debugLogError(String msg, Object... args)
    {
        if (ServuxConfigProvider.INSTANCE.hasDebugMode())
        {
            LOGGER.error(msg, args);
        }
    }
}
