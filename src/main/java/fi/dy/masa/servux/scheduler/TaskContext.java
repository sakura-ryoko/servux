package fi.dy.masa.servux.scheduler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record TaskContext(MinecraftServer server, ServerLevel world, ServerPlayer player, String name, long startTime)
{
}
