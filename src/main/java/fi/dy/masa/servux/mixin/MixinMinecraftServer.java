package fi.dy.masa.servux.mixin;

import java.util.function.BooleanSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @Shadow private Profiler profiler;
    @Shadow private int ticks;

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickEnd(BooleanSupplier supplier, CallbackInfo ci)
    {
        this.profiler.push("servux_tick");
        DataProviderManager.INSTANCE.tickProviders((MinecraftServer) (Object) this, this.ticks);
        this.profiler.pop();
    }

    @Inject(method = "prepareStartRegion", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/MathHelper;square(I)I", shift = At.Shift.BEFORE),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void onPrepareStartRegion(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci,
                                      ServerWorld serverWorld, BlockPos blockPos, ServerChunkManager serverChunkManager, int i)
    {
        if (StructureDataProvider.INSTANCE.getSpawnPos() != blockPos)
        {
            StructureDataProvider.INSTANCE.setSpawnPos(blockPos);
        }
        if (StructureDataProvider.INSTANCE.getSpawnChunkRadius() != i)
        {
            StructureDataProvider.INSTANCE.setSpawnChunkRadius(i);
        }
    }
}
