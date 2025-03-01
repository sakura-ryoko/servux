package fi.dy.masa.servux.mixin.network;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1010)
public class MixinServerPlayNetworkHandler_EasyPlace
{
    @Shadow public ServerPlayerEntity player;

    @Redirect(method = "onPlayerInteractBlock", require = 0,
              at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Vec3d;subtract(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d servux$removeHitPosCheck(Vec3d hitVec, Vec3d blockCenter)
    {
        return Vec3d.ZERO;
        //return hitVec.subtract(blockCenter);
    }
}
