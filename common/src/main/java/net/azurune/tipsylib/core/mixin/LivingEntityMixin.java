package net.azurune.tipsylib.core.mixin;

import net.azurune.tipsylib.common.util.IStatusEffectInstance;
import net.azurune.tipsylib.core.register.TLDamageTypes;
import net.azurune.tipsylib.core.register.TLStatusEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow @Final private Map<MobEffect, MobEffectInstance> activeEffects;
    @Shadow @Nullable public abstract MobEffectInstance getEffect(MobEffect effect);
    @Unique @Final LivingEntity living = (LivingEntity) (Object) this;
    @Unique public Level level;

    @Inject(at = @At("HEAD"), method = "tickEffects")
    public void tipsylib_tickEffects(CallbackInfo ci) {
        for (MobEffectInstance statusEffect : this.activeEffects.values()) {
            if (statusEffect instanceof IStatusEffectInstance effect) {
                effect.setEntity((LivingEntity) (Object) this);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "canStandOnFluid", cancellable = true)
    public void tipsylib_canStandOnFluid(FluidState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getType() == Fluids.WATER || state.getType() == Fluids.FLOWING_WATER)
            if (living != null && (this.living.hasEffect(TLStatusEffects.WATER_WALKING))) cir.setReturnValue(true);

        if (state.getType() == Fluids.LAVA || state.getType() == Fluids.FLOWING_LAVA)
            if (living != null && (this.living.hasEffect(TLStatusEffects.LAVA_WALKING))) cir.setReturnValue(true);
    }

    @Inject(at = @At("HEAD"), method = "hurt", cancellable = true)
    public void tipsylib_hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            if (living.hasEffect(TLStatusEffects.TRAIL_BLAZING) || living.hasEffect(TLStatusEffects.PYROMANIAC) || living.hasEffect(TLStatusEffects.LAVA_WALKING) && source.is(DamageTypeTags.IS_FIRE))
                cir.setReturnValue(false);
        }

        if (living.hasEffect(TLStatusEffects.BURNING_THORNS)) {
            Entity entity = source.getEntity();
            if (entity != null) entity.setSecondsOnFire(5 * (getEffect(TLStatusEffects.BURNING_THORNS).getAmplifier() + 1));
        }

        if (living.hasEffect(TLStatusEffects.RETALIATION)) {
            Entity entity = source.getEntity();
            if (entity != null) {
                DamageSource damagesource = new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TLDamageTypes.RETALIATION));
                entity.hurt(damagesource, 1.0F + (getEffect(TLStatusEffects.RETALIATION).getAmplifier() + 1));
            }
        }

        if (living.hasEffect(TLStatusEffects.TOUGH_SKIN) && source.is(DamageTypeTags.IS_EXPLOSION))
            cir.setReturnValue(false);

        if (living.hasEffect(TLStatusEffects.DIVERSION) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            if (living.hasEffect(MobEffects.LUCK)) {
                if (Math.random() < 0.2) {
                    level.playSound(null, living.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS);
                    cir.cancel();
                }
            } else if (Math.random() < 0.15) {
                cir.cancel();
            }
    }

    @ModifyVariable(at = @At("HEAD"), method = "hurt", argsOnly = true)
    public float shatterSpleen(float amount) {
        if (living.hasEffect(TLStatusEffects.SHATTERSPLEEN)) {
            return amount + amount * (0.5F * living.getEffect(TLStatusEffects.SHATTERSPLEEN).getAmplifier() + 0.5F);
        }
        return amount;
    }

    @ModifyVariable(at = @At("HEAD"), method = "hurt", argsOnly = true)
    public float frailty(float amount, DamageSource source) {
        if (living.hasEffect(TLStatusEffects.FRAILTY) && source.is(DamageTypeTags.IS_FALL)) {
            return amount + amount * (0.5F * living.getEffect(TLStatusEffects.FRAILTY).getAmplifier() + 0.5F);
        }
        return amount;
    }

    @Inject(at = @At("TAIL"), method = "getDamageAfterMagicAbsorb")
    public void modifyAppliedDamage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Entity entity = source.getEntity();
        if (entity instanceof LivingEntity attacker) {
            if (living.hasEffect(TLStatusEffects.BACKLASH)) {
                DamageSource damagesource = new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TLDamageTypes.BACKLASH));
                attacker.hurt(damagesource, (amount * 0.25F) + living.getEffect(TLStatusEffects.BACKLASH).getAmplifier() + 1.0F);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "heal", cancellable = true)
    public void heal(float amount, CallbackInfo ci) {
        if (living.hasEffect(TLStatusEffects.BLEEDING)) ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo ci) {
        BlockPos pos = living.blockPosition();
        if (living.hasEffect(TLStatusEffects.TRAIL_BLAZING) && living.level().getBlockState(pos).isAir()) {
            living.level().setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
        }

        if (living.getBlockStateOn().is(BlockTags.FIRE) && living.hasEffect(TLStatusEffects.PYROMANIAC)) {
            if (living.tickCount % 20 == 0) {
                if (living.getHealth() < living.getMaxHealth()) {
                    living.heal((getEffect(TLStatusEffects.PYROMANIAC).getAmplifier() + 1.0F));
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "checkFallDamage", cancellable = true)
    public void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition, CallbackInfo ci) {
        if (living.hasEffect(TLStatusEffects.STEEL_FEET)) ci.cancel();
    }
}
