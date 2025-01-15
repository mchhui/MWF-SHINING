package com.modularwarfare.utility;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;

public class CommonUtil {
    /**
     * Attacks the given entity with the given damage source and amount, but
     * preserving the entity's original velocity instead of applying knockback
     */
    public static boolean attackEntityWithoutKnockback(Entity entity, DamageSource source, float amount) {
        double vx = entity.motionX;
        double vy = entity.motionY;
        double vz = entity.motionZ;
        boolean succeeded = entity.attackEntityFrom(source, amount);
        entity.motionX = vx;
        entity.motionY = vy;
        entity.motionZ = vz;
        return succeeded;
    }
} 