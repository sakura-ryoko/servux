package fi.dy.masa.servux.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.servux.dataproviders.HudDataProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class MixinServerWorld
{
//    @Shadow private int spawnChunkRadius;
    @Shadow @NotNull public abstract MinecraftServer getServer();

    @Inject(method = "setRespawnData", at = @At("TAIL"))
    private void servux_onSetSpawnPos(LevelData.RespawnData spawnPoint, CallbackInfo ci)
    {
        if (HudDataProvider.INSTANCE.isEnabled())
        {
            HudDataProvider.INSTANCE.setSpawnPos(spawnPoint.globalPos());
//            HudDataProvider.INSTANCE.setSpawnChunkRadius((this.spawnChunkRadius - 1));
        }
    }

    @Inject(method = "advanceWeatherCycle()V", at = @At(value = "INVOKE",
                                                target = "Lnet/minecraft/world/level/storage/ServerLevelData;setRaining(Z)V"))
    private void servux_onTickWeather(CallbackInfo ci,
                                      @Local(ordinal = 0) int i, @Local(ordinal = 1) int j, @Local(ordinal = 2) int k,
                                      @Local(ordinal = 1) boolean bl2, @Local(ordinal = 2) boolean bl3)
    {
        /*
        this.worldProperties.setThunderTime(j);
        this.worldProperties.setRainTime(k);
        this.worldProperties.setClearWeatherTime(i);
        this.worldProperties.setThundering(bl2);
        this.worldProperties.setRaining(bl3);
         */

        if (HudDataProvider.INSTANCE.isEnabled())
        {
            HudDataProvider.INSTANCE.tickWeather(i, k, j, bl3, bl2);
        }
    }
}
