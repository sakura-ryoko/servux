package fi.dy.masa.servux.mixin.world;

import fi.dy.masa.servux.util.WorldUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LevelChunk.class)
public abstract class MixinWorldChunk_UpdateSuppression
{
    @Redirect(method = "setBlockState",
                slice = @Slice(from = @At(value = "INVOKE",
                                target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;")),
                at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isClientSide()Z", ordinal = 0))
    private boolean servux_redirectIsRemote(Level world)
    {
        return WorldUtils.shouldPreventBlockUpdates(world) || world.isClientSide();
    }
}
