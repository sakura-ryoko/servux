package fi.dy.masa.servux.util;

import java.util.function.Predicate;
import me.lucko.fabric.api.permissions.v0.Permissions;
import org.jetbrains.annotations.NotNull;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class PermissionsUtil
{
	public static @NotNull Predicate<CommandSourceStack> require(@NotNull String node, int level)
	{
		return Permissions.require(node, PermissionLevel.byId(Mth.clamp(level, 0, PermissionLevel.OWNERS.id())));
	}

	public static boolean check(Entity entity, @NotNull String node, int level)
	{
		return Permissions.check(entity, node, PermissionLevel.byId(Mth.clamp(level, 0, PermissionLevel.OWNERS.id())));
	}
}
