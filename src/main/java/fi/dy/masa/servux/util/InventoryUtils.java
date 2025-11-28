package fi.dy.masa.servux.util;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public class InventoryUtils
{
    public static boolean isShulkerBox(ItemStack stack)
    {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static boolean shulkerBoxHasItems(ItemStack stack)
    {
        ContainerComponent container = stack.getComponents().get(DataComponentTypes.CONTAINER);

        if (container != null)
        {
            return container.iterateNonEmpty().iterator().hasNext();
        }

        return false;
    }
}
