package fi.dy.masa.servux.util;

import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

public class BlockUtils
{
    /**
     * Returns the first PropertyDirection property from the provided state, if any.
     *
     * @param state (BlockState)
     * @return the first PropertyDirection, or null if there are no such properties
     */
    public static Optional<DirectionProperty> getFirstDirectionProperty(BlockState state)
    {
        for (Property<?> prop : state.getProperties())
        {
            if (prop instanceof DirectionProperty)
            {
                return Optional.of((DirectionProperty) prop);
            }
        }

        return Optional.empty();
    }

    /**
     * Returns the EnumFacing value of the first found PropertyDirection
     * type blockstate property in the given state, if any.
     * If there are no PropertyDirection properties, then null is returned.
     *
     * @param state
     * @return
     */
    public static Optional<Direction> getFirstPropertyFacingValue(BlockState state)
    {
        Optional<DirectionProperty> propOptional = getFirstDirectionProperty(state);
        return propOptional.map(directionProperty -> Direction.byId(state.get(directionProperty).getId()));
    }

    @Nullable
    public static Direction getPropertyFacingValue(BlockState state)
    {
        return state.contains(Properties.FACING) ? state.get(Properties.FACING) : null;
    }
}
