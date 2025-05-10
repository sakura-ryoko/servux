package fi.dy.masa.servux.mixin.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.BlockMirror;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.util.BlockUtils;

@Mixin(ChestBlock.class)
public class MixinChestBlock
{
    @Inject(method = "mirror", at = @At("HEAD"), cancellable = true)
    private void servux_fixChestMirror(BlockState state, BlockMirror mirror, CallbackInfoReturnable<BlockState> cir)
    {
        ChestType type = state.get(ChestBlock.CHEST_TYPE);

        if (LitematicsDataProvider.INSTANCE.isEnabled() &&
            LitematicsDataProvider.INSTANCE.fixChestMirror.getValue()
            && type != ChestType.SINGLE)
        {
            state = BlockUtils.fixMirrorDoubleChest(state, mirror, type);
            cir.setReturnValue(state);
        }
    }
}
