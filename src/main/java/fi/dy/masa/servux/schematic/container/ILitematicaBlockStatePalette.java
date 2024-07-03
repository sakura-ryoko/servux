package fi.dy.masa.servux.schematic.container;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtList;

import javax.annotation.Nullable;
import java.util.List;

public interface ILitematicaBlockStatePalette
{
    /**
     * Gets the palette id for the given block state and adds
     * the state to the palette if it doesn't exist there yet.
     */
    int idFor(BlockState state);

    /**
     * Gets the block state by the palette id.
     */
    @Nullable
    BlockState getBlockState(int indexKey);

    int getPaletteSize();

    void readFromNBT(NbtList tagList);

    NbtList writeToNBT();

    /**
     * Sets the current mapping of the palette.
     * This is meant for reading the palette from file.
     * @param list
     * @return true if the mapping was set successfully, false if it failed
     */
    boolean setMapping(List<BlockState> list);
}
