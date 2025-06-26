package fi.dy.masa.servux.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.servux.dataproviders.TweaksDataProvider;
import fi.dy.masa.servux.util.InventoryUtils;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <a href="https://github.com/kikugie/stackable-shulkers-fix">...</a> by KikuGie
 * Priority 999 if installed with stackable-shulkers-fix
 */
@Mixin(value = HopperBlockEntity.class, priority = 999)
public class MixinHopperBlockEntity
{
    @WrapOperation(
            method = "isFull",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/item/ItemStack;getMaxCount()I")
    )
    private int servux_modifyShulkerMaxCount(ItemStack instance, Operation<Integer> original)
    {
        if (TweaksDataProvider.INSTANCE.isStackableShulkersFixActive())
        {
            return InventoryUtils.isShulkerBox(instance) ? instance.getCount() : original.call(instance);
        }

        return original.call(instance);
    }

    @WrapOperation(
            method = "isInventoryFull",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/item/ItemStack;getMaxCount()I")
    )
    private static int servux_modifyShulkerMaxCountStatic(ItemStack instance, Operation<Integer> original)
    {
        if (TweaksDataProvider.INSTANCE.isStackableShulkersFixActive())
        {
            return InventoryUtils.isShulkerBox(instance) ? 1 : original.call(instance);
        }

        return original.call(instance);
    }

    @Inject(
            method = "canMergeItems",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void servux_cancelItemMerging(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir)
    {
        if (TweaksDataProvider.INSTANCE.isStackableShulkersFixActive())
        {
            if (InventoryUtils.isShulkerBox(first) || InventoryUtils.isShulkerBox(second)) cir.setReturnValue(false);
        }
    }
}
