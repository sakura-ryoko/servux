package fi.dy.masa.servux.mixin;

import java.util.function.BooleanSupplier;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.data.StructureDataProvider;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.profiler.Profiler;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @Shadow private Profiler profiler;
    @Shadow private int ticks;
    @Shadow public abstract GameRules getGameRules();
    @Shadow public abstract ServerWorld getOverworld();

    @Inject(method = "tick", at = @At("RETURN"))
    private void servux_onTickEnd(BooleanSupplier supplier, CallbackInfo ci)
    {
        this.profiler.push("servux_tick");
        DataProviderManager.INSTANCE.tickProviders((MinecraftServer) (Object) this, this.ticks);
        this.profiler.pop();
    }

    /*  Moved to MaLiLib

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;setupServer()Z"), method = "runServer")
    private void servux_onServerStarting(CallbackInfo ci)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerStarting((MinecraftServer) (Object) this);
    }
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;createMetadata()Lnet/minecraft/server/ServerMetadata;", ordinal = 0), method = "runServer")
    private void servux_onServerStarted(CallbackInfo ci)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerStarted((MinecraftServer) (Object) this);
    }
    @Inject(at = @At("HEAD"), method = "shutdown")
    private void servux_onServerStopping(CallbackInfo info)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerStopping((MinecraftServer) (Object) this);
    }

    @Inject(at = @At("TAIL"), method = "shutdown")
    private void servux_onServerStopped(CallbackInfo info)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerStopped((MinecraftServer) (Object) this);
    }
     */

    @Inject(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setSpawnPos(Lnet/minecraft/util/math/BlockPos;F)V", shift = At.Shift.AFTER))
    private void servux_checkSpawnChunkRadius(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci)
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
