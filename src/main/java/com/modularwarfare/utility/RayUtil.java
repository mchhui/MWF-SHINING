package com.modularwarfare.utility;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.hitbox.hits.BulletHit;
import com.modularwarfare.common.network.PacketGunTrailAskServer;
import com.modularwarfare.common.playerstate.PlayerStateManager;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import mchhui.modularmovements.coremod.ModularMovementsHooks;
import mchhui.modularmovements.tactical.client.ClientLitener;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class RayUtil {

    // 存储当前位置和目标位置的静态变量
    private static double currentX = 0;
    private static double currentY = 0;
    private static double targetX = 0;
    private static double targetY = 0;
    private static double startX = 0;
    private static double startY = 0;
    private static float lastAccuracy = 0;
    private static long accuracyChangeTime = 0;
    private static final long ACCURACY_TRANSITION_TIME = 100; // 100ms的过渡时间
    
    public static Vec3d getGunAccuracy(float pitch, float yaw, final float accuracy, final Random rand) {

        long seed = (long)(pitch * 10000) + (long)(yaw * 10000) + (long)(accuracy * 10000);
        rand.setSeed(seed);
        

        Vec3d defaultVec = getDefaultAccuracy(pitch, yaw, accuracy, rand);
        
        // 检查是否为客户端环境且启用了增强瞄准
        if(Minecraft.getMinecraft() != null && Minecraft.getMinecraft().world != null && Minecraft.getMinecraft().world.isRemote) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if(player == null) return defaultVec;
                    
            ItemStack heldItem = player.getHeldItemMainhand();
            if(!heldItem.isEmpty() && heldItem.getItem() instanceof ItemGun) {
                ItemGun itemGun = (ItemGun)heldItem.getItem();
                if(itemGun.type != null && itemGun.type.useEnhancedAiming) {
                    long currentTime = System.currentTimeMillis();
                            
                    if(Math.abs(lastAccuracy - accuracy) > 0.0001f || currentTime - accuracyChangeTime > 200) {
                        accuracyChangeTime = currentTime;
                        lastAccuracy = accuracy;
                                
                        startX = currentX;
                        startY = currentY;
                                
                        double angle = Math.atan2(currentY, currentX) + (rand.nextDouble() - 0.5) * Math.PI * 1.5;
                        double currentRadius = Math.sqrt(currentX * currentX + currentY * currentY);
                                
                        double newRadius = Math.min(accuracy, currentRadius + (rand.nextDouble() - 0.3) * accuracy * 0.8);
                                
                        targetX = Math.cos(angle) * newRadius;
                        targetY = Math.sin(angle) * newRadius;
                    }
                            
                    float moveProgress = (currentTime - accuracyChangeTime) / 150.0f;
                    moveProgress = Math.min(1.0f, moveProgress);
                            
                    moveProgress = moveProgress < 0.5f ? 
                        2.0f * moveProgress * moveProgress : 
                        -1.0f + (4.0f - 2.0f * moveProgress) * moveProgress;
                            
                    currentX = startX + (targetX - startX) * moveProgress;
                    currentY = startY + (targetY - startY) * moveProgress;
                            
                    // 只在客户端添加后坐力效果
                    double finalX = currentX + RenderParameters.playerRecoilYaw * 0.5f;
                    double finalY = currentY + RenderParameters.playerRecoilPitch * 0.5f;
                            
                    Vec3d vec3d = new Vec3d(finalX, finalY, 100).normalize();
                    return vec3d.rotatePitch((float)(-pitch * Math.PI / 180))
                                  .rotateYaw((float)(-yaw * Math.PI / 180));
                }
            }
        }
        return defaultVec;
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
                if (ClientLitener.clientPlayerState.isCrawling) {
                    acc *= gun.accuracyCrawlFactor;
                } else if (player.isSneaking() || ClientLitener.clientPlayerState.isSitting) {
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
//                if (ClientLitener.clientPlayerState.isCrawling) {
//                    acc *= gun.accuracyCrawlFactor;
//                } else if (player.isSneaking() || ClientLitener.clientPlayerState.isSitting) {
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
            Vec3d dir = getGunAccuracy(rotationPitch, rotationYaw, accuracy, world.rand);

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
}