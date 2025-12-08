package fi.dy.masa.servux.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1005)
public class MixinServerPlayNetworkHandler_QueryNbt
{
    @Shadow public ServerPlayer player;

    @Redirect(method = "handleBlockEntityTagQuery",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"))
    private boolean servux_onQueryBlockNbt(PermissionSet instance, Permission permission)
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

    @Redirect(method = "handleEntityTagQuery",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"))
    private boolean servux_onQueryEntityNbt(PermissionSet instance, Permission permission)
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
