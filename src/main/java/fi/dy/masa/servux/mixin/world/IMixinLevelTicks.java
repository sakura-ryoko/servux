package fi.dy.masa.servux.mixin.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelTicks.class)
public interface IMixinLevelTicks<T>
{
    @Accessor("allContainers")
    Long2ObjectMap<LevelChunkTicks<@NotNull T>> servux_getChunkTickSchedulers();
}
