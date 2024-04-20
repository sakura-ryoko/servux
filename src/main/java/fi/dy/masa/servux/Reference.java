package fi.dy.masa.servux;

import java.io.File;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.MinecraftVersion;
import fi.dy.masa.servux.util.StringUtils;

public class Reference
{
    public static final String MOD_ID = "servux";
    public static final String MOD_NAME = "ServuX";
    public static final String MOD_VERSION = StringUtils.getModVersionString(MOD_ID);
    public static final String MC_VERSION = MinecraftVersion.CURRENT.getName();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_STRING = MOD_ID + "-" + MOD_TYPE + "-" + MC_VERSION + "-" + MOD_VERSION;

    public static final File DEFAULT_RUN_DIR = FabricLoader.getInstance().getGameDir().toFile();
    public static final File DEFAULT_CONFIG_DIR = FabricLoader.getInstance().getConfigDir().toFile();
}
