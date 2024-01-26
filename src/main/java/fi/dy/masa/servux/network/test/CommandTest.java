package fi.dy.masa.servux.network.test;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fi.dy.masa.servux.Servux;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandTest
{
    public static void registerCommandTest()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("network-test")
                            .then(argument("player", EntityArgumentType.player())
                                    .executes(ctx -> testPlayer(ctx.getSource(), EntityArgumentType.getPlayer(ctx,"player"), "", ctx))
                                    .then(argument("message", StringArgumentType.greedyString())
                                            .executes(ctx -> testPlayer(ctx.getSource(), EntityArgumentType.getPlayer(ctx,"player"), StringArgumentType.getString(ctx, "message"), ctx))
                                    )
                            )
        ));
    }

    private static int testPlayer(ServerCommandSource src, ServerPlayerEntity target, String message, CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException
    {
        String user = src.getPlayerOrThrow().getName().getLiteralString();
        if (target != null)
        {
            // Run S2C test -> Player
            ServerDebugSuite.testS2C(target, message);
        }
        else {
            // Run C2S test
            Servux.printDebug("testPlayer(): No player entity to send a packet to.");
        }
        Servux.printDebug("testPlayer(): --> Executed!");
        return 1;
    }
}
