package com.modularwarfare.raycast;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.ballistics.GetLivingAABBEvent;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.hitbox.hits.BulletHit;
import com.modularwarfare.common.hitbox.hits.OBBHit;
import com.modularwarfare.common.hitbox.hits.PlayerHit;
import com.modularwarfare.common.hitbox.playerdata.PlayerData;
import com.modularwarfare.common.vector.Matrix4f;
import com.modularwarfare.common.vector.Vector3f;
import com.modularwarfare.raycast.obb.OBBModelBox;
import com.modularwarfare.raycast.obb.OBBModelBox.RayCastResult;
import com.modularwarfare.raycast.obb.OBBPlayerManager;
import com.modularwarfare.raycast.obb.OBBPlayerManager.OBBDebugObject;
import com.modularwarfare.raycast.obb.OBBPlayerManager.PlayerOBBModelObject;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * This is the default ModularWarfare RayCaster
 * It can be overwritten by your own RayCasting
 */
public class DefaultRayCasting extends RayCasting {

    //在未来应当考虑穿透
    @Override
    public List<BulletHit> computeDetection(World world, Vec3d origin, Vec3d forward, double maxDistance, float borderSize, float penetrateSize, float maxPenetrateBlockResistance, float penetrateBlocksResistance, HashSet<Entity> excluded, boolean collideablesOnly, int ping) {
        // Vec3d lookVec = new Vec3d(tx-x, ty-y, tz-z);
        Vec3d endVec = origin.add(forward.scale(maxDistance));
        AxisAlignedBB bb = new AxisAlignedBB(new BlockPos(origin), new BlockPos(endVec)).grow(borderSize);

        /*
         * 2023.12.31删除了这个莫名其妙的offset 因为他会导致平视射击时判定错误
         * */
//        List<Entity> allEntities = world.getEntitiesWithinAABBExcludingEntity(null, bb.offset(0,-1.62f,0));
        final float originPenetrateSize = penetrateSize;
        final float originBlockPenetrate = penetrateBlocksResistance;
        List<BulletHit> allHits = new ArrayList<>();
        List<RayTraceResult> blockHits = rayTraceBlocks(world, origin, endVec, maxPenetrateBlockResistance, penetrateBlocksResistance, true, true, false);
        if (blockHits != null && !blockHits.isEmpty()) {
            int lastHitIndex = blockHits.size() - 1;
            RayTraceResult lastHitResult = blockHits.get(lastHitIndex);
            maxDistance = lastHitResult.hitVec.distanceTo(origin);
        	endVec = lastHitResult.hitVec;
            for (RayTraceResult blockHit : blockHits) {
                allHits.add(new BulletHit(blockHit, blockHit.hitVec.distanceTo(origin), 0.f, 0.f));
            }
        }

        Vector3f rayVec=new Vector3f(endVec.subtract(origin));
        Vector3f normaliseVec=rayVec.normalise(null);
        OBBModelBox ray=new OBBModelBox();
        float pitch=(float) Math.asin(normaliseVec.y);
        normaliseVec.y=0;
        normaliseVec=normaliseVec.normalise(null);
        float yaw=(float)Math.asin(normaliseVec.x);
        if(normaliseVec.z<0) {
            yaw=(float) (Math.PI-yaw);
        }
        Matrix4f matrix=new Matrix4f();
        matrix.rotate(yaw, new Vector3f(0, 1, 0));
        matrix.rotate(pitch, new Vector3f(-1, 0, 0));
        ray.center=new Vector3f(origin.add(endVec).scale(0.5));
        ray.axis.x=new Vector3f(0, 0, 0);
        ray.axis.y=new Vector3f(0, 0, 0);
        ray.axis.z=Matrix4f.transform(matrix, new Vector3f(0, 0, maxDistance/2), null);
        ray.axisNormal.x=Matrix4f.transform(matrix, new Vector3f(1, 0, 0), null);
        ray.axisNormal.y=Matrix4f.transform(matrix, new Vector3f(0, 1, 0), null);
        ray.axisNormal.z=Matrix4f.transform(matrix, new Vector3f(0, 0, 1), null);

        if(OBBPlayerManager.debug) {
            System.out.println("test0:"+ origin +"|"+Minecraft.getMinecraft().player.getPositionVector());
            OBBPlayerManager.lines.add(new OBBDebugObject(ray));
            OBBPlayerManager.lines.add(new OBBDebugObject(new Vector3f(origin), new Vector3f(endVec)));
        }
        //Iterate over all entities
        for (int i = 0; i < world.loadedEntityList.size(); i++) {
            Entity obj = world.loadedEntityList.get(i);

            if (((excluded != null && !excluded.contains(obj)) || excluded == null)) {
                if (obj instanceof EntityPlayer) {
                    //to delete in the future
                    
                    /*
                    PlayerData data = ModularWarfare.PLAYERHANDLER.getPlayerData((EntityPlayer) obj);

                    int snapshotToTry = ping / 50;
                    if (snapshotToTry >= data.snapshots.length)
                        snapshotToTry = data.snapshots.length - 1;

                    PlayerSnapshot snapshot = data.snapshots[snapshotToTry];

                    if (snapshot == null)
                        snapshot = data.snapshots[0];

                    if (snapshot != null && snapshot.hitboxes != null){
                        for (PlayerHitbox hitbox : snapshot.hitboxes) {
                            RayTraceResult intercept = hitbox.getAxisAlignedBB(snapshot.pos).calculateIntercept(startVec, realVecEnd);
                            if (intercept != null) {
                                intercept.entityHit = hitbox.player;

                                if (ModConfig.INSTANCE.debug_hits_message) {
                                    long currentTime = System.nanoTime();
                                    ModularWarfare.LOGGER.info("Shooter's ping: " + ping / 20 + "ms | " + ping + "ticks");
                                    ModularWarfare.LOGGER.info("Took the snapshot " + snapshotToTry + " Part: " + hitbox.type.toString());
                                    ModularWarfare.LOGGER.info("Delta (currentTime - snapshotTime) = " + (currentTime - snapshot.time) * 1e-6 + "ms");
                                }

                                return new PlayerHit(hitbox, intercept);
                            }
                        }
                    }
                    */
                    //Minecraft.getMinecraft().player.sendMessage(new TextComponentString("test:"+startVec+" "+endVec));
                    PlayerOBBModelObject obbModelObject = OBBPlayerManager.getPlayerOBBObject(obj.getName());
                    OBBModelBox finalBox=null;
                    List<OBBModelBox> boxes = obbModelObject.calculateIntercept(ray);
                    if (!boxes.isEmpty()) {
                        double t = Double.MAX_VALUE;
                        Vector3f hitFaceNormal=null;
                        RayCastResult temp;
                        Vector3f startVector=new Vector3f(origin);
                        for (OBBModelBox obb : boxes) {
                            temp=OBBModelBox.testCollisionOBBAndRay(obb, startVector, rayVec);
                            if(temp.t<t) {
                                t=temp.t;
                                hitFaceNormal=temp.normal;
                                finalBox=obb;
                            }
                        }

                        if(OBBPlayerManager.debug) {
                            OBBPlayerManager.lines.add(new OBBDebugObject(new Vector3f(origin.x+rayVec.x*t, origin.y+rayVec.y*t, origin.z+rayVec.z*t)));
                        }
                        if (finalBox != null) {
                            PlayerData data = ModularWarfare.PLAYER_HANDLER.getPlayerData((EntityPlayer) obj);
                            RayTraceResult intercept = new RayTraceResult(obj, new Vec3d(finalBox.center.x, finalBox.center.y, finalBox.center.z));

                            allHits.add(new OBBHit((EntityPlayer)obj,finalBox.copy(), intercept, intercept.hitVec.distanceTo(origin), 0, 0));
                        }
                    }
                }
            }
        }

        Vec3d hit = null;
        AxisAlignedBB entityBb;// = ent.getBoundingBox();
        RayTraceResult intercept;
//        System.out.println("test1:"+allEntities.size());
        List<Entity> allEntities = world.getEntitiesWithinAABBExcludingEntity(null, bb.grow(1));
        for (Entity ent : allEntities) {
            if ((ent.canBeCollidedWith() || !collideablesOnly) && ((excluded != null && !excluded.contains(ent)) || excluded == null)) {
                if (ent instanceof EntityLivingBase && !(ent instanceof EntityPlayer)) {
                    EntityLivingBase entityLivingBase = (EntityLivingBase) ent;
                    if (!ent.isDead && entityLivingBase.getHealth() > 0.0F) {
                        double entBorder = ent.getCollisionBorderSize();
                        if (entBorder == 0) {
                            entBorder = ModConfig.INSTANCE.general.collisionBorderSizeFixNonPlayer;
                        }
                        entityBb = ent.getEntityBoundingBox();
                        if (entityBb != null) {
                            entityBb = entityBb.grow(entBorder);
                            GetLivingAABBEvent aabbEvent=new GetLivingAABBEvent(entityLivingBase, entityBb) ;
                            MinecraftForge.EVENT_BUS.post(aabbEvent);
                            entityBb=aabbEvent.box;
                            intercept = entityBb.calculateIntercept(origin, endVec);
//                            System.out.println("test:"+intercept);
                            if (intercept != null) {
                                double currentHitDistance = intercept.hitVec.distanceTo(origin);
                                hit = intercept.hitVec;
                                if (currentHitDistance < maxDistance) {
                                    allHits.add(new BulletHit(new RayTraceResult(ent, hit), currentHitDistance, 0, 0));
                                }
                            }
                        }
                    }
                } else if (ent instanceof EntityGrenade) {
                    float entBorder = ent.getCollisionBorderSize();
                    entityBb = ent.getEntityBoundingBox();
                    if (entityBb != null) {
                        entityBb = entityBb.grow(entBorder, entBorder, entBorder);
                        intercept = entityBb.calculateIntercept(origin, endVec);
                        if (intercept != null) {
                            double currentHitDistance = (float) intercept.hitVec.distanceTo(origin);
                            hit = intercept.hitVec;
                            if (currentHitDistance < maxDistance) {
                                allHits.add(new BulletHit(new RayTraceResult(ent, hit), currentHitDistance, 0, 0));
                            }
                        }
                    }
                }
            }
        }
        if (allHits.isEmpty()) {
            return allHits;
        }
        allHits.sort(Comparator.comparingDouble(bulletHit -> bulletHit.distance));
        List<BulletHit> result = new ArrayList<>();
        for (BulletHit currentHit : allHits) {
            if ((originPenetrateSize > 0 && penetrateSize <= 0) || (originBlockPenetrate > 0 && penetrateBlocksResistance <= 0)) {
                break;
            }
            currentHit.remainingPenetrate = originPenetrateSize == 0 ? 1.f : penetrateSize / originPenetrateSize;
            currentHit.remainingBlockPenetrate = originBlockPenetrate == 0 ? 1.f : penetrateBlocksResistance / originBlockPenetrate;
            do {
                if (originPenetrateSize > 0) {
                    if (currentHit instanceof PlayerHit) {
                        penetrateSize -= 0.5f;
                        break;
                    }
                    if (currentHit instanceof OBBHit) {
                        OBBHit obbHit = (OBBHit) currentHit;
                        if (obbHit.entity instanceof EntityPlayer) {
                            penetrateSize -= 0.5f;
                            break;
                        }
                        double avgSize = (obbHit.box.size.x + obbHit.box.size.y + obbHit.box.size.z) / 3;
                        penetrateSize -= (float) avgSize;
                        break;
                    }
                    if (currentHit.rayTraceResult.typeOfHit == RayTraceResult.Type.ENTITY && currentHit.rayTraceResult.entityHit != null) {
                        AxisAlignedBB entityBoundingBox = currentHit.rayTraceResult.entityHit.getEntityBoundingBox();
                        double avgSize = entityBoundingBox.getAverageEdgeLength();
                        penetrateSize -= (float) avgSize;
                        break;
                    }
                }
                if (originBlockPenetrate > 0) {
                    if (currentHit.rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
                        BlockPos blockPos = new BlockPos(currentHit.rayTraceResult.hitVec);
                        IBlockState iBlockState = world.getBlockState(blockPos);
                        Block block = iBlockState.getBlock();
                        float blockExplosionResistance = block.getExplosionResistance(world, blockPos, null, new Explosion(world, null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1.f, false, false));
                        penetrateBlocksResistance -= blockExplosionResistance;
                    }
                }
            } while (false);
            result.add(currentHit);
        }
        return result;
    }

    @Nullable
    public List<RayTraceResult> rayTraceBlocks(World world, Vec3d vec31, Vec3d vec32, float maxPenetrateBlockResistance, float penetrateBlocksResistance, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        if (!Double.isNaN(vec31.x) && !Double.isNaN(vec31.y) && !Double.isNaN(vec31.z)) {
            if (!Double.isNaN(vec32.x) && !Double.isNaN(vec32.y) && !Double.isNaN(vec32.z)) {
                List<RayTraceResult> result = new ArrayList<>();
                int i = MathHelper.floor(vec32.x);
                int j = MathHelper.floor(vec32.y);
                int k = MathHelper.floor(vec32.z);
                int l = MathHelper.floor(vec31.x);
                int i1 = MathHelper.floor(vec31.y);
                int j1 = MathHelper.floor(vec31.z);
                BlockPos blockpos = new BlockPos(l, i1, j1);
                IBlockState iblockstate = world.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if ((!ignoreBlockWithoutBoundingBox || iblockstate.getCollisionBoundingBox(world, blockpos) != Block.NULL_AABB) && block.canCollideCheck(iblockstate, stopOnLiquid)) {
                    RayTraceResult raytraceresult = iblockstate.collisionRayTrace(world, blockpos, vec31, vec32);

                    if (raytraceresult != null) {
                        result.add(raytraceresult);
                        return result;
                    }
                }

                RayTraceResult raytraceresult2 = null;
                int k1 = 200;

                while (k1-- >= 0) {
                    if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z)) {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k) {
                        if (returnLastUncollidableBlock && raytraceresult2 != null) {
                            result.add(raytraceresult2);
                        }
                        return result;
                    }

                    boolean flag2 = true;
                    boolean flag = true;
                    boolean flag1 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l) {
                        d0 = (double) l + 1.0D;
                    } else if (i < l) {
                        d0 = (double) l + 0.0D;
                    } else {
                        flag2 = false;
                    }

                    if (j > i1) {
                        d1 = (double) i1 + 1.0D;
                    } else if (j < i1) {
                        d1 = (double) i1 + 0.0D;
                    } else {
                        flag = false;
                    }

                    if (k > j1) {
                        d2 = (double) j1 + 1.0D;
                    } else if (k < j1) {
                        d2 = (double) j1 + 0.0D;
                    } else {
                        flag1 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = vec32.x - vec31.x;
                    double d7 = vec32.y - vec31.y;
                    double d8 = vec32.z - vec31.z;

                    if (flag2) {
                        d3 = (d0 - vec31.x) / d6;
                    }

                    if (flag) {
                        d4 = (d1 - vec31.y) / d7;
                    }

                    if (flag1) {
                        d5 = (d2 - vec31.z) / d8;
                    }

                    if (d3 == -0.0D) {
                        d3 = -1.0E-4D;
                    }

                    if (d4 == -0.0D) {
                        d4 = -1.0E-4D;
                    }

                    if (d5 == -0.0D) {
                        d5 = -1.0E-4D;
                    }

                    EnumFacing enumfacing;

                    if (d3 < d4 && d3 < d5) {
                        enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
                        vec31 = new Vec3d(d0, vec31.y + d7 * d3, vec31.z + d8 * d3);
                    } else if (d4 < d5) {
                        enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
                        vec31 = new Vec3d(vec31.x + d6 * d4, d1, vec31.z + d8 * d4);
                    } else {
                        enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        vec31 = new Vec3d(vec31.x + d6 * d5, vec31.y + d7 * d5, d2);
                    }

                    l = MathHelper.floor(vec31.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
                    i1 = MathHelper.floor(vec31.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
                    j1 = MathHelper.floor(vec31.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
                    blockpos = new BlockPos(l, i1, j1);
                    IBlockState iblockstate1 = world.getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();
                    float blockExplosionResistance = block1.getExplosionResistance(world, blockpos, null, new Explosion(world, null, blockpos.getX(), blockpos.getY(), blockpos.getZ(), 1.f, false, false));

//                    if (ModConfig.INSTANCE.shots.shot_break_glass) {
//                        if (block1 instanceof BlockGlass || block1 instanceof BlockStainedGlassPane || block1 instanceof BlockStainedGlass) {
//                            world.setBlockToAir(blockpos);
//                            ModularWarfare.NETWORK.sendToAllAround(new PacketPlaySound(blockpos, "impact.glass", 1f, 1f), new NetworkRegistry.TargetPoint(0, blockpos.getX(), blockpos.getY(), blockpos.getZ(), 25));
//                            continue;
//                        }
//                    }
//
//                    if (block1 instanceof BlockPane) {
//                        ModularWarfare.NETWORK.sendToAllAround(new PacketPlaySound(blockpos, "impact.iron", 1f, 1f), new NetworkRegistry.TargetPoint(0, blockpos.getX(), blockpos.getY(), blockpos.getZ(), 25));
//                        continue;
//                    }
//
//                    if (block1 instanceof BlockDoor || block1 instanceof BlockLeaves) {
//                        continue;
//                    }

                    if (!ignoreBlockWithoutBoundingBox || iblockstate1.getMaterial() == Material.BARRIER || iblockstate1.getMaterial() == Material.PORTAL || iblockstate1.getCollisionBoundingBox(world, blockpos) != Block.NULL_AABB) {
                        if (block1.canCollideCheck(iblockstate1, stopOnLiquid)) {
                            RayTraceResult raytraceresult1 = iblockstate1.collisionRayTrace(world, blockpos, vec31, vec32);
                            if (raytraceresult1 != null) {
                                result.add(raytraceresult1);
                                if (blockExplosionResistance < maxPenetrateBlockResistance && penetrateBlocksResistance > blockExplosionResistance) {
                                    penetrateBlocksResistance -= blockExplosionResistance;
                                    continue;
                                }
                                return result;
                            }
                        } else {
                            raytraceresult2 = new RayTraceResult(RayTraceResult.Type.MISS, vec31, enumfacing, blockpos);
                        }
                    }
                }

                if (!returnLastUncollidableBlock || raytraceresult2 == null) {
                    return null;
                }
                result.add(raytraceresult2);
                return result;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
