package fi.dy.masa.servux.schematic.container;

import java.util.List;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import fi.dy.masa.servux.dataproviders.DataProviderManager;

public class LitematicaBlockStatePaletteHashMap implements ILitematicaBlockStatePalette
{
    private final CrudeIncrementalIntIdentityHashBiMap<@NotNull BlockState> statePaletteMap;
    private final ILitematicaBlockStatePaletteResizer paletteResizer;
    private final int bits;

    public LitematicaBlockStatePaletteHashMap(int bitsIn, ILitematicaBlockStatePaletteResizer paletteResizer)
    {
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
        this.statePaletteMap = CrudeIncrementalIntIdentityHashBiMap.create(1 << bitsIn);
    }

    @Override
    public int idFor(BlockState state)
    {
        int i = this.statePaletteMap.getId(state);

        if (i == -1)
        {
            i = this.statePaletteMap.add(state);

            if (i >= (1 << this.bits))
            {
                i = this.paletteResizer.onResize(this.bits + 1, state);
            }
        }

        return i;
    }

    @Override
    @Nullable
    public BlockState getBlockState(int indexKey)
    {
        return this.statePaletteMap.byId(indexKey);
    }

    @Override
    public int getPaletteSize()
    {
        return this.statePaletteMap.size();
    }

    private void requestNewId(BlockState state)
    {
        final int origId = this.statePaletteMap.add(state);

        if (origId >= (1 << this.bits))
        {
            int newId = this.paletteResizer.onResize(this.bits + 1, LitematicaBlockStateContainer.AIR_BLOCK_STATE);

            if (newId <= origId)
            {
                this.statePaletteMap.add(state);
            }
        }
    }

    @Override
    public void readFromNBT(ListTag tagList)
    {
        //RegistryEntryLookup<Block> lookup = Registries.BLOCK.getReadOnlyWrapper();
        HolderGetter<@NotNull Block> lookup = DataProviderManager.INSTANCE.getRegistryManager().lookupOrThrow(Registries.BLOCK);
        // Ugly, but it should work, without changing the ILitematicaBlockStatePalette interface.

        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompoundOrEmpty(i);
            BlockState state = NbtUtils.readBlockState(lookup, tag);

            if (i > 0 || state != LitematicaBlockStateContainer.AIR_BLOCK_STATE)
            {
                this.requestNewId(state);
            }
        }
    }

    @Override
    public ListTag writeToNBT()
    {
        ListTag tagList = new ListTag();

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            BlockState state = this.statePaletteMap.byId(id);

            if (state == null)
            {
                state = LitematicaBlockStateContainer.AIR_BLOCK_STATE;
            }

            CompoundTag tag = NbtUtils.writeBlockState(state);
            tagList.add(tag);
        }

        return tagList;
    }

    @Override
    public boolean setMapping(List<BlockState> list)
    {
        this.statePaletteMap.clear();

        for (BlockState blockState : list)
        {
            this.statePaletteMap.add(blockState);
        }

        return true;
    }
}
