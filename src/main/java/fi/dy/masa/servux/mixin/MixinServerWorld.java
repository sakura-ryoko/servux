package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

@Mixin(ServerWorld.class)
public class MixinServerWorld
{
    @Shadow private int spawnChunkRadius;

    @Inject(method = "setSpawnPos", at = @At("TAIL"))
    private void checkSpawnPos(BlockPos pos, float angle, CallbackInfo ci)
    {
        // Decrement SPAWN_CHUNK_RADIUS by 1 here to get the real value.
        Servux.printDebug("MixinServerWorld#checkSpawnPos(): Spawn Position: {}, SPAWN_CHUNK_RADIUS: {}", pos.toShortString(), (this.spawnChunkRadius - 1));

        StructureDataProvider.INSTANCE.setSpawnPos(pos);
        StructureDataProvider.INSTANCE.setSpawnChunkRadius((this.spawnChunkRadius - 1));
    }
}
