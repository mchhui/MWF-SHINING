package com.modularwarfare.raycast;

import com.modularwarfare.common.hitbox.hits.BulletHit;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;

public abstract class RayCasting {

    public abstract List<BulletHit> computeDetection(World world, Vec3d origin, Vec3d forward, double maxDistance, float borderSize, float penetrateSiz, float maxPenetrateBlockResistance, float penetrateBlocksResistance, HashSet<Entity> excluded, boolean collideablesOnly, int ping);

    public abstract List<RayTraceResult> rayTraceBlocks(World world, Vec3d vec31, Vec3d vec32, float maxPenetrateBlockResistance, float penetrateBlocksResistance, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock);
}