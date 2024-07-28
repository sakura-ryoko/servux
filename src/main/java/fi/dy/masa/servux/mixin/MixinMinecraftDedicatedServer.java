package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.datafixers.DataFixer;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.dedicated.ServerPropertiesLoader;
import net.minecraft.util.ApiServices;
import net.minecraft.world.level.storage.LevelStorage;
import fi.dy.masa.servux.event.ServerInitHandler;

@Mixin(MinecraftDedicatedServer.class)
public class MixinMinecraftDedicatedServer
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void servux_DedicatedServerInit(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager,
                                   SaveLoader saveLoader, ServerPropertiesLoader propertiesLoader, DataFixer dataFixer,
                                   ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci)
    {
        ((ServerInitHandler) ServerInitHandler.getInstance()).onServerInit();
    }
}
