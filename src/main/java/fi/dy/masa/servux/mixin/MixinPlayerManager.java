package fi.dy.masa.servux.mixin;

import fi.dy.masa.servux.event.PlayerHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager
{
    public MixinPlayerManager() { super(); }

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void eventOnPlayerJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerJoin(player);
    }
    @Inject(method = "remove", at = @At("HEAD"))
    private void eventOnPlayerLeave(ServerPlayerEntity player, CallbackInfo ci)
    {
        ((PlayerHandler) PlayerHandler.getInstance()).onPlayerLeave(player);
    }
}
