package fi.dy.masa.servux.mixin.item;

import fi.dy.masa.servux.dataproviders.TweaksDataProvider;
import fi.dy.masa.servux.util.InventoryUtils;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack
{
    @Shadow public abstract ComponentMap getComponents();

    @Inject(method = "getMaxCount", at = @At("RETURN"), cancellable = true)
    public void servux_getMaxStackSizeStackSensitive(CallbackInfoReturnable<Integer> cir)
    {
        if (TweaksDataProvider.INSTANCE.shouldEmptyShulkersStack() &&
            InventoryUtils.isShulkerBox((ItemStack) (Object) this) &&
            InventoryUtils.shulkerBoxHasItems((ItemStack) (Object) this) == false)
        {
            final int result = TweaksDataProvider.INSTANCE.getEmptyShulkersMaxCount((ItemStack) (Object) this);

            if (this.getComponents().getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1) < result)
            {
                cir.setReturnValue(result);
            }
        }
    }
}
