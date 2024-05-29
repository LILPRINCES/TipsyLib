package net.azurune.tipsylib.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class HeartBreakEffect extends MobEffect {
    public HeartBreakEffect(MobEffectCategory $$0, int $$1) {
        super($$0, $$1);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setHealth(entity.getHealth());
    }

    public boolean isDurationEffectTick(int duration, int $$1) {
        return true;
    }
}
