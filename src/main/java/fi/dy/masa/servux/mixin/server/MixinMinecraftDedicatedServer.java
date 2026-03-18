package fi.dy.masa.servux.mixin.server;

import java.util.Optional;

import com.mojang.datafixers.DataFixer;
import fi.dy.masa.servux.event.ServerInitHandler;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DedicatedServer.class)
public class MixinMinecraftDedicatedServer
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void servux_DedicatedServerInit(Thread serverThread, LevelStorageSource.LevelStorageAccess levelStorageSource,
                                            PackRepository packRepository, WorldStem worldStem, Optional<GameRules> gameRules,
                                            DedicatedServerSettings settings, DataFixer fixerUpper, Services services, CallbackInfo ci)
    {
        ((ServerInitHandler) ServerInitHandler.getInstance()).onServerInit();
    }
}
