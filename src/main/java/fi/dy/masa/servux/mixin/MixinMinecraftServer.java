package fi.dy.masa.servux.mixin;

import java.util.function.BooleanSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.GameRules;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @Shadow private Profiler profiler;
    @Shadow private int ticks;
    @Shadow public abstract GameRules getGameRules();
    @Shadow public abstract ServerWorld getOverworld();

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickEnd(BooleanSupplier supplier, CallbackInfo ci)
    {
        this.profiler.push("servux_tick");
        DataProviderManager.INSTANCE.tickProviders((MinecraftServer) (Object) this, this.ticks);
        this.profiler.pop();
    }

    @Inject(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setSpawnPos(Lnet/minecraft/util/math/BlockPos;F)V", shift = At.Shift.AFTER))
    private void checkSpawnChunkRadius(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci)
    {
        BlockPos worldSpawn = this.getOverworld().getSpawnPos();
        int radius = this.getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS);
        Servux.printDebug("MixinMinecraftServer#servux_checkSpawnChunkRadius(): Spawn Position: {}, SPAWN_CHUNK_RADIUS: {}", this.getOverworld().getSpawnPos().toShortString(), radius);

        if (StructureDataProvider.INSTANCE.getSpawnPos() != worldSpawn)
        {
            // Only set if value changed from stored value
            StructureDataProvider.INSTANCE.setSpawnPos(worldSpawn);
        }
        StructureDataProvider.INSTANCE.setSpawnChunkRadius(radius);
    }
}
