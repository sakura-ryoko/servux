package fi.dy.masa.servux.mixin.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1005)
public class MixinServerGamePacketListenerImpl_queryNbt
{
    @Shadow public ServerPlayer player;

    @WrapOperation(method = "handleBlockEntityTagQuery",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"))
    private boolean servux_onQueryBlockNbt(PermissionSet instance, Permission permission, Operation<Boolean> original)
    {
        if (EntitiesDataProvider.INSTANCE.hasNbtQueryOverride())
        {
	        //Servux.debugLog("received NbtQueryBlock request from: {}", this.player.getName().getLiteralString());
	        return EntitiesDataProvider.INSTANCE.hasNbtQueryPermission(this.player);
        }

        return original.call(instance, permission);
    }

    @WrapOperation(method = "handleEntityTagQuery",
                   at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"))
    private boolean servux_onQueryEntityNbt(PermissionSet instance, Permission permission, Operation<Boolean> original)
    {
        if (EntitiesDataProvider.INSTANCE.hasNbtQueryOverride())
        {
	        //Servux.debugLog("received NbtQueryEntity request from: {}", this.player.getName().getLiteralString());
	        return EntitiesDataProvider.INSTANCE.hasNbtQueryPermission(this.player);
        }

        return original.call(instance, permission);
    }
}
