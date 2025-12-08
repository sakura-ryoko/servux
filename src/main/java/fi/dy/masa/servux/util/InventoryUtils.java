package fi.dy.masa.servux.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class InventoryUtils
{
    public static boolean isShulkerBox(ItemStack stack)
    {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static boolean shulkerBoxHasItems(ItemStack stack)
    {
        ItemContainerContents container = stack.getComponents().get(DataComponents.CONTAINER);

        if (container != null)
        {
            return container.nonEmptyItems().iterator().hasNext();
        }

        return false;
    }
}
