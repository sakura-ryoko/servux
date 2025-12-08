package fi.dy.masa.servux.mixin.entity;

import org.jetbrains.annotations.NotNull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;

@Mixin(ItemEntity.class)
public class MixinItemEntity
{
	@Unique private boolean isAllay = false;

	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void servux$fixAllayGathering5(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
			source.getEntity() instanceof Allay)
		{
			this.isAllay = true;
		}
	}

	@SuppressWarnings("unchecked")
	@Redirect(method = "hurtServer",
	          at = @At(value = "INVOKE",
	                   target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
	private <T> T servux$fixAllayGathering6(GameRules instance, GameRule<@NotNull T> rule)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
			this.isAllay && rule.gameRuleType() == GameRuleType.BOOL)        // Ensure BOOL type
		{
			return (T) (Object) true;
		}

		this.isAllay = false;
		return instance.get(rule);
	}
}
