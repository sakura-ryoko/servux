package fi.dy.masa.servux.mixin.world;

import org.objectweb.asm.Opcodes;

import fi.dy.masa.servux.util.WorldUtils;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk_UpdateSuppression
{
    @Redirect(method = "setBlockState",
                slice = @Slice(from = @At(value = "INVOKE",
                                target = "Lnet/minecraft/world/chunk/ChunkSection;getBlockState(III)" +
                                          "Lnet/minecraft/block/BlockState;")),
                at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;isClient:Z",
                         ordinal = 0,
                         opcode = Opcodes.GETFIELD))
    private boolean servux_redirectIsRemote(World world)
    {
        return WorldUtils.shouldPreventBlockUpdates(world) || world.isClient;
    }
}
