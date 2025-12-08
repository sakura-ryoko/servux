package fi.dy.masa.servux.util;

import net.minecraft.world.level.Level;

public class WorldUtils
{
    public static boolean shouldPreventBlockUpdates(Level world)
    {
        return ((IWorldUpdateSuppressor) world).servux_getShouldPreventBlockUpdates();
    }

    public static void setShouldPreventBlockUpdates(Level world, boolean preventUpdates)
    {
        ((IWorldUpdateSuppressor) world).servux_setShouldPreventBlockUpdates(preventUpdates);
    }
}
