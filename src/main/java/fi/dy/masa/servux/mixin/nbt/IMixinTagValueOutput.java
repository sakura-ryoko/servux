package fi.dy.masa.servux.mixin.nbt;

import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagValueOutput.class)
public interface IMixinTagValueOutput
{
    @Accessor("ops")
    DynamicOps<?> servux_getOps();

    @Accessor("output")
    CompoundTag servux_getNbt();
}
