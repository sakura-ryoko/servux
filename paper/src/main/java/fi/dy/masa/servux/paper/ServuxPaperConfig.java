package fi.dy.masa.servux.paper;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed wrapper around the plugin's {@code config.yml}, analogous to Fabric's per-provider
 * {@code IServuxSetting} groups (but as a flat set of static-cached values, matching this port's
 * simpler scope).
 * <p>
 * Values are cached in static fields on {@link #load(FileConfiguration)} (called once on enable,
 * and again on {@code /servux reload}), rather than reading the config live on every access.
 */
public final class ServuxPaperConfig
{
    private static boolean debugLog = false;

    private static int hudDataUpdateInterval = 40;
    private static boolean hudDataShareSeed = false;
    private static boolean hudDataShareWeatherStatus = false;
    private static boolean hudDataLoggersEnabled = false;
    private static List<String> hudDataLoggersEnableList = List.of("tps", "mob_caps");

    private static int structuresUpdateInterval = 40;
    private static int structuresTimeout = 600;
    private static boolean structuresBlacklistEnabled = false;
    private static boolean structuresWhitelistEnabled = false;
    private static List<String> structuresBlacklist = List.of("minecraft:buried_treasure");
    private static List<String> structuresWhitelist = List.of();

    private static boolean entityDataAllowPlayerInventory = true;
    private static boolean entityDataAllowPlayerEnderItems = true;

    private ServuxPaperConfig()
    {
    }

    public static void load(FileConfiguration config)
    {
        debugLog = config.getBoolean("debug_log", false);

        hudDataUpdateInterval = config.getInt("hud_data.update_interval", 40);
        hudDataShareSeed = config.getBoolean("hud_data.share_seed", false);
        hudDataShareWeatherStatus = config.getBoolean("hud_data.share_weather_status", false);
        hudDataLoggersEnabled = config.getBoolean("hud_data.loggers_enabled", false);
        hudDataLoggersEnableList = config.getStringList("hud_data.loggers_enable_list");

        structuresUpdateInterval = config.getInt("structures.update_interval", 40);
        structuresTimeout = config.getInt("structures.timeout", 600);
        structuresBlacklistEnabled = config.getBoolean("structures.blacklist_enabled", false);
        structuresWhitelistEnabled = config.getBoolean("structures.whitelist_enabled", false);
        structuresBlacklist = config.getStringList("structures.blacklist");
        structuresWhitelist = config.getStringList("structures.whitelist");

        entityDataAllowPlayerInventory = config.getBoolean("entity_data.nbt_allow_player_inventory", true);
        entityDataAllowPlayerEnderItems = config.getBoolean("entity_data.nbt_allow_player_ender_items", true);
    }

    public static boolean debugLog()
    {
        return debugLog;
    }

    public static int hudDataUpdateInterval()
    {
        return hudDataUpdateInterval;
    }

    public static boolean hudDataShareSeed()
    {
        return hudDataShareSeed;
    }

    public static boolean hudDataShareWeatherStatus()
    {
        return hudDataShareWeatherStatus;
    }

    public static boolean hudDataLoggersEnabled()
    {
        return hudDataLoggersEnabled;
    }

    public static List<String> hudDataLoggersEnableList()
    {
        return hudDataLoggersEnableList;
    }

    public static int structuresUpdateInterval()
    {
        return structuresUpdateInterval;
    }

    public static int structuresTimeout()
    {
        return structuresTimeout;
    }

    public static boolean structuresBlacklistEnabled()
    {
        return structuresBlacklistEnabled;
    }

    public static boolean structuresWhitelistEnabled()
    {
        return structuresWhitelistEnabled;
    }

    public static List<String> structuresBlacklist()
    {
        return structuresBlacklist;
    }

    public static List<String> structuresWhitelist()
    {
        return structuresWhitelist;
    }

    public static boolean entityDataAllowPlayerInventory()
    {
        return entityDataAllowPlayerInventory;
    }

    public static boolean entityDataAllowPlayerEnderItems()
    {
        return entityDataAllowPlayerEnderItems;
    }
}
