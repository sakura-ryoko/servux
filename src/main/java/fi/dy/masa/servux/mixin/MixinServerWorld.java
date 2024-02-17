package fi.dy.masa.servux.mixin;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.data.StructureDataProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class MixinServerWorld
{
    @Shadow private int spawnChunkRadius;

    @Inject(method = "setSpawnPos", at = @At("TAIL"))
    private void servux_checkSpawnPos(BlockPos pos, float angle, CallbackInfo ci)
    {
        // Decrement SPAWN_CHUNK_RADIUS by 1 here to get the real value.
        Servux.printDebug("MixinServerWorld#servux_checkSpawnPos(): Spawn Position: {}, SPAWN_CHUNK_RADIUS: {}", pos.toShortString(), (this.spawnChunkRadius - 1));
        StructureDataProvider.INSTANCE.setSpawnPos(pos);
        StructureDataProvider.INSTANCE.setSpawnChunkRadius((this.spawnChunkRadius - 1));
    }
}
