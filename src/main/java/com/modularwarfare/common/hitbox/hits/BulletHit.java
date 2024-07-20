package com.modularwarfare.common.hitbox.hits;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;

public class BulletHit {

    public RayTraceResult rayTraceResult;
    public double distance;
    public float remainingPenetrate;// 0.0-1.0; If it is a block hit, it is always 0
    public float remainingBlockPenetrate;// 0.0-1.0; If it is a block hit, it is always 0

    public BulletHit(RayTraceResult result, double distance, float remainingPenetrate, float remainingBlockPenetrate) {
        this.rayTraceResult = result;
        this.distance = distance;
        this.remainingPenetrate = remainingPenetrate;
        this.remainingBlockPenetrate = remainingBlockPenetrate;
    }

    public Entity getEntity() {
        return rayTraceResult.entityHit;
    }
}
