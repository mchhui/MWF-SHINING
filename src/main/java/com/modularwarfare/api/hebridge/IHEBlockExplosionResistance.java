package com.modularwarfare.api.hebridge;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@FunctionalInterface
public interface IHEBlockExplosionResistance {

    float getExplosionResistanceForBlockRayHit(World world, Vec3d rayStart, Vec3d rayEnd, RayTraceResult blockHit,
        BlockPos blockPos, Block block);
}
