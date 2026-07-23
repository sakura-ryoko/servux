package fi.dy.masa.servux.mixin.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInputContextHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagValueInput.class)
public interface IMixinTagValueInput
{
    @Accessor("context")
    ValueInputContextHelper servux_getContext();

    @Accessor("input")
    CompoundTag servux_getNbt();
}
