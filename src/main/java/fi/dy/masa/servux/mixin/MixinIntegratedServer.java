package fi.dy.masa.servux.mixin;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.ServuxReference;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServer
{
    @Inject(method = "openToLan", at = @At("RETURN"))
    private void servux_checkOpenToLan(GameMode gameMode, boolean cheatsAllowed, int port, CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValue())
        {
            Servux.logger.info("Servux OpenToLan Mode detected.  Listening for Clients.");
            ServuxReference.setOpenToLan(true);
        }
    }
}
