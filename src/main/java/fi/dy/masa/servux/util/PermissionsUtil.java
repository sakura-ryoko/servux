package fi.dy.masa.servux.util;

import org.jetbrains.annotations.NotNull;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class PermissionsUtil
{
	public static boolean require(CommandSourceStack ctx, @NotNull String node, int level)
	{
		PermissionLevel pl = PermissionLevel.byId(Mth.clamp(level, 0, PermissionLevel.OWNERS.id()));
		Identifier id = Identifier.parse(node);

		if (ctx.isPlayer() && ctx.getPlayer() != null)
		{
			return ctx.getPlayer().checkPermission(id, pl);
		}

		return ctx.permissions().hasPermission(new Permission.HasCommandLevel(pl));
	}

	public static boolean check(Entity entity, @NotNull String node, int level)
	{
		PermissionLevel pl = PermissionLevel.byId(Mth.clamp(level, 0, PermissionLevel.OWNERS.id()));
		Identifier id = Identifier.parse(node);
		return entity.checkPermission(id, pl);
	}
}
