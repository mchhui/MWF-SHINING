package com.modularwarfare.utility;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Set;

public final class DamageControlHelper {

    private DamageControlHelper() {
    }

    public static boolean canDamageTarget(Entity attacker, Entity target, boolean ignoreFriendlyTargets) {
        if (!ignoreFriendlyTargets || attacker == null) {
            return true;
        }
        if (target == attacker) {
            return false;
        }
        if (attacker.isOnSameTeam(target) || target.isOnSameTeam(attacker)) {
            return false;
        }
        if (attacker instanceof EntityPlayer && target instanceof EntityPlayer) {
            EntityPlayer playerAttacker = (EntityPlayer) attacker;
            if (!playerAttacker.canAttackPlayer((EntityPlayer) target)) {
                return false;
            }
        }
        return true;
    }

    public static void clearHurtResistantTime(Entity target, boolean damaged) {
        if (damaged && target instanceof EntityLivingBase) {
            ((EntityLivingBase) target).hurtResistantTime = 0;
        }
    }
    
    public static boolean markImpactOnce(Set<Integer> hitEntityIds, Entity target) {
        return hitEntityIds.add(target.getEntityId());
    }
}
