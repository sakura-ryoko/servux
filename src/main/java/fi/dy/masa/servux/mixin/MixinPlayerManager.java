package fi.dy.masa.servux.mixin;

import java.net.SocketAddress;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import fi.dy.masa.servux.event.PlayerHandler;

/**
 * Interface for processing various server side Player Manager events
 */
@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager
{
    @Unique
    private GameProfile profileTemp;

    public MixinPlayerManager() { super(); }

    @Inject(method = "checkCanJoin", at = @At("RETURN"))
    private void eventOnClientConnect(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onClientConnect(address, profile, cir.getReturnValue());
    }

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void eventOnPlayerJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerJoin(connection.getAddress(), clientData.gameProfile(), player);
    }

    @Inject(method = "respawnPlayer", at = @At("RETURN"))
    private void eventOnPlayerRespawn(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerRespawn(cir.getReturnValue(), player);
    }

    @Inject(method = "addToOperators", at = @At("HEAD"))
    private void captureGameProfileOp(GameProfile profile, CallbackInfo ci)
    {
        this.profileTemp = profile;
    }

    @Redirect(method = "addToOperators",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/server/PlayerManager;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/network/ServerPlayerEntity;"))
    private ServerPlayerEntity eventOnPlayerOp(PlayerManager instance, UUID uuid)
    {
        ServerPlayerEntity player = instance.getPlayer(uuid);

        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerOp(this.profileTemp, uuid, player);

        if (this.profileTemp != null)
        {
            this.profileTemp = null;
        }

        return player;
    }

    @Inject(method = "removeFromOperators", at = @At("HEAD"))
    private void captureGameProfileDeOp(GameProfile profile, CallbackInfo ci)
    {
        this.profileTemp = profile;
    }

    @Redirect(method = "removeFromOperators",
            at = @At(value = "INVOKE",
                    target="Lnet/minecraft/server/PlayerManager;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/network/ServerPlayerEntity;"))
    private ServerPlayerEntity eventOnPlayerDeOp(PlayerManager instance, UUID uuid)
    {
        ServerPlayerEntity player = instance.getPlayer(uuid);

        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerDeOp(this.profileTemp, uuid, player);

        if (this.profileTemp != null)
        {
            this.profileTemp = null;
        }

        return player;
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void eventOnPlayerLeave(ServerPlayerEntity player, CallbackInfo ci)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerLeave(player);
    }
}
