package fi.dy.masa.servux.util.game;

import java.util.Iterator;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

public class BlockUtils
{
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');
    private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

    /**
     * Returns the first PropertyDirection property from the provided state, if any.
     *
     * @param state ()
     * @return the first PropertyDirection, or null if there are no such properties
     */
    @SuppressWarnings("unchecked")
    public static Optional<EnumProperty<@NotNull Direction>> getFirstDirectionProperty(BlockState state)
    {
        for (Property<?> prop : state.getProperties())
        {
            if (prop instanceof EnumProperty<?> ep)
            {
                if (ep.getValueClass().equals(Direction.class))
                {
                    return Optional.of((EnumProperty<@NotNull Direction>) ep);
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
        Optional<EnumProperty<@NotNull Direction>> propOptional = getFirstDirectionProperty(state);
        return propOptional.map((directionProperty) -> Direction.byName(((Direction) state.getValue(directionProperty)).getName()));
    }

    @Nullable
    public static Direction getPropertyFacingValue(BlockState state)
    {
        return state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : null;
    }

    public static BlockState fixMirrorDoubleChest(BlockState state, Mirror mirror, ChestType type)
    {
        Direction facing = state.getValue(ChestBlock.FACING);
        Direction.Axis axis = facing.getAxis();

        if (mirror == Mirror.FRONT_BACK) // x
        {
            state = state.setValue(ChestBlock.TYPE, type.getOpposite());

            if (axis == Direction.Axis.X)
            {
                state = state.setValue(ChestBlock.FACING, facing.getOpposite());
            }
        }
        else if (mirror == Mirror.LEFT_RIGHT) // z
        {
            state = state.setValue(ChestBlock.TYPE, type.getOpposite());

            if (axis == Direction.Axis.Z)
            {
                state = state.setValue(ChestBlock.FACING, facing.getOpposite());
            }
        }

        return state;
    }

    /**
     * Parses the provided string into the full block state.<br>
     * The string should be in either one of the following formats:<br>
     * 'minecraft:stone' or 'minecraft:smooth_stone_slab[half=top,waterlogged=false]'
     */
    public static Optional<BlockState> getBlockStateFromString(String str)
    {
        int index = str.indexOf("["); // [f=b]
        String blockName = index != -1 ? str.substring(0, index) : str;

        try
        {
            Identifier id = Identifier.tryParse(blockName);

            if (id != null && BuiltInRegistries.BLOCK.containsKey(id))
            {
                Optional<Holder.Reference<@NotNull Block>> opt = BuiltInRegistries.BLOCK.get(id);
                Block block;

                if (opt.isPresent())
                {
                    block = opt.get().value();
                    BlockState state = block.defaultBlockState();

                    if (index != -1 && str.length() > (index + 4) && str.charAt(str.length() - 1) == ']')
                    {
                        StateDefinition<@NotNull Block, @NotNull BlockState> stateManager = block.getStateDefinition();
                        String propStr = str.substring(index + 1, str.length() - 1);

                        for (String propAndVal : COMMA_SPLITTER.split(propStr))
                        {
                            Iterator<String> valIter = EQUAL_SPLITTER.split(propAndVal).iterator();

                            if (valIter.hasNext() == false)
                            {
                                continue;
                            }

                            Property<?> prop = stateManager.getProperty(valIter.next());

                            if (prop == null || valIter.hasNext() == false)
                            {
                                continue;
                            }

                            Comparable<?> val = getPropertyValueByName(prop, valIter.next());

                            if (val != null)
                            {
                                state = getBlockStateWithProperty(state, prop, val);
                            }
                        }
                    }

                    return Optional.of(state);
                }
            }
        }
        catch (Exception e)
        {
            return Optional.empty();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> BlockState getBlockStateWithProperty(BlockState state, Property<@NotNull T> prop, Comparable<?> value)
    {
        return state.setValue(prop, (T) value);
    }

    @Nullable
    public static <T extends Comparable<T>> T getPropertyValueByName(Property<@NotNull T> prop, String valStr)
    {
        return prop.getValue(valStr).orElse(null);
    }
}
