package fi.dy.masa.servux.mixin.item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import fi.dy.masa.servux.util.PlacementHandler;
import fi.dy.masa.servux.util.PlacementHandler.UseContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Should override Carpet-Extra's version with a higher priority
 */
@Mixin(value = BlockItem.class, priority = 950)
public abstract class MixinBlockItem_EasyPlace extends Item
{
    private MixinBlockItem_EasyPlace(Properties builder)
    {
        super(builder);
    }

    @Shadow protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);
    @Shadow public abstract Block getBlock();

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void servux_modifyPlacementState(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir)
    {
        if (ctx.getPlayer() instanceof ServerPlayer player)
        {
            if (ServuxConfigProvider.INSTANCE.hasPermission_EasyPlace(player) == false)
            {
                return;
            }
        }

        BlockState stateOrig = this.getBlock().getStateForPlacement(ctx);

		if (stateOrig != null)
		{

			if (!ServuxConfigProvider.INSTANCE.isEasyPlaceValidatorEnabled() || this.canPlace(ctx, stateOrig))
			{
				UseContext context = UseContext.from(ctx, ctx.getHand());
				cir.setReturnValue(PlacementHandler.applyPlacementProtocolV3(stateOrig, context));
			}
		}
    }
}
