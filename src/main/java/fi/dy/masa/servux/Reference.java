package fi.dy.masa.servux;

import net.minecraft.MinecraftVersion;

public class Reference
{
    public static final String MOD_ID = "servux";
    public static final String MOD_NAME = "ServuX";
    public static final String MOD_VERSION = Servux.getModVersionString(MOD_ID);
    public static final String MC_VERSION = MinecraftVersion.CURRENT.getName();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_STRING = MOD_ID + "-" + MOD_TYPE + "-" + MC_VERSION + "-" + MOD_VERSION;
}
