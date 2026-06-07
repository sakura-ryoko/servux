package fi.dy.masa.servux.mixin.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;

import static net.minecraft.world.level.block.StairBlock.FACING;
import static net.minecraft.world.level.block.StairBlock.SHAPE;

@Mixin(StairBlock.class)
public abstract class MixinStairBlock extends Block
{
    public MixinStairBlock(Properties settings)
    {
        super(settings);
    }

    @Inject(method = "mirror", at = @At(value = "HEAD"), cancellable = true)
    private void servux_fixStairsMirror(BlockState state, Mirror mirror, CallbackInfoReturnable<BlockState> cir)
    {
        if (LitematicsDataProvider.INSTANCE.isEnabled() &&
            LitematicsDataProvider.INSTANCE.fixStairMirror.getValue())
        {
            Direction direction = state.getValue(FACING);
            StairsShape stairShape = state.getValue(SHAPE);

            // Fixes X Axis for FRONT_BACK being inverted for INNER_LEFT / INNER_RIGHT
            if (direction.getAxis() == Direction.Axis.X && mirror == Mirror.FRONT_BACK)
            {
                cir.setReturnValue(
                        switch (stairShape)
                        {
                            case INNER_LEFT  -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                            case INNER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                            case OUTER_LEFT  -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                            case OUTER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                            default          -> state.rotate(Rotation.CLOCKWISE_180);
                        }
                );

                cir.cancel();
            }
            // Fixes missing Axis STAIR_SHAPE flips
            else if ((direction.getAxis() == Direction.Axis.X && mirror == Mirror.LEFT_RIGHT) ||
                     (direction.getAxis() == Direction.Axis.Z && mirror == Mirror.FRONT_BACK))
            {
                cir.setReturnValue(
                        switch (stairShape)
                        {
                            case INNER_LEFT  -> state.setValue(SHAPE, StairsShape.INNER_RIGHT);
                            case INNER_RIGHT -> state.setValue(SHAPE, StairsShape.INNER_LEFT);
                            case OUTER_LEFT  -> state.setValue(SHAPE, StairsShape.OUTER_RIGHT);
                            case OUTER_RIGHT -> state.setValue(SHAPE, StairsShape.OUTER_LEFT);
                            default          -> state;
                        }
                );

                cir.cancel();
            }
        }
    }
}
