package fi.dy.masa.servux;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class ServuxReference
{
    public static final String MOD_ID = "servux";
    public static final String MOD_NAME = "ServuX";
    public static final String MOD_VERSION = Servux.getModVersionString(MOD_ID);
    public static final EnvType MOD_ENVIRONMENT = FabricLoader.getInstance().getEnvironmentType();
    public static final String COMMON_NAMESPACE = "fi.dy.masa";
    // Default Namespace For Network API
    public static final boolean MOD_DEBUG = true;
    public static boolean isServer() { return MOD_ENVIRONMENT == EnvType.SERVER; }
}
