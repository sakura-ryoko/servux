package fi.dy.masa.servux.mixin.entity;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleType;
import net.minecraft.world.rule.GameRules;
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

	@SuppressWarnings("unchecked")
	@Redirect(method = "damage",
	          at = @At(value = "INVOKE",
	                   target = "Lnet/minecraft/world/rule/GameRules;getValue(Lnet/minecraft/world/rule/GameRule;)Ljava/lang/Object;"))
	private <T> T servux$fixAllayGathering6(GameRules instance, GameRule<T> rule)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
			this.isAllay && rule.getType() == GameRuleType.BOOL)        // Ensure BOOL type
		{
			return (T) (Object) true;
		}

		this.isAllay = false;
		return instance.getValue(rule);
	}
}
