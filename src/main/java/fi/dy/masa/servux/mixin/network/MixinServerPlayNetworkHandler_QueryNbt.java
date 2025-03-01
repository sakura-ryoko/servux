package fi.dy.masa.servux.mixin.network;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1005)
public class MixinServerPlayNetworkHandler_QueryNbt
{
    @Shadow public ServerPlayerEntity player;

    @ModifyConstant(method = "onQueryBlockNbt", constant = @Constant(intValue = 2))
    private int servux_onQueryBlockNbt(int constant)
    {
        if (EntitiesDataProvider.INSTANCE.hasNbtQueryOverride())
        {
            if (EntitiesDataProvider.INSTANCE.hasNbtQueryPermission(this.player))
            {
                //Servux.debugLog("received NbtQueryBlock request from: {}", this.player.getName().getLiteralString());
                return 0;
            }
            else
            {
                return 4;
            }
        }
        else
        {
            return constant;
        }
    }

    @ModifyConstant(method = "onQueryEntityNbt", constant = @Constant(intValue = 2))
    private int servux_onQueryEntityNbt(int constant)
    {
        if (EntitiesDataProvider.INSTANCE.hasNbtQueryOverride())
        {
            if (EntitiesDataProvider.INSTANCE.hasNbtQueryPermission(this.player))
            {
                //Servux.debugLog("received NbtQueryEntity request from: {}", this.player.getName().getLiteralString());
                return 0;
            }
            else
            {
                return 4;
            }
        }
        else
        {
            return constant;
        }
    }
}
