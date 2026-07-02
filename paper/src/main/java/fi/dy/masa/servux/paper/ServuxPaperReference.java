package fi.dy.masa.servux.paper;

import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import io.papermc.paper.ServerBuildInfo;

/**
 * Small shared reference/logging helper, analogous to Fabric's {@code Reference} constants +
 * {@code Servux.debugLog(...)} - centralizes the mod identifier string (previously computed
 * ad hoc per-channel) and a config-gated debug log, used across the hud_data/structures/entity_data
 * channel and provider classes.
 *
 * @see <a href="../../../../../../../src/main/java/fi/dy/masa/servux/Reference.java">Reference.java (Fabric reference)</a>
 */
public final class ServuxPaperReference
{
    public static final String MOD_ID = "servux";
    public static final String MOD_TYPE = "paper";

    private static String modString = MOD_ID + "-" + MOD_TYPE + "-unknown";
    private static boolean debugLogEnabled = false;
    private static Logger logger;

    private ServuxPaperReference()
    {
    }

    /** Call once from {@code onEnable()}. */
    public static void init(Plugin plugin)
    {
        modString = MOD_ID + "-" + MOD_TYPE + "-" + ServerBuildInfo.buildInfo().minecraftVersionId() + "-" + plugin.getPluginMeta().getVersion();
        logger = plugin.getSLF4JLogger();
    }

    public static void setDebugLogEnabled(boolean enabled)
    {
        debugLogEnabled = enabled;
    }

    /** Mirrors Fabric's {@code Reference.MOD_STRING}. */
    public static String modString()
    {
        return modString;
    }

    /** Unconditional logger, for Fabric-style {@code Servux.LOGGER.error(...)/.warn(...)} parity (not gated by `debug_log`). */
    public static Logger logger()
    {
        return logger;
    }

    /** Mirrors Fabric's {@code Servux.debugLog(...)} - gated on the `debug_log` config toggle. */
    public static void debugLog(String msg, Object... args)
    {
        if (debugLogEnabled && logger != null)
        {
            logger.info(msg, args);
        }
    }
}
