package fi.dy.masa.servux.mixin.entity;

import org.jetbrains.annotations.NotNull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

@Mixin(Mob.class)
public abstract class MixinMobEntity
{
	@Unique boolean isAllay = false;

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void servux$fixAllayGathering3(CallbackInfo ci)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering())
		{
			Entity entity = (Entity) (Object) this;

			if (entity.getType() == EntityType.ALLAY)
			{
				this.isAllay = true;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Redirect(method = "aiStep",
	          at = @At(value = "INVOKE",
	                   target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
	private <T> T servux$fixAllayGathering4(GameRules instance, GameRule<@NotNull T> rule)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
			this.isAllay)
		{
			return (T) (Object) true;
		}

		this.isAllay = false;
		return instance.get(rule);
	}
}
