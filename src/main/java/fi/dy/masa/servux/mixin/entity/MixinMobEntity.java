package fi.dy.masa.servux.mixin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(MobEntity.class)
public abstract class MixinMobEntity
{
	@Unique boolean isAllay = false;

	@Inject(method = "tickMovement", at = @At("HEAD"))
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

	@Redirect(method = "tickMovement",
	          at = @At(value = "INVOKE",
	                   target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
	private boolean servux$fixAllayGathering4(GameRules instance, GameRules.Key<GameRules.BooleanRule> rule)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
			this.isAllay)
		{
			return true;
		}

		this.isAllay = false;
		return instance.getBoolean(rule);
	}
}
