 package com.modularwarfare.api.ballistics;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.Event;

public class GetLivingAABBEvent extends Event{
     public final EntityLivingBase entity;
     public AxisAlignedBB box;
    public GetLivingAABBEvent(EntityLivingBase entity, AxisAlignedBB box) {
        this.entity = entity;
        this.box = box;
    }
     
     
}
