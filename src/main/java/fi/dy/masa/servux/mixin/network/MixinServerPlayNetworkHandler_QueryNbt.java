package fi.dy.masa.servux.mixin.network;

import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1005)
public class MixinServerPlayNetworkHandler_QueryNbt
{
    @Shadow public ServerPlayerEntity player;

    @Redirect(method = "onQueryBlockNbt",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/command/permission/PermissionPredicate;hasPermission(Lnet/minecraft/command/permission/Permission;)Z"))
    private boolean servux_onQueryBlockNbt(PermissionPredicate instance, Permission permission)
    {
        if (EntitiesDataProvider.INSTANCE.hasNbtQueryOverride())
        {
	        //Servux.debugLog("received NbtQueryBlock request from: {}", this.player.getName().getLiteralString());
	        return EntitiesDataProvider.INSTANCE.hasNbtQueryPermission(this.player);
        }
        else
        {
            return instance.hasPermission(permission);
        }
    }

    @Redirect(method = "onQueryEntityNbt",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/command/permission/PermissionPredicate;hasPermission(Lnet/minecraft/command/permission/Permission;)Z"))
    private boolean servux_onQueryEntityNbt(PermissionPredicate instance, Permission permission)
    {
        if (EntitiesDataProvider.INSTANCE.hasNbtQueryOverride())
        {
	        //Servux.debugLog("received NbtQueryEntity request from: {}", this.player.getName().getLiteralString());
	        return EntitiesDataProvider.INSTANCE.hasNbtQueryPermission(this.player);
        }
        else
        {
            return instance.hasPermission(permission);
        }
    }
}
