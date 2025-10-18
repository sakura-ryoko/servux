package fi.dy.masa.servux.mixin.entity;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(ItemEntity.class)
public class MixinItemEntity
{
	@Unique private boolean isAllay = false;

	@Inject(method = "damage", at = @At("HEAD"))
	private void servux$fixAllayGathering5(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
			source.getAttacker() instanceof AllayEntity)
		{
			this.isAllay = true;
		}
	}

	@Redirect(method = "damage",
	          at = @At(value = "INVOKE",
	                   target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
	private boolean servux$fixAllayGathering6(GameRules instance, GameRules.Key<GameRules.BooleanRule> rule)
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
