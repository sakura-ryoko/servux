package fi.dy.masa.servux.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity extends Entity
{
	@Unique private boolean isAllay = false;

	public MixinItemEntity(EntityType<?> type, Level level)
	{
		super(type, level);
	}

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
	@WrapOperation(method = "hurtServer",
	               at = @At(value = "INVOKE",
	                        target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
	private <T> T servux$fixAllayGathering6(GameRules instance, GameRule<T> gameRule, Operation<T> original)
	{
		if (EntitiesDataProvider.INSTANCE.hasFixAllayGathering() &&
				this.isAllay && gameRule.gameRuleType() == GameRuleType.BOOL)        // Ensure BOOL type
		{
			return (T) (Object) true;
		}

		this.isAllay = false;
		return original.call(instance, gameRule);
	}
}
