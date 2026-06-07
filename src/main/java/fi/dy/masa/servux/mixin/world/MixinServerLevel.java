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
public abstract class MixinServerLevel
{
//    @Shadow private int spawnChunkRadius;
    @Shadow @NotNull public abstract MinecraftServer getServer();

    @Inject(method = "setRespawnData", at = @At("TAIL"))
    private void servux_onSetSpawnPos(LevelData.RespawnData respawnData, CallbackInfo ci)
    {
        if (HudDataProvider.INSTANCE.isEnabled())
        {
            HudDataProvider.INSTANCE.setSpawnPos(respawnData.globalPos());
//            HudDataProvider.INSTANCE.setSpawnChunkRadius((this.spawnChunkRadius - 1));
        }
    }

    @Inject(method = "advanceWeatherCycle()V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V")
    )
    private void servux_onTickWeather(CallbackInfo ci,
                                      @Local(name = "clearWeatherTime") int clearWeatherTime,
                                      @Local(name = "thunderTime") int thunderTime,
                                      @Local(name = "rainTime") int rainTime,
                                      @Local(name = "thundering") boolean thundering,
                                      @Local(name = "raining") boolean raining)
    {
        if (HudDataProvider.INSTANCE.isEnabled())
        {
            HudDataProvider.INSTANCE.tickWeather(clearWeatherTime, rainTime, thunderTime, raining, thundering);
        }
    }
}
