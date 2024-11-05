package fi.dy.masa.servux.mixin;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.registry.DynamicRegistryManager;
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
                                      target = "Lnet/minecraft/world/level/storage/LevelStorage$Session;backupLevelDataFile(Lnet/minecraft/registry/DynamicRegistryManager;Lnet/minecraft/world/SaveProperties;)V",
                                      shift = At.Shift.AFTER))
    private static void servux_captureImmutable(String[] args, CallbackInfo ci, @Local DynamicRegistryManager.Immutable immutable)
    {
        DataProviderManager.INSTANCE.onCaptureImmutable(immutable);
    }
}