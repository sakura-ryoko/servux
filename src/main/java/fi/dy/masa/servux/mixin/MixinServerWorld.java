package fi.dy.masa.servux.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.servux.dataproviders.StructureDataProvider;

@Mixin(ServerWorld.class)
public class MixinServerWorld
{
    //@Final @Shadow private ServerWorldProperties worldProperties;
    @Shadow private int spawnChunkRadius;

    @Inject(method = "setSpawnPos", at = @At("TAIL"))
    private void servux_onSetSpawnPos(BlockPos pos, float angle, CallbackInfo ci)
    {
        StructureDataProvider.INSTANCE.setSpawnPos(pos);
        StructureDataProvider.INSTANCE.setSpawnChunkRadius((this.spawnChunkRadius - 1));
    }

    @Inject(method = "tickWeather()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerWorldProperties;setRaining(Z)V"))
    private void servux_onTickWeather(CallbackInfo ci)
    {
        /*
        int thunderTime = 0;
        int rainTime = 0;
        int clearTime = 0;

        // TODO --> Add Weather handling
        if (this.worldProperties.isThundering())
        {
            thunderTime = this.worldProperties.getThunderTime();
        }
        else if (this.worldProperties.isRaining())
        {
            rainTime = this.worldProperties.getRainTime();
        }
        else
        {
            clearTime = this.worldProperties.getClearWeatherTime();
        }
         */

        // Process
    }
}
