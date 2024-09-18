package fi.dy.masa.servux.util;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

import javax.annotation.Nullable;

public class BlockUtils
{
    /**
     * Returns the first PropertyDirection property from the provided state, if any.
     * @param state
     * @return the first PropertyDirection, or null if there are no such properties
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static EnumProperty<Direction> getFirstDirectionProperty(BlockState state)
    {
        for (Property<?> prop : state.getProperties())
        {
            if (prop instanceof EnumProperty<?> enumProperty)
            {
                if (enumProperty.getType().equals(Direction.class))
                {
                    return (EnumProperty<Direction>) enumProperty;
                }
            }
        }

        return null;
    }

    /**
     * Returns the EnumFacing value of the first found PropertyDirection
     * type blockstate property in the given state, if any.
     * If there are no PropertyDirection properties, then null is returned.
     * @param state
     * @return
     */
    @Nullable
    public static Direction getFirstPropertyFacingValue(BlockState state)
    {
        return getPropertyFacingValue(state);
    }

    @Nullable
    public static Direction getPropertyFacingValue(BlockState state)
    {
        return state.contains(Properties.FACING) ? state.get(Properties.FACING) : null;
    }
}
