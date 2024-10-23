package fi.dy.masa.servux.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.servux.dataproviders.HudDataProvider;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld
{
    @Shadow private int spawnChunkRadius;

    @Shadow @NotNull public abstract MinecraftServer getServer();

    @Inject(method = "setSpawnPos", at = @At("TAIL"))
    private void servux_onSetSpawnPos(BlockPos pos, float angle, CallbackInfo ci)
    {
        HudDataProvider.INSTANCE.setSpawnPos(pos);
        HudDataProvider.INSTANCE.setSpawnChunkRadius((this.spawnChunkRadius - 1));
    }

    @Inject(method = "tickWeather()V", at = @At(value = "INVOKE",
                                                target = "Lnet/minecraft/world/level/ServerWorldProperties;setRaining(Z)V"))
    private void servux_onTickWeather(CallbackInfo ci,
                                      @Local(ordinal = 0) int i, @Local(ordinal = 1) int j, @Local(ordinal = 2) int k,
                                      @Local(ordinal = 1) boolean bl2)
    {
        HudDataProvider.INSTANCE.tickWeather(i, bl2 ? j : k, bl2);
    }
}
