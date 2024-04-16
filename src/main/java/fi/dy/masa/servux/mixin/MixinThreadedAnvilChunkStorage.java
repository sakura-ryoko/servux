package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.servux.dataproviders.StructureDataProvider;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage
{
    @Inject(method = "track(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/chunk/WorldChunk;)V",
            at = @At("HEAD"))
    private static void servux_onSendChunkPacket(ServerPlayerEntity player,
                                                 WorldChunk chunk,
                                                 CallbackInfo ci)
    {
        if (StructureDataProvider.INSTANCE.isEnabled())
        {
            StructureDataProvider.INSTANCE.onStartedWatchingChunk(player, chunk);
        }
    }
}
