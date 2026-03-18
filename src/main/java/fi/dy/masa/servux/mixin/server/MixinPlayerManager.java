package fi.dy.masa.servux.mixin.server;

import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.event.PlayerHandler;

/**
 * Interface for processing various server side Player Manager events
 */
@Mixin(PlayerList.class)
public abstract class MixinPlayerManager
{
    @Unique private NameAndId profileTemp;

    public MixinPlayerManager() { super(); }

    @Inject(method = "canPlayerLogin", at = @At("RETURN"))
    private void servux_onClientConnect(SocketAddress address, NameAndId playerConfigEntry, CallbackInfoReturnable<Component> cir)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onClientConnect(address, playerConfigEntry, cir.getReturnValue());
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void servux_onPlayerJoin(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerJoin(connection.getRemoteAddress(), clientData.gameProfile(), player);
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void servux_onPlayerRespawn(ServerPlayer player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerRespawn(cir.getReturnValue(), player);
    }

    @Inject(method = "op(Lnet/minecraft/server/players/NameAndId;Ljava/util/Optional;Ljava/util/Optional;)V", at = @At("HEAD"))
    private void servux_onCaptureGameProfileOp(NameAndId player, Optional<LevelBasedPermissionSet> permissionLevel, Optional<Boolean> canBypassPlayerLimit, CallbackInfo ci)
    {
        this.profileTemp = player;
    }

    @WrapOperation(method = "op(Lnet/minecraft/server/players/NameAndId;Ljava/util/Optional;Ljava/util/Optional;)V",
                   at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/server/players/PlayerList;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer servux_onPlayerOp(PlayerList instance, UUID uUID, Operation<ServerPlayer> original)
    {
        ServerPlayer player = instance.getPlayer(uUID);

        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerOp(this.profileTemp, uUID, player);

        if (this.profileTemp != null)
        {
            this.profileTemp = null;
        }

        return original.call(instance, uUID);
    }

    @Inject(method = "deop", at = @At("HEAD"))
    private void servux_onGameProfileDeOp(NameAndId player, CallbackInfo ci)
    {
        this.profileTemp = player;
    }

    @WrapOperation(method = "deop",
            at = @At(value = "INVOKE",
                    target="Lnet/minecraft/server/players/PlayerList;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer servux_onPlayerDeOp(PlayerList instance, UUID uUID, Operation<ServerPlayer> original)
    {
        ServerPlayer player = instance.getPlayer(uUID);

        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerDeOp(this.profileTemp, uUID, player);

        if (this.profileTemp != null)
        {
            this.profileTemp = null;
        }

        return original.call(instance, uUID);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void servux_onPlayerLeave(ServerPlayer player, CallbackInfo ci)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerLeave(player);
    }
}
