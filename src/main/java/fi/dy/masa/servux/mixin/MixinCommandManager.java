package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import fi.dy.masa.servux.commands.CommandProvider;

@Mixin(CommandManager.class)
public class MixinCommandManager
{
    @Shadow @Final private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/dedicated/command/WhitelistCommand;register(Lcom/mojang/brigadier/CommandDispatcher;)V",
            shift = At.Shift.AFTER))
    private void servux_injectCommands(CommandManager.RegistrationEnvironment environment,
                                       CommandRegistryAccess registryAccess, CallbackInfo ci)
    {
        CommandProvider.getInstance().registerCommands(this.dispatcher, registryAccess, environment);
    }
}
