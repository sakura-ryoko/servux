package fi.dy.masa.servux.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(Allay.class)
public abstract class MixinAllayEntity
{
	@SuppressWarnings("unchecked")
	@WrapOperation(method = "wantsToPickUp",
	               at = @At(value = "INVOKE",
	                   target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
	private <T> T servux$fixAllayGathering1(GameRules instance, GameRule<T> gameRule, Operation<T> original)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
			gameRule.gameRuleType() == GameRuleType.BOOL)        // Ensure BOOL type
		{
			return (T) (Object) true;
		}

		return original.call(instance, gameRule);
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
