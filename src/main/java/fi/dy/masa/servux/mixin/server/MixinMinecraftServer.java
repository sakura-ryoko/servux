package fi.dy.masa.servux.mixin.server;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.HudDataProvider;
import fi.dy.masa.servux.event.ServerHandler;
import fi.dy.masa.servux.scheduler.TaskScheduler;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @Shadow private int tickCount;
    @Shadow public abstract ResourceManager getResourceManager();
	@Shadow protected abstract GlobalPos selectLevelLoadFocusPos();

	@Inject(method = "tickServer", at = @At(value = "RETURN", ordinal = 1))
    private void servux_onTickEnd(BooleanSupplier haveTime, CallbackInfo ci,
                                  @Local(name = "profiler") ProfilerFiller profiler)
    {
        profiler.push("servux_tick");
        DataProviderManager.INSTANCE.tickProviders((MinecraftServer) (Object) this, this.tickCount, profiler);
        TaskScheduler.getInstance().runTasks(profiler);
        profiler.pop();
    }

    @Inject(method = "prepareLevels",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/server/MinecraftServer;updateMobSpawningFlags()V",
					 shift = At.Shift.BEFORE)
    )
    private void servux_onPrepareStartRegion(CallbackInfo ci)
    {
        if (HudDataProvider.INSTANCE.isEnabled())
        {
            HudDataProvider.INSTANCE.setSpawnPos(this.selectLevelLoadFocusPos());
//            HudDataProvider.INSTANCE.setSpawnChunkRadius(i);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;initServer()Z"), method = "runServer")
    private void servux_onServerStarting(CallbackInfo ci)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerStarting((MinecraftServer) (Object) this);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;", ordinal = 0), method = "runServer")
    private void servux_onServerStarted(CallbackInfo ci)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerStarted((MinecraftServer) (Object) this);
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void servux_startResourceReload(Collection<String> packsToEnable, CallbackInfoReturnable<CompletableFuture<Void>> cir)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerResourceReloadPre((MinecraftServer) (Object) this, this.getResourceManager());
    }

    @Inject(method = "reloadResources", at = @At("TAIL"))
    private void servux_endResourceReload(Collection<String> packsToEnable, CallbackInfoReturnable<CompletableFuture<Void>> cir)
    {
        cir.getReturnValue().handleAsync((value, throwable) ->
        {
            ((ServerHandler) ServerHandler.getInstance()).onServerResourceReloadPost((MinecraftServer) (Object) this, this.getResourceManager(), throwable == null);
            return value;
        }, (MinecraftServer) (Object) this);
    }

    @Inject(at = @At("HEAD"), method = "stopServer")
    private void servux_onServerStopping(CallbackInfo info)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerStopping((MinecraftServer) (Object) this);
    }

    @Inject(at = @At("TAIL"), method = "stopServer")
    private void servux_onServerStopped(CallbackInfo info)
    {
        ((ServerHandler) ServerHandler.getInstance()).onServerStopped((MinecraftServer) (Object) this);
    }
}
