package fi.dy.masa.servux.mixin;

import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.PayloadTypeRegister;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.CustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayNetworkHandler.class, priority = 998)
public class MixinClientPlayNetworkHandler
{
    @Inject(method="warnOnUnknownPayload", at = @At("HEAD"), cancellable = true)
    private void servux_checkUnknownPayload(CustomPayload payload, CallbackInfo ci)
    {
        PayloadType type = PayloadTypeRegister.getInstance().getPayloadType(payload.getId().id());
        if (type != null)
        {
            // Silently cancel it.
            if (ci.isCancellable())
                ci.cancel();
        }
    }
}
