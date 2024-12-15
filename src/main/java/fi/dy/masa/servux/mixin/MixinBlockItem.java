package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import fi.dy.masa.servux.util.PlacementHandler;
import fi.dy.masa.servux.util.PlacementHandler.UseContext;

/**
 * Should override Carpet-Extra's version with a higher priority
 */
@Mixin(value = BlockItem.class, priority = 1010)
public abstract class MixinBlockItem extends Item
{
    private MixinBlockItem(Settings builder)
    {
        super(builder);
    }

    @Shadow protected abstract boolean canPlace(ItemPlacementContext context, BlockState state);
    @Shadow public abstract Block getBlock();

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void modifyPlacementState(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir)
    {
        if (ctx.getPlayer() instanceof ServerPlayerEntity player)
        {
            if (ServuxConfigProvider.INSTANCE.hasPermission_EasyPlace(player) == false)
            {
                return;
            }
        }

        BlockState stateOrig = this.getBlock().getPlacementState(ctx);

        if (stateOrig != null && this.canPlace(ctx, stateOrig))
        {
            UseContext context = UseContext.from(ctx, ctx.getHand());
            cir.setReturnValue(PlacementHandler.applyPlacementProtocolV3(stateOrig, context));
        }
    }
}
