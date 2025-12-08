package fi.dy.masa.servux.schematic.container;

import net.minecraft.world.level.block.state.BlockState;

public interface ILitematicaBlockStatePaletteResizer
{
    int onResize(int bits, BlockState state);
}
