package fi.dy.masa.servux.mixin.item;

import net.minecraft.core.TypedInstance;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.dataproviders.TweaksDataProvider;
import fi.dy.masa.servux.util.InventoryUtils;

@Mixin(ItemInstance.class)
public interface IMixinItemInstance extends TypedInstance<Item>, DataComponentGetter
{
	@Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
	private void servux_getMaxStackSizeStackSensitive(CallbackInfoReturnable<Integer> cir)
	{
		if (TweaksDataProvider.INSTANCE.shouldEmptyShulkersStack())
		{
			try
			{
				final ItemStack stack = (ItemStack) (Object) this;

				if (InventoryUtils.isShulkerBox(stack) &&
					InventoryUtils.shulkerBoxHasItems(stack) == false)
				{
					final int result = TweaksDataProvider.INSTANCE.getEmptyShulkersMaxCount(stack);

					if (this.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) < result)
					{
						cir.setReturnValue(result);
					}
				}
			}
			catch (Throwable ignored) {}
		}
	}
}
