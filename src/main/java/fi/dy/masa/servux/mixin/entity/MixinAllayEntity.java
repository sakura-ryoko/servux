package fi.dy.masa.servux.mixin.entity;

import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(AllayEntity.class)
public abstract class MixinAllayEntity
{
//	@Shadow public abstract Brain<AllayEntity> getBrain();

	@Redirect(method = "canGather",
	          at = @At(value = "INVOKE",
	                   target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
	private boolean servux$fixAllayGathering1(GameRules instance, GameRules.Key<GameRules.BooleanRule> rule)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering())
		{
			return true;
		}

		return instance.getBoolean(rule);
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
