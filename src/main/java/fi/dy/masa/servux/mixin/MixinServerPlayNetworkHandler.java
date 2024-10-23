package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1005)
public class MixinServerPlayNetworkHandler
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

    @ModifyConstant(method = "onQueryBlockNbt", constant = @Constant(intValue = 2))
    private int servux_onQueryBlockNbt(int constant)
    {
        if (EntitiesDataProvider.INSTANCE.hasNbtQueryOverride())
        {
            if (EntitiesDataProvider.INSTANCE.hasNbtQueryPermission(this.player))
            {
                //Servux.debugLog("received NbtQueryBlock request from: {}", this.player.getName().getLiteralString());
                return 0;
            }
            else
            {
                return 4;
            }
        }
        else
        {
            return constant;
        }
    }

    @ModifyConstant(method = "onQueryEntityNbt", constant = @Constant(intValue = 2))
    private int servux_onQueryEntityNbt(int constant)
    {
        if (EntitiesDataProvider.INSTANCE.hasNbtQueryOverride())
        {
            if (EntitiesDataProvider.INSTANCE.hasNbtQueryPermission(this.player))
            {
                //Servux.debugLog("received NbtQueryEntity request from: {}", this.player.getName().getLiteralString());
                return 0;
            }
            else
            {
                return 4;
            }
        }
        else
        {
            return constant;
        }
    }
}
