package fi.dy.masa.servux.mixin.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1010)
public class MixinServerPlayNetworkHandler_EasyPlace
{
    @Shadow public ServerPlayer player;

    @Redirect(method = "handleUseItemOn", require = 0,
              at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 servux$removeHitPosCheck(Vec3 hitVec, Vec3 blockCenter)
    {
        return Vec3.ZERO;
    }
}
