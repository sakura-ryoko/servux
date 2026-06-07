package fi.dy.masa.servux.mixin.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 950)
public class MixinServerGamePacketListenerImpl_easyPlace
{
    @Shadow public ServerPlayer player;

    @WrapOperation(method = "handleUseItemOn", require = 0,
                   at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 servux$removeHitPosCheck(Vec3 instance, Vec3 vec3, Operation<Vec3> original)
    {
        return Vec3.ZERO;
    }
}
