package com.modularwarfare.raycast;

import com.modularwarfare.common.hitbox.hits.BulletHit;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;

public abstract class RayCasting {

    public abstract BulletHit computeDetection(World world, Vec3d origin, Vec3d forward, double maxDistance, float borderSize, HashSet<Entity> excluded, boolean collideablesOnly, int ping);

    public abstract RayTraceResult rayTraceBlocks(World world, Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock);
}