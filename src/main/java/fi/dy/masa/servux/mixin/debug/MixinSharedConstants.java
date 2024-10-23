package fi.dy.masa.servux.mixin.debug;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.servux.dataproviders.DebugDataProvider;

@Mixin(SharedConstants.class)
public abstract class MixinSharedConstants
{
    @Shadow @Mutable
    public static boolean isDevelopment;

    public MixinSharedConstants() {}

    /*
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void servux_enableServerDevelopmentMode(CallbackInfo ci)
    {
        if (DebugDataProvider.INSTANCE.isEnabled() &&
            DebugDataProvider.INSTANCE.isServerDevelopmentMode())
        {
            isDevelopment = true;
        }
    }
     */
}
