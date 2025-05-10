package fi.dy.masa.servux.util;

import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.math.Direction;

public class BlockUtils
{
    /**
     * Returns the first PropertyDirection property from the provided state, if any.
     *
     * @param state ()
     * @return the first PropertyDirection, or null if there are no such properties
     */
    @SuppressWarnings("unchecked")
    public static Optional<EnumProperty<Direction>> getFirstDirectionProperty(BlockState state)
    {
        for (Property<?> prop : state.getProperties())
        {
            if (prop instanceof EnumProperty<?> ep)
            {
                if (ep.getType().equals(Direction.class))
                {
                    return Optional.of((EnumProperty<Direction>) ep);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Returns the EnumFacing value of the first found PropertyDirection
     * type blockstate property in the given state, if any.
     * If there are no PropertyDirection properties, then null is returned.
     *
     * @param state ()
     * @return ()
     */
    public static Optional<Direction> getFirstPropertyFacingValue(BlockState state)
    {
        Optional<EnumProperty<Direction>> propOptional = getFirstDirectionProperty(state);
        return propOptional.map((directionProperty) -> Direction.byId(((Direction) state.get(directionProperty)).getId()));
    }

    @Nullable
    public static Direction getPropertyFacingValue(BlockState state)
    {
        return state.contains(Properties.FACING) ? state.get(Properties.FACING) : null;
    }

    public static BlockState fixMirrorDoubleChest(BlockState state, BlockMirror mirror, ChestType type)
    {
        Direction facing = state.get(ChestBlock.FACING);
        Direction.Axis axis = facing.getAxis();

        if (mirror == BlockMirror.FRONT_BACK) // x
        {
            state = state.with(ChestBlock.CHEST_TYPE, type.getOpposite());

            if (axis == Direction.Axis.X)
            {
                state = state.with(ChestBlock.FACING, facing.getOpposite());
            }
        }
        else if (mirror == BlockMirror.LEFT_RIGHT) // z
        {
            state = state.with(ChestBlock.CHEST_TYPE, type.getOpposite());

            if (axis == Direction.Axis.Z)
            {
                state = state.with(ChestBlock.FACING, facing.getOpposite());
            }
        }

        return state;
    }
}
