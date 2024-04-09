package com.modularwarfare.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingEvent;

public class EntityHeadShotEvent extends LivingEvent{
    public final EntityLivingBase shooter;
    public EntityHeadShotEvent(EntityLivingBase entity,EntityLivingBase shooter) {
        super(entity);
        this.shooter=shooter;
    }
    
    public EntityLivingBase getVictim() {
        return this.getEntityLiving();
    }
    
    public EntityLivingBase getShooter() {
        return this.shooter;
    }
}
