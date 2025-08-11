package com.modularwarfare.utility;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.EntityShootingAPI;
import com.modularwarfare.api.ballistics.GetLivingAABBEvent;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.hitbox.hits.BulletHit;
import com.modularwarfare.common.playerstate.PlayerStateManager;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import mchhui.modularmovements.coremod.ModularMovementsHooks;
import mchhui.modularmovements.tactical.client.ClientListener;
import mchhui.modularmovements.tactical.server.ServerListener;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import net.minecraft.util.math.AxisAlignedBB;

public class RayUtil {

    private static class AimingPoint {
        double currentX = 0;
        double currentY = 0;
        double targetX = 0;
        double targetY = 0;
        double startX = 0;
        double startY = 0;
        float lastAccuracy = 0;
        long accuracyChangeTime = 0;
    }
    
    private static AimingPoint mainAimPoint = new AimingPoint();
    private static AimingPoint[] subAimPoints = new AimingPoint[32]; // 支持最多32颗子弹
    private static int currentBulletIndex = 0;
    
    public static void resetBulletIndex() {
        currentBulletIndex = 0;
    }
    
    public static Vec3d getGunAccuracy(float pitch, float yaw, final float accuracy, final Random rand, EntityLivingBase entity) {
        if(Minecraft.getMinecraft() != null && Minecraft.getMinecraft().world != null && Minecraft.getMinecraft().world.isRemote && entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
                    
            ItemStack heldItem = player.getHeldItemMainhand();
            if(!heldItem.isEmpty() && heldItem.getItem() instanceof ItemGun) {
                ItemGun itemGun = (ItemGun)heldItem.getItem();
                if(itemGun.type != null && itemGun.type.useEnhancedAiming) {
                    
                    ItemBullet bulletItem = ItemGun.getUsedBullet(heldItem, itemGun.type);
                    boolean isSlug = bulletItem != null && bulletItem.type != null && bulletItem.type.isSlug;
                    
                    AimingPoint aimPoint;
                    if(itemGun.type.numBullets > 1 && !isSlug) {
                        if(subAimPoints[currentBulletIndex] == null) {
                            subAimPoints[currentBulletIndex] = new AimingPoint();
                        }
                        aimPoint = subAimPoints[currentBulletIndex];
                        currentBulletIndex = (currentBulletIndex + 1) % itemGun.type.numBullets;
                    } else {
                        aimPoint = mainAimPoint;
                    }
                    
                    long seed = (long)(pitch * 10000) + (long)(yaw * 10000) + (long)(accuracy * 10000);
                    
                    if(itemGun.type.numBullets > 1) {
                        long time = System.nanoTime();
                        seed = seed * 31 + currentBulletIndex * 17 + (time & 0xFFFF);
                        seed = seed * 31 + (time >>> 16) & 0xFFFF;
                        seed = seed * 31 + (long)(Math.sin(currentBulletIndex * Math.PI / itemGun.type.numBullets) * 10000);
                    }
                    rand.setSeed(seed);
                    
                    long currentTime = System.currentTimeMillis();
                            
                    if(Math.abs(aimPoint.lastAccuracy - accuracy) > 0.0001f || currentTime - aimPoint.accuracyChangeTime > 200) {
                        aimPoint.accuracyChangeTime = currentTime;
                        aimPoint.lastAccuracy = accuracy;
                                
                        aimPoint.startX = aimPoint.currentX;
                        aimPoint.startY = aimPoint.currentY;
                                
                        double angleRange = itemGun.type.numBullets > 1 ? Math.PI * 2.0 : Math.PI * 1.5;
                        double angle = Math.atan2(aimPoint.currentY, aimPoint.currentX) + (rand.nextDouble() - 0.5) * angleRange;
                        double currentRadius = Math.sqrt(aimPoint.currentX * aimPoint.currentX + aimPoint.currentY * aimPoint.currentY);
                                
                        double radiusChange = itemGun.type.numBullets > 1 ? 1.2 : 0.8;
                        double newRadius = Math.min(accuracy, currentRadius + (rand.nextDouble() - 0.3) * accuracy * radiusChange);
                                
                        aimPoint.targetX = Math.cos(angle) * newRadius;
                        aimPoint.targetY = Math.sin(angle) * newRadius;
                    }
                            
                    float moveProgress = (currentTime - aimPoint.accuracyChangeTime) / 150.0f;
                    moveProgress = Math.min(1.0f, moveProgress);
                            
                    moveProgress = moveProgress < 0.5f ? 
                        2.0f * moveProgress * moveProgress : 
                        -1.0f + (4.0f - 2.0f * moveProgress) * moveProgress;
                            
                    aimPoint.currentX = aimPoint.startX + (aimPoint.targetX - aimPoint.startX) * moveProgress;
                    aimPoint.currentY = aimPoint.startY + (aimPoint.targetY - aimPoint.startY) * moveProgress;
                            
                    double finalX = aimPoint.currentX + RenderParameters.playerRecoilYaw * 0.5f;
                    double finalY = aimPoint.currentY + RenderParameters.playerRecoilPitch * 0.5f;
                            
                    Vec3d vec3d = new Vec3d(finalX, finalY, 100).normalize();
                    return vec3d.rotatePitch((float)(-pitch * Math.PI / 180))
                                  .rotateYaw((float)(-yaw * Math.PI / 180));
                }
            }
        }
        return getDefaultAccuracy(pitch, yaw, accuracy, rand);
    }


    private static Vec3d getDefaultAccuracy(float pitch, float yaw, final float accuracy, final Random rand) {
        final float randAccPitch = rand.nextFloat() * accuracy;
        final float randAccYaw = rand.nextFloat() * accuracy;
        Vec3d vec3d = new Vec3d(rand.nextBoolean() ? randAccYaw : (-randAccYaw), 
                               rand.nextBoolean() ? randAccPitch : (-randAccPitch), 
                               100).normalize();
        return vec3d.rotatePitch((float)(-pitch * Math.PI / 180))
                   .rotateYaw((float)(-yaw * Math.PI / 180));
    }

    public static float calculateAccuracy(final ItemGun item, final EntityLivingBase player) {
        // 在服务器端，使用EntityShootingAPI的精度计算
        if (!player.world.isRemote) {
            return EntityShootingAPI.calculateServerAccuracy(item, player);
        }
        
        final GunType gun = item.type;
        //新增枪管散射影响
        float accuracyBarrelFactor = 1.0f;
        if (GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel) != null) {
            ItemAttachment barrelAttachment = (ItemAttachment) GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel).getItem();
            accuracyBarrelFactor = barrelAttachment.type.barrel.accuracyFactor;
        };
        //新增激光散射影响
        float accuracyLaserFactor = 1.0f;
        if (GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Laser) != null) {
            ItemAttachment laserAttachment = (ItemAttachment) GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Laser).getItem();
            accuracyLaserFactor = laserAttachment.type.laser.accuracyFactor;
        };
        float acc = gun.bulletSpread * accuracyBarrelFactor * accuracyLaserFactor;
            
        if (player.posX != player.lastTickPosX || player.posZ != player.lastTickPosZ) {
            acc += gun.accuracyMoveOffset;
        }
        if (!player.onGround) {
            acc += gun.accuracyHoverOffset;
        }
        if (player.isSprinting()) {
            acc += gun.accuracySprintOffset;
        }
        //潜行处理在下面
//        if (player.isSneaking()) {
//            acc *= gun.accuracySneakFactor;
//        }
        
        //Client side
        if(player.world.isRemote) {
        	if(ClientRenderHooks.isAiming || ClientRenderHooks.isAimingScope) {
                boolean f1=true;
                if(player.world.isRemote) {
                    if(player==Minecraft.getMinecraft().player) {
                        if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
                            acc *= gun.accuracyThirdAimFactor;
                            f1=false;
                        }
                    }
                }
                if(f1) {
                    acc *= gun.accuracyAimFactor;
                }
            }
        	if (ModularWarfare.isLoadedModularMovements) {
                if (ClientListener.clientPlayerState.isCrawling) {
                    acc *= gun.accuracyCrawlFactor;
                } else if (player.isSneaking() || ClientListener.clientPlayerState.isSitting) {
                    acc *= gun.accuracySneakFactor;
                }
            } else {
                if (player.isSneaking()) {
                    acc *= gun.accuracySneakFactor;
                }
            }
        }else {//Server side
        	Boolean bb=ServerTickHandler.playerAimInstant.get(player.getUniqueID());
            if(bb!=null&&bb) {
                acc *= gun.accuracyAimFactor;
            }
            if (ModularWarfare.isLoadedModularMovements) {
                if (ServerListener.isCrawling(player.getEntityId())) {
                    acc *= gun.accuracyCrawlFactor;
                } else if (player.isSneaking() || ServerListener.isSitting(player.getEntityId())) {
                    acc *= gun.accuracySneakFactor;
                }
            } else {
                if (player.isSneaking()) {
                    acc *= gun.accuracySneakFactor;
                }
            }
        }

        acc*=PlayerStateManager.clientPlayerState.accuracyFactor;
        
        if (acc < 0) {
            acc = 0;
        }
        /** Bullet Accuracy **/
        if (player.getHeldItemMainhand() != null) {
            if (player.getHeldItemMainhand().getItem() instanceof ItemGun) {
                ItemBullet bullet = ItemGun.getUsedBullet(player.getHeldItemMainhand(), ((ItemGun) player.getHeldItemMainhand().getItem()).type);
                if (bullet != null) {
                    if (bullet.type != null) {
                        acc *= bullet.type.bulletAccuracyFactor;
                    }
                }
            }
        }
        return acc;
    }

//    public static float calculateAccuracyClient(final ItemGun item, final EntityPlayer player) {
//        final GunType gun = item.type;
//        //新增枪管散射影响
//        float accuracyBarrelFactor = 1.0f;
//        if (GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel) != null) {
//            ItemAttachment barrelAttachment = (ItemAttachment) GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel).getItem();
//            accuracyBarrelFactor = barrelAttachment.type.barrel.accuracyFactor;
//        };
//        float acc = gun.bulletSpread * accuracyBarrelFactor;
//        final GameSettings settings = Minecraft.getMinecraft().gameSettings;
//        if (settings.keyBindForward.isKeyDown() || settings.keyBindLeft.isKeyDown() || settings.keyBindBack.isKeyDown() || settings.keyBindRight.isKeyDown()) {
//            acc += 0.75f;
//        }
//        if (!player.onGround) {
//            acc += 1.5f;
//        }
//        if (player.isSprinting()) {
//            acc += 0.25f;
//        }
//        if (player.isSneaking()) {
//            acc *= gun.accuracySneakFactor;
//        }
//        
//      //Client side
//        if(player.world.isRemote) {
//        	if(ClientRenderHooks.isAiming || ClientRenderHooks.isAimingScope) {
//                acc *= gun.accuracyAimFactor;
//            }else {
//                
//            }
//        	if (ModularWarfare.isLoadedModularMovements) {
//                if (ClientListener.clientPlayerState.isCrawling) {
//                    acc *= gun.accuracyCrawlFactor;
//                } else if (player.isSneaking() || ClientListener.clientPlayerState.isSitting) {
//                    acc *= gun.accuracySneakFactor;
//                }
//            } else {
//                if (player.isSneaking()) {
//                    acc *= gun.accuracySneakFactor;
//                }
//            }
//        }else {//Server side
//        	Boolean bb=ServerTickHandler.playerAimInstant.get(player);
//            if(bb!=null&&bb) {
//                acc *= gun.accuracyAimFactor;
//            }else {
//            }
//            if (ModularWarfare.isLoadedModularMovements) {
//                if (ServerListener.isCrawling(player.getEntityId())) {
//                    acc *= gun.accuracyCrawlFactor;
//                } else if (player.isSneaking() || ServerListener.isSitting(player.getEntityId())) {
//                    acc *= gun.accuracySneakFactor;
//                }
//            } else {
//                if (player.isSneaking()) {
//                    acc *= gun.accuracySneakFactor;
//                }
//            }
//        }
//
//        
//        
//        if (acc < 0) {
//            acc = 0;
//        }
//        
//        /** Bullet Accuracy **/
//        if (player.getHeldItemMainhand() != null) {
//            if (player.getHeldItemMainhand().getItem() instanceof ItemGun) {
//                ItemBullet bullet = ItemGun.getUsedBullet(player.getHeldItemMainhand(), ((ItemGun) player.getHeldItemMainhand().getItem()).type);
//                if (bullet != null) {
//                    if (bullet.type != null) {
//                        acc *= bullet.type.bulletAccuracyFactor;
//                    }
//                }
//            }
//        }
//        return acc;
//    }

    @Nullable
    @SideOnly(Side.CLIENT)
    public static RayTraceResult rayTrace(Entity entity, double blockReachDistance, float partialTicks)
    {
        Vec3d vec3d = entity.getPositionEyes(partialTicks);
        Vec3d vec3d1 = entity.getLook(partialTicks);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);

        if(ModularWarfare.isLoadedModularMovements) {
            if (entity instanceof EntityPlayer) {
                vec3d = ModularMovementsHooks.onGetPositionEyes((EntityPlayer) entity, partialTicks);
            }
        }

        return entity.world.rayTraceBlocks(vec3d, vec3d2, false, true, false);
    }

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

    /**
     * Helper method which does a rayTrace for entities from an entity's eye level in the direction they are looking
     * with a specified range, using the tracePath method. Tidies up the code a bit. Border size defaults to 1.
     *
     * @param world
     * @param range
     * @return
     */
    @Nullable
    public static List<BulletHit> standardEntityRayTrace(Side side, World world, float rotationPitch, float rotationYaw, EntityLivingBase player, double range, ItemGun item, boolean isPunched) {
        // 基础检查
        if (world == null || player == null || item == null || item.type == null) {
            return null;
        }

        // 检查玩家手持物品
        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemGun)) {
            return null;
        }

        HashSet<Entity> hashset = new HashSet<Entity>(1);
        hashset.add(player);

        try {
            float accuracy = calculateAccuracy(item, player);
            float penetrate = item.type.gunPenetrateSize;
            float maxPenetrateBlockResistance = item.type.gunMaxPenetrateBlockResistance;
            float penetrateBlocksResistance = item.type.gunPenetrateBlocksResistance;

            ItemBullet usedBullet = ItemAmmo.getUsedBullet(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND));
            if (usedBullet != null) {
                penetrate *= usedBullet.type.bulletPenetrateFactor;
                maxPenetrateBlockResistance *= usedBullet.type.bulletBlockPenetrateFactor;
                penetrateBlocksResistance *= usedBullet.type.bulletBlockPenetrateFactor;
            }
            Vec3d dir = getGunAccuracy(rotationPitch, rotationYaw, accuracy, world.rand, player);

            if(side.isServer()) {
                // Server side code...
            }

            int ping = 0;
            if (player instanceof EntityPlayerMP) {
                final EntityPlayerMP entityPlayerMP = (EntityPlayerMP) player;
                ping = entityPlayerMP.ping;
            }

            Vec3d origin = player.getPositionEyes(1.0f);
            if(ModularWarfare.isLoadedModularMovements) {
                if (player instanceof EntityPlayer) {
                    origin = ModularMovementsHooks.onGetPositionEyes((EntityPlayer) player, 1.0f);
                }
            }

            return ModularWarfare.INSTANCE.RAY_CASTING.computeDetection(world, origin, dir, range, 0.001f, penetrate,
                    maxPenetrateBlockResistance, penetrateBlocksResistance, hashset, false, ping);
        } catch (Exception e) {
            // 如果发生任何错误，返回null
            return null;
        }
    }

    /**
     * 为实体设计的射线追踪方法
     * 移除了玩家特定的逻辑，如战术动作、ping值等
     * 
     * @param side 服务端或客户端
     * @param world 世界
     * @param rotationPitch 俯仰角度
     * @param rotationYaw 偏航角度
     * @param entity 实体
     * @param range 射程
     * @param item 武器
     * @param isPunched 是否被击中
     * @param weaponStack 武器堆栈（可为null，如果不使用手中武器）
     * @return 射线追踪结果
     */
    @Nullable
    public static List<BulletHit> standardEntityRayTraceForEntity(Side side, World world, float rotationPitch, float rotationYaw, EntityLivingBase entity, double range, ItemGun item, boolean isPunched, ItemStack weaponStack) {
        if (world == null || entity == null || item == null || item.type == null) {
            return null;
        }

        if (weaponStack != null && (weaponStack.isEmpty() || !(weaponStack.getItem() instanceof ItemGun))) {
            return null;
        }

        HashSet<Entity> hashset = new HashSet<Entity>(1);
        hashset.add(entity);

        try {
            float accuracy = calculateAccuracy(item, entity);
            float penetrate = item.type.gunPenetrateSize;
            float maxPenetrateBlockResistance = item.type.gunMaxPenetrateBlockResistance;
            float penetrateBlocksResistance = item.type.gunPenetrateBlocksResistance;

            ItemBullet usedBullet = null;
            if (weaponStack != null) {
                usedBullet = ItemAmmo.getUsedBullet(weaponStack);
            }
            
            if (usedBullet != null) {
                penetrate *= usedBullet.type.bulletPenetrateFactor;
                maxPenetrateBlockResistance *= usedBullet.type.bulletBlockPenetrateFactor;
                penetrateBlocksResistance *= usedBullet.type.bulletBlockPenetrateFactor;
            }
            
            Vec3d dir;
            if (side.isServer()) {
                dir = EntityShootingAPI.getServerDefaultAccuracy(rotationPitch, rotationYaw, accuracy, world.rand);
            } else {
                dir = getGunAccuracy(rotationPitch, rotationYaw, accuracy, world.rand, entity);
            }

            Vec3d origin = entity.getPositionEyes(1.0f);

            int ping = 0;


            if (side.isServer()) {
                return performSimpleAABBRayTrace(world, origin, dir, range, penetrate, maxPenetrateBlockResistance, penetrateBlocksResistance, hashset);
            } else {
            return ModularWarfare.INSTANCE.RAY_CASTING.computeDetection(world, origin, dir, range, 0.001f, penetrate,
                    maxPenetrateBlockResistance, penetrateBlocksResistance, hashset, false, ping);
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 执行简单的AABB射线检测（服务器端专用）
     */
    private static List<BulletHit> performSimpleAABBRayTrace(World world, Vec3d origin, Vec3d dir, double range, float penetrate, float maxPenetrateBlockResistance, float penetrateBlocksResistance, HashSet<Entity> excluded) {
        List<BulletHit> hits = new ArrayList<>();
        
        final float originPenetrateSize = penetrate;
        final float originBlockPenetrate = penetrateBlocksResistance;
        
        Vec3d endVec = origin.add(dir.scale(range));
        
        RayTraceResult blockResult = world.rayTraceBlocks(origin, endVec, false, true, false);
        if (blockResult != null && blockResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            double distance = blockResult.hitVec.distanceTo(origin);
            hits.add(new BulletHit(blockResult, distance, 0.0f, 0.0f));
            endVec = blockResult.hitVec;
        }
        
        AxisAlignedBB rayBox = new AxisAlignedBB(origin.x, origin.y, origin.z, endVec.x, endVec.y, endVec.z).grow(1.0);
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, rayBox);
        
        for (Entity ent : entities) {
            if (excluded.contains(ent) || !ent.canBeCollidedWith() || ent.isDead) {
                continue;
            }
            
            if (ent instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) ent;
                if (player.getHealth() > 0.0F) {
                    double entBorder = ent.getCollisionBorderSize();
                    if (entBorder == 0) {
                        entBorder = ModConfig.INSTANCE.general.collisionBorderSizeFixNonPlayer;
                    }
                    
                    AxisAlignedBB entityBb = ent.getEntityBoundingBox();
                    if (entityBb != null) {
                        entityBb = entityBb.grow(entBorder);
                        
                        try {
                            GetLivingAABBEvent aabbEvent = new GetLivingAABBEvent(player, entityBb);
                            MinecraftForge.EVENT_BUS.post(aabbEvent);
                            entityBb = aabbEvent.box;
                        } catch (Exception e) {
                        }
                        
                        RayTraceResult intercept = entityBb.calculateIntercept(origin, endVec);
                        if (intercept != null) {
                            double currentHitDistance = intercept.hitVec.distanceTo(origin);
                            if (currentHitDistance < range) {
                                RayTraceResult entityResult = new RayTraceResult(player, intercept.hitVec);
                                float remainingPenetrate = originPenetrateSize == 0 ? 1.0f : penetrate / originPenetrateSize;
                                float remainingBlockPenetrate = originBlockPenetrate == 0 ? 1.0f : penetrateBlocksResistance / originBlockPenetrate;
                                hits.add(new BulletHit(entityResult, currentHitDistance, remainingPenetrate, remainingBlockPenetrate));
                            }
                        }
                    }
                }
            }
            else if (ent instanceof EntityLivingBase && !(ent instanceof EntityPlayer)) {
                EntityLivingBase entityLivingBase = (EntityLivingBase) ent;
                if (entityLivingBase.getHealth() > 0.0F) {
                    double entBorder = ent.getCollisionBorderSize();
                    if (entBorder == 0) {
                        entBorder = ModConfig.INSTANCE.general.collisionBorderSizeFixNonPlayer;
                    }
                    
                    AxisAlignedBB entityBb = ent.getEntityBoundingBox();
                    if (entityBb != null) {
                        entityBb = entityBb.grow(entBorder);
                        
                        try {
                            GetLivingAABBEvent aabbEvent = new GetLivingAABBEvent(entityLivingBase, entityBb);
                            MinecraftForge.EVENT_BUS.post(aabbEvent);
                            entityBb = aabbEvent.box;
                        } catch (Exception e) {
                        }
                        
                        RayTraceResult intercept = entityBb.calculateIntercept(origin, endVec);
                        if (intercept != null) {
                            double currentHitDistance = intercept.hitVec.distanceTo(origin);
                            if (currentHitDistance < range) {
                                RayTraceResult entityResult = new RayTraceResult(entityLivingBase, intercept.hitVec);
                                float remainingPenetrate = originPenetrateSize == 0 ? 1.0f : penetrate / originPenetrateSize;
                                float remainingBlockPenetrate = originBlockPenetrate == 0 ? 1.0f : penetrateBlocksResistance / originBlockPenetrate;
                                hits.add(new BulletHit(entityResult, currentHitDistance, remainingPenetrate, remainingBlockPenetrate));
                            }
                        }
                    }
                }
            } 
            else if (ent instanceof EntityGrenade) {
                float entBorder = ent.getCollisionBorderSize();
                AxisAlignedBB entityBb = ent.getEntityBoundingBox();
                if (entityBb != null) {
                    entityBb = entityBb.grow(entBorder, entBorder, entBorder);
                    RayTraceResult intercept = entityBb.calculateIntercept(origin, endVec);
                    if (intercept != null) {
                        double currentHitDistance = intercept.hitVec.distanceTo(origin);
                        if (currentHitDistance < range) {
                            RayTraceResult entityResult = new RayTraceResult(ent, intercept.hitVec);
                            float remainingPenetrate = originPenetrateSize == 0 ? 1.0f : penetrate / originPenetrateSize;
                            float remainingBlockPenetrate = originBlockPenetrate == 0 ? 1.0f : penetrateBlocksResistance / originBlockPenetrate;
                            hits.add(new BulletHit(entityResult, currentHitDistance, remainingPenetrate, remainingBlockPenetrate));
                        }
                    }
                }
            }
        }
        
        hits.sort((a, b) -> Double.compare(a.distance, b.distance));
        
        return hits;
    }
}