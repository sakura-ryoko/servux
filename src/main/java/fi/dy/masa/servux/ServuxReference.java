package fi.dy.masa.servux;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class ServuxReference
{
    public static final String MOD_ID = "servux";
    public static final String MOD_NAME = "ServuX";
    public static final String MOD_VERSION = Servux.getModVersionString(MOD_ID);
    private static final EnvType MOD_ENVIRONMENT = FabricLoader.getInstance().getEnvironmentType();
    public static final boolean MOD_DEBUG = true;
    public static boolean isServer() { return MOD_ENVIRONMENT == EnvType.SERVER; }
    public static boolean isClient() { return MOD_ENVIRONMENT == EnvType.CLIENT; }
    private static boolean MOD_OPEN_TO_LAN = false;
    private static boolean MOD_DEDICATED = false;
    public static void setOpenToLan(boolean toggle)
    {
        if (toggle && isClient())
        {
            MOD_OPEN_TO_LAN = true;
            MOD_DEDICATED = false;
        }
        else if (!toggle && isServer())
        {
            MOD_OPEN_TO_LAN = false;
        }
    }
    public static void setDedicated(boolean toggle)
    {
        if (toggle && isServer())
        {
            MOD_OPEN_TO_LAN = false;
            MOD_DEDICATED = true;
        }
        else if (!toggle && isClient())
        {
            MOD_DEDICATED = false;
        }
    }
    public static boolean isDedicated() { return MOD_DEDICATED; }
    public static boolean isOpenToLan() { return MOD_OPEN_TO_LAN; }
    // For keeping networking API separated for basic sanity checks.
}
