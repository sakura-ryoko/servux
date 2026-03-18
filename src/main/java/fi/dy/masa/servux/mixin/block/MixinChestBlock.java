package fi.dy.masa.servux.mixin.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.util.game.BlockUtils;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

@Mixin(ChestBlock.class)
public class MixinChestBlock
{
    @Inject(method = "mirror", at = @At("HEAD"), cancellable = true)
    private void servux_fixChestMirror(BlockState state, Mirror mirror, CallbackInfoReturnable<BlockState> cir)
    {
        ChestType type = state.getValue(ChestBlock.TYPE);

        if (LitematicsDataProvider.INSTANCE.isEnabled() &&
            LitematicsDataProvider.INSTANCE.fixChestMirror.getValue()
            && type != ChestType.SINGLE)
        {
            state = BlockUtils.fixMirrorDoubleChest(state, mirror, type);
            cir.setReturnValue(state);
        }
    }
}
