package fi.dy.masa.servux.mixin;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.HudDataProvider;
import fi.dy.masa.servux.event.ServerHandler;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @Shadow private int ticks;
    @Shadow public abstract ResourceManager getResourceManager();

    @Inject(method = "tick", at = @At(value = "RETURN", ordinal = 1))
    private void servux_onTickEnd(BooleanSupplier supplier, CallbackInfo ci, @Local Profiler profiler)
    {
        profiler.push("servux_tick");
        DataProviderManager.INSTANCE.tickProviders((MinecraftServer) (Object) this, this.ticks, profiler);
        profiler.pop();
    }

    @Inject(method = "prepareStartRegion", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/MathHelper;square(I)I", shift = At.Shift.BEFORE)
    )
    private void servux_onPrepareStartRegion(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci,
                                             @Local BlockPos blockPos, @Local int i)
    {
        HudDataProvider.INSTANCE.setSpawnPos(blockPos);
        HudDataProvider.INSTANCE.setSpawnChunkRadius(i);
    }

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

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void servux_startResourceReload(Collection<String> collection, CallbackInfoReturnable<CompletableFuture<Void>> cir)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerResourceReloadPre((MinecraftServer) (Object) this, this.getResourceManager());
    }

    @Inject(method = "reloadResources", at = @At("TAIL"))
    private void servux_endResourceReload(Collection<String> collection, CallbackInfoReturnable<CompletableFuture<Void>> cir)
    {
        cir.getReturnValue().handleAsync((value, throwable) ->
        {
            ((ServerHandler) ServerHandler.getInstance()).onServerResourceReloadPost((MinecraftServer) (Object) this, this.getResourceManager(), throwable == null);
            return value;
        }, (MinecraftServer) (Object) this);
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
}
