package fi.dy.masa.servux.mixin.server;

import java.nio.file.Paths;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Main;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import fi.dy.masa.servux.dataproviders.DataProviderManager;

@Mixin(Main.class)
public class MixinMain
{
    // Apparently, 1.21.11 breaks here
    @WrapOperation(method = "main",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;saveDataTag(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/storage/WorldData;)V"
                   )
    )
    private static void servux_onCaptureImmutable(LevelStorageSource.LevelStorageAccess instance, RegistryAccess registryAccess, WorldData worldData, Operation<Void> original)
    {
        DataProviderManager.INSTANCE.onCaptureImmutable((RegistryAccess.Frozen) registryAccess);
        DataProviderManager.INSTANCE.onCaptureRootDir(Paths.get("server.properties"));
        original.call(instance, registryAccess, worldData);
    }
}
