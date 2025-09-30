package fi.dy.masa.servux.mixin.entity;

import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(AllayEntity.class)
public class MixinAllayEntity
{
	@Redirect(method = "canGather",
	          at = @At(value = "INVOKE",
	                   target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
	private boolean servux$fixAllayGathering(GameRules instance, GameRules.Key<GameRules.BooleanRule> rule)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering())
		{
			return true;
		}

		return instance.getBoolean(GameRules.DO_MOB_GRIEFING);
	}
}
