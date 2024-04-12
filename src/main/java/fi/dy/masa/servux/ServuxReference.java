package fi.dy.masa.servux;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.MinecraftVersion;

public class ServuxReference
{
    public static final String MOD_ID = "servux";
    public static final String MOD_NAME = "ServuX";
    public static final String MOD_VERSION = Servux.getModVersionString(MOD_ID);
    public static final String MC_VERSION = MinecraftVersion.CURRENT.getName();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_STRING = MOD_ID + "-" + MOD_TYPE + "-" + MC_VERSION + "-" + MOD_VERSION;
    private static final EnvType MOD_ENVIRONMENT = FabricLoader.getInstance().getEnvironmentType();
    public static final boolean MOD_DEBUG = true;
    private static boolean serverOpenToLan = false;
    private static boolean serverDedicated = false;
    public static boolean isServer() {return MOD_ENVIRONMENT == EnvType.SERVER;}
    public static boolean isClient() {return MOD_ENVIRONMENT == EnvType.CLIENT;}
    public static boolean isDedicated() {return serverDedicated;}
    public static boolean isOpenToLan() {return serverOpenToLan;}

    public static void setOpenToLan(boolean toggle)
    {
        if (toggle && isClient())
        {
            serverOpenToLan = true;
            serverDedicated = false;
        } else
        {
            serverOpenToLan = false;
        }
    }

    public static void setDedicated(boolean toggle)
    {
        if (toggle && isServer())
        {
            serverDedicated = true;
            serverOpenToLan = false;
        } else
        {
            serverDedicated = false;
        }
    }
}
