package fi.dy.masa.servux.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldTickScheduler.class)
public interface IMixinWorldTickScheduler<T>
{
    @Accessor("chunkTickSchedulers")
    Long2ObjectMap<ChunkTickScheduler<T>> servux_getChunkTickSchedulers();
}
