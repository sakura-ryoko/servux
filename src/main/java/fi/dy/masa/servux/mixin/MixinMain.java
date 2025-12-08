package fi.dy.masa.servux.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.servux.dataproviders.DataProviderManager;

@Mixin(Main.class)
public class MixinMain
{
    @Inject(method = "main", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;saveDataTag(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/storage/WorldData;)V",
            shift = At.Shift.AFTER))
    private static void servux_onCaptureImmutable(String[] args, CallbackInfo ci, @Local RegistryAccess.Frozen immutable)
    {
        DataProviderManager.INSTANCE.onCaptureImmutable(immutable);
    }
}
