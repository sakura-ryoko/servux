package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.Vec3d;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1005)
public class MixinServerPlayNetworkHandler
{
    @Redirect(method = "onPlayerInteractBlock", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Vec3d;subtract(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d servux$removeHitPosCheck(Vec3d hitVec, Vec3d blockCenter)
    {
        return Vec3d.ZERO;
        //return hitVec.subtract(blockCenter);
    }
}
