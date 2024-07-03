package fi.dy.masa.servux.mixin;

import fi.dy.masa.servux.util.IWorldUpdateSuppressor;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public class MixinWorld implements IWorldUpdateSuppressor
{
    private boolean litematica_preventBlockUpdates;

    @Override
    public boolean litematica_getShouldPreventBlockUpdates()
    {
        return this.litematica_preventBlockUpdates;
    }

    @Override
    public void litematica_setShouldPreventBlockUpdates(boolean preventUpdates)
    {
        this.litematica_preventBlockUpdates = preventUpdates;
    }
}
