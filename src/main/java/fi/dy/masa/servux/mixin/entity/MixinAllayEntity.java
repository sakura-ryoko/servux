package fi.dy.masa.servux.mixin.entity;

import org.jetbrains.annotations.NotNull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;

@Mixin(Allay.class)
public abstract class MixinAllayEntity
{
	@SuppressWarnings("unchecked")
	@Redirect(method = "wantsToPickUp",
	          at = @At(value = "INVOKE",
	                   target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
	private <T> T servux$fixAllayGathering1(GameRules instance, GameRule<@NotNull T> rule)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
			rule.gameRuleType() == GameRuleType.BOOL)        // Ensure BOOL type
		{
			return (T) (Object) true;
		}

		return instance.get(rule);
	}

//	@Inject(method = "isItemPickupCoolingDown",
//	        at = @At("RETURN"), cancellable = true)
//	private void servux$fixAllayGathering2(CallbackInfoReturnable<Boolean> cir)
//	{
//		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
//			cir.getReturnValue())
//		{
//			cir.setReturnValue(false);
//		}
//	}
}
