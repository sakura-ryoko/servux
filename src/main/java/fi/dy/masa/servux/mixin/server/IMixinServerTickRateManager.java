package fi.dy.masa.servux.mixin.server;

import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerTickRateManager.class)
public interface IMixinServerTickRateManager
{
    @Accessor("remainingSprintTicks")
    long servux_getStringTicks();
}
