package com.modularwarfare.utility;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAttachment;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.hitbox.hits.BulletHit;
import com.modularwarfare.common.network.PacketGunTrail;
import com.modularwarfare.common.network.PacketGunTrailAskServer;
import mchhui.modularmovements.coremod.ModularMovementsHooks;
import mchhui.modularmovements.tactical.client.ClientLitener;
import mchhui.modularmovements.tactical.server.ServerListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Random;

public class RayUtil {

    public static Vec3d getGunAccuracy(float pitch, float yaw, final float accuracy, final Random rand) {
        final float randAccPitch = rand.nextFloat() * accuracy;
        final float randAccYaw = rand.nextFloat() * accuracy;
        /*
         * 2023/8/5
         * 修复万向轴死锁带来的bug
         * */
        Vec3d vec3d = new Vec3d(rand.nextBoolean() ? randAccYaw : (-randAccYaw), rand.nextBoolean() ? randAccPitch : (-randAccPitch), 100).normalize();
        vec3d = vec3d.rotatePitch((float)(-pitch * Math.PI / 180));
        vec3d = vec3d.rotateYaw((float)(-yaw * Math.PI / 180));
        return vec3d;
    }

    public static float calculateAccuracy(final ItemGun item, final EntityLivingBase player) {
        final GunType gun = item.type;
        //新增枪管散射影响
        float accuracyBarrelFactor = 1.0f;
        if (GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel) != null) {
            ItemAttachment barrelAttachment = (ItemAttachment) GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel).getItem();
            accuracyBarrelFactor = barrelAttachment.type.barrel.accuracyFactor;
        };
        float acc = gun.bulletSpread * accuracyBarrelFactor;
            
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
        Vec3d vec3d2 = vec3d.addVector(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);

        if(ModularWarfare.isLoadedModularMovements) {
            if (entity instanceof EntityPlayer) {
                vec3d = ModularMovementsHooks.onGetPositionEyes((EntityPlayer) entity, partialTicks);
            }
        }

        return entity.world.rayTraceBlocks(vec3d, vec3d2, false, true, false);
    }

    /**
     * Attacks the given entity with the given damage source and amount, but
     * preserving the entity's original velocity instead of applying knockback, as
     * would happen with
     * {@link EntityLivingBase#attackEntityFrom(DamageSource, float)} <i>(More
     * accurately, calls that method as normal and then resets the entity's velocity
     * to what it was before).</i> Handy for when you need to damage an entity
     * repeatedly in a short space of time.
     *
     * @param entity The entity to attack
     * @param source The source of the damage
     * @param amount The amount of damage to apply
     * @return True if the attack succeeded, false if not.
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
    public static BulletHit standardEntityRayTrace(Side side, World world, float rotationPitch, float rotationYaw, EntityLivingBase player, double range, ItemGun item, boolean isPunched) {

        HashSet<Entity> hashset = new HashSet<Entity>(1);
        hashset.add(player);

        float accuracy = calculateAccuracy(item, player);
        Vec3d dir = getGunAccuracy(rotationPitch, rotationYaw, accuracy, player.world.rand);
        double dx = dir.x * range;
        double dy = dir.y * range;
        double dz = dir.z * range;

        if(side.isServer()) {
//            ModularWarfare.NETWORK.sendToDimension(new PacketGunTrail(item.type,player.posX, player.getEntityBoundingBox().minY + player.getEyeHeight() - 0.10000000149011612, player.posZ, player.motionX, player.motionZ, dir.x, dir.y, dir.z, range, 10, isPunched), player.world.provider.getDimension());
        } else {
            ItemStack gunStack=player.getHeldItemMainhand();
            ItemStack bulletStack=null;
            String model=null;
            String tex=null;
            boolean glow=false;
            if(gunStack.getItem() instanceof ItemGun) {
                GunType gunType=((ItemGun)gunStack.getItem()).type;
                if (gunType.acceptedBullets != null) {
                    bulletStack= new ItemStack(gunStack.getTagCompound().getCompoundTag("bullet"));
                }else {
                    ItemStack stackAmmo = new ItemStack(gunStack.getTagCompound().getCompoundTag("ammo"));
                    if(stackAmmo!=null&&!stackAmmo.isEmpty()&&stackAmmo.hasTagCompound()) {
                        bulletStack= new ItemStack(stackAmmo.getTagCompound().getCompoundTag("bullet"));  
                    }
                }
            }
            if (bulletStack != null) {
                if (bulletStack.getItem() instanceof ItemBullet) {
                    BulletType bulletType = ((ItemBullet)bulletStack.getItem()).type;
                    model = bulletType.trailModel;
                    tex = bulletType.trailTex;
                    glow = bulletType.trailGlow;
                }
            }
            ModularWarfare.NETWORK.sendToServer(new PacketGunTrailAskServer(item.type,model,tex,glow,player.posX,player.getEntityBoundingBox().minY + player.getEyeHeight() - 0.10000000149011612, player.posZ, player.motionX, player.motionZ, dir.x, dir.y, dir.z, range, 10, isPunched));
        }

        int ping = 0;
        if (player instanceof EntityPlayerMP) {
            final EntityPlayerMP entityPlayerMP = (EntityPlayerMP) player;
            ping = entityPlayerMP.ping;
        }

        Vec3d offsetVec = player.getPositionEyes(1.0f);
        if(ModularWarfare.isLoadedModularMovements) {
            if (player instanceof EntityPlayer) {
                offsetVec = ModularMovementsHooks.onGetPositionEyes((EntityPlayer) player, 1.0f);
            }
        }

        return ModularWarfare.INSTANCE.RAY_CASTING.computeDetection(world, (float) offsetVec.x, (float) offsetVec.y, (float) offsetVec.z, (float) (offsetVec.x + dx), (float) (offsetVec.y + dy), (float) (offsetVec.z + dz), 0.001f, hashset, false, ping);
    }
}