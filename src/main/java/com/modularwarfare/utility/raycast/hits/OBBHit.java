package com.modularwarfare.utility.raycast.hits;

import com.modularwarfare.utility.raycast.obb.OBBModelBox;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;

/**
 * OBB ray hit for any entity that registered boxes into {@code EntityOBBManager}.
 */
public class OBBHit extends BulletHit {
    public Entity entity;
    public OBBModelBox box;

    public OBBHit(Entity entity, OBBModelBox box, RayTraceResult result, double distance, float remainingPenetrate, float remainingBlockPenetrate) {
        super(result, distance, remainingPenetrate, remainingBlockPenetrate);
        this.box = box;
        this.entity = entity;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }
}
