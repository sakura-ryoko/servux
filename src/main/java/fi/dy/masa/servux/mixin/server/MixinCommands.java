package fi.dy.masa.servux.mixin.server;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.brigadier.CommandDispatcher;
import fi.dy.masa.servux.commands.CommandProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

@Mixin(Commands.class)
public class MixinCommands
{
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/commands/WhitelistCommand;register(Lcom/mojang/brigadier/CommandDispatcher;)V",
            shift = At.Shift.AFTER))
    private void servux_injectCommands(Commands.CommandSelection environment,
                                       CommandBuildContext registryAccess, CallbackInfo ci)
    {
        ((CommandProvider) CommandProvider.getInstance()).registerCommands(this.dispatcher, registryAccess, environment);
    }
}
