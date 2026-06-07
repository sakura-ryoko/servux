package fi.dy.masa.servux.mixin.world;

import fi.dy.masa.servux.util.IWorldUpdateSuppressor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public class MixinLevel_updateSuppression implements IWorldUpdateSuppressor
{
    @Unique private boolean servux_preventBlockUpdates;

    @Override
    public boolean servux_getShouldPreventBlockUpdates()
    {
        return this.servux_preventBlockUpdates;
    }

    @Override
    public void servux_setShouldPreventBlockUpdates(boolean preventUpdates)
    {
        this.servux_preventBlockUpdates = preventUpdates;
    }
}
