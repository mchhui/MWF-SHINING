package com.modularwarfare.common.guns.manager;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.EntityShootingAPI;
import com.modularwarfare.api.WeaponFireEvent;
import com.modularwarfare.api.WeaponHitEvent;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.common.armor.ArmorType;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;
import com.modularwarfare.common.entity.EntityExplosiveProjectile;
import com.modularwarfare.common.entity.EntityThrowerProjectile;
import com.modularwarfare.common.entity.environment.EntityShell;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.hitbox.hits.BulletHit;
import com.modularwarfare.common.hitbox.hits.OBBHit;
import com.modularwarfare.common.hitbox.hits.PlayerHit;
import com.modularwarfare.common.hitbox.maths.EnumHitboxType;
import com.modularwarfare.raycast.obb.OBBModelBox;
import com.modularwarfare.common.network.*;
import com.modularwarfare.common.playerstate.PlayerStateManager;
import com.modularwarfare.utility.RayUtil;
import com.teamderpy.shouldersurfing.client.ShoulderHelper;
import com.teamderpy.shouldersurfing.client.ShoulderInstance;
import mchhui.modularmovements.tactical.client.ClientListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.relauncher.Side;
import com.modularwarfare.common.vector.Matrix4f;
import com.modularwarfare.common.vector.Vector3f;
import com.modularwarfare.raycast.obb.OBBModelBox;
import com.modularwarfare.raycast.obb.OBBPlayerManager;
import com.modularwarfare.raycast.obb.OBBPlayerManager.OBBDebugObject;
import com.modularwarfare.raycast.DefaultRayCasting;
import com.modularwarfare.client.model.InstantBulletTeslaRender;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class ShotManager {
    public static boolean defemptyclickLock=true;

    public static void fireClient(EntityPlayer entityPlayer, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode) {
        GunType gunType = itemGun.type;

        if (ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).reloading) {
            if(gunType.allowReloadFiring) {
                ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).stopReload();
                ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).reset();
                ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).updateCurrentItem(entityPlayer);
            }
        }

        // Can fire checks
        if (!checkCanFireClient(entityPlayer, world, gunStack, itemGun, fireMode)) {
            return;
        }

        int shotCount = fireMode == WeaponFireMode.BURST ? gunStack.getTagCompound().getInteger("shotsremaining") > 0 ? gunStack.getTagCompound().getInteger("shotsremaining") : gunType.numBurstRounds : 1;

        // Weapon pre fire event
        WeaponFireEvent.PreClient preFireEvent = new WeaponFireEvent.PreClient(entityPlayer, gunStack, itemGun, gunType.weaponMaxRange);
        MinecraftForge.EVENT_BUS.post(preFireEvent);
        if (preFireEvent.isCanceled())
            return;

        if (preFireEvent.getResult() == Event.Result.DEFAULT || preFireEvent.getResult() == Event.Result.ALLOW) {
            if (!ItemGun.hasNextShot(gunStack)) {
                if (fireMode == WeaponFireMode.BURST) gunStack.getTagCompound().setInteger("shotsremaining", 0);
                if(defemptyclickLock) {
                    gunType.playClientSound(entityPlayer, WeaponSoundType.DryFire);
                    ModularWarfare.PROXY.onShootFailedAnimation(entityPlayer, gunType.internalName);
                    defemptyclickLock=false;
                }
                return;
            }
        }

        ModularWarfare.PROXY.onShootAnimation(entityPlayer, gunType.internalName, gunType.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw);

        // Sound
        if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Barrel) != null) {
            ItemAttachment barrelAttachment = (ItemAttachment) GunType.getAttachment(gunStack, AttachmentPresetEnum.Barrel).getItem();
            if (barrelAttachment.type.barrel.isSuppressor) {
                gunType.playClientSound(entityPlayer, WeaponSoundType.FireSuppressed);
            } else {
                if (ItemGun.getAmmoCount(entityPlayer.getHeldItemMainhand()) <= 1 && itemGun.type.weaponSoundMap.containsKey(WeaponSoundType.FireLast)) {
                    gunType.playClientSound(entityPlayer, WeaponSoundType.FireLast);
                } else {
                    gunType.playClientSound(entityPlayer, WeaponSoundType.Fire);
                }
            }
        } else if (GunType.isPackAPunched(gunStack)) {
            gunType.playClientSound(entityPlayer, WeaponSoundType.Punched);
            if (ItemGun.getAmmoCount(entityPlayer.getHeldItemMainhand()) <= 1 && itemGun.type.weaponSoundMap.containsKey(WeaponSoundType.FireLast)) {
                gunType.playClientSound(entityPlayer, WeaponSoundType.FireLast);
            } else {
                gunType.playClientSound(entityPlayer, WeaponSoundType.Fire);
            }
        } else {
            if (ItemGun.getAmmoCount(entityPlayer.getHeldItemMainhand()) <= 1 && itemGun.type.weaponSoundMap.containsKey(WeaponSoundType.FireLast)) {
                gunType.playClientSound(entityPlayer, WeaponSoundType.FireLast);
            } else {
                gunType.playClientSound(entityPlayer, WeaponSoundType.Fire);
            }
        }

        if (gunType.weaponType == WeaponType.BoltSniper || gunType.weaponType == WeaponType.Shotgun) {
            gunType.playClientSound(entityPlayer, WeaponSoundType.Pump);
        }

        // Burst Stuff
        if (fireMode == WeaponFireMode.BURST) {
            shotCount = shotCount - 1;
            gunStack.getTagCompound().setInteger("shotsremaining", shotCount);
        }
        
        ClientTickHandler.playerNextTime.put(entityPlayer.getUniqueID(), System.currentTimeMillis()+(long)((60f*1000/gunType.roundsPerMin)/PlayerStateManager.clientPlayerState.roundsPerMinFactor/PlayerStateManager.clientPlayerState.devetionRoundsPerMinFactor));

        if ((gunType.dropBulletCasing)) {
            /**
             * Drop casing
             */
            int numBullets = gunType.numBullets;
            ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
            if (bulletItem != null) {
                if (bulletItem.type.isSlug) {
                    numBullets = 1;
                }
            }
            GunEnhancedRenderConfig cfg=ModularWarfare.getRenderConfig(gunType, GunEnhancedRenderConfig.class);

            EntityShell shell = new EntityShell(world, entityPlayer,gunStack, itemGun, bulletItem);

            shell.setHeadingFromThrower(entityPlayer, entityPlayer.rotationPitch+cfg.extra.shellPitchOffset, entityPlayer.rotationYaw + 110+cfg.extra.shellYawOffset, 0.0F, 0.2F, 5,0.1f+cfg.extra.shellForwardOffset);
            world.spawnEntity(shell);
        }

        ItemGun.consumeShot(gunStack);

        /**
         * Hit Register
         */
        if (gunType.weaponType == WeaponType.Launcher || gunType.weaponType == WeaponType.Thrower) {
            ModularWarfare.NETWORK.sendToServer(new PacketGunFire(gunType.internalName, gunType.fireTickDelay, gunType.recoilPitch, gunType.recoilYaw, gunType.recoilAimReducer, gunType.bulletSpread, entityPlayer.rotationPitch, entityPlayer.rotationYaw));
        } else {
            DefaultRayCasting.onShot();
            fireClientSide(entityPlayer, itemGun);
        }
        
        /**
         * recoil
         * */

        RenderParameters.rate = Math.min(RenderParameters.rate + 0.03f, 1f);

        float recoilPitchGripFactor = 1.0f;
        float recoilYawGripFactor = 1.0f;

        float recoilPitchBarrelFactor = 1.0f;
        float recoilYawBarrelFactor = 1.0f;
        
        float recoilPitchStockFactor = 1.0f;
        float recoilYawStockFactor = 1.0f;

        float recoilPitchLaserFactor = 1.0f;
        float recoilYawLaserFactor = 1.0f;

        float recoilPistolgripFactor = 1.0f;
        float recoilYawPistolgripFactor = 1.0f;

        float recoilPitchHandguardFactor = 1.0f;
        float recoilYawHandguardFactor = 1.0f;

        if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Grip) != null) {
            ItemAttachment gripAttachment = (ItemAttachment) GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Grip).getItem();
            recoilPitchGripFactor = gripAttachment.type.grip.recoilPitchFactor;
            recoilYawGripFactor = gripAttachment.type.grip.recoilYawFactor;
        }

        if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel) != null) {
            ItemAttachment barrelAttachment = (ItemAttachment) GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel).getItem();
            recoilPitchBarrelFactor = barrelAttachment.type.barrel.recoilPitchFactor;
            recoilYawBarrelFactor = barrelAttachment.type.barrel.recoilYawFactor;
        }
        
        if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Stock) != null) {
            ItemAttachment stockAttachment = (ItemAttachment) GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Stock).getItem();
            recoilPitchStockFactor = stockAttachment.type.stock.recoilPitchFactor;
            recoilYawStockFactor = stockAttachment.type.stock.recoilYawFactor;
        }

        if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Laser) != null) {
            ItemAttachment laserAttachment = (ItemAttachment) GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Laser).getItem();
            recoilPitchLaserFactor = laserAttachment.type.laser.recoilPitchFactor;
            recoilYawLaserFactor = laserAttachment.type.laser.recoilYawFactor;
        }

        if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Pistolgrip) != null) {
            ItemAttachment pistolgripAttachment = (ItemAttachment) GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Pistolgrip).getItem();
            recoilPistolgripFactor = pistolgripAttachment.type.pistolgrip.recoilPitchFactor;
            recoilYawPistolgripFactor = pistolgripAttachment.type.pistolgrip.recoilYawFactor;
        }

        if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Handguard) != null) {
            ItemAttachment handguardAttachment = (ItemAttachment) GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Handguard).getItem();
            recoilPitchHandguardFactor = handguardAttachment.type.handguard.recoilPitchFactor;
            recoilYawHandguardFactor = handguardAttachment.type.handguard.recoilYawFactor;
        }

        boolean isCrawling = false;
        if(ModularWarfare.isLoadedModularMovements){
            if(ClientListener.clientPlayerState.isCrawling){
                isCrawling = true;
            }
        }
        float offsetYaw = 0;
        float offsetPitch = 0;
        if (!(ClientRenderHooks.isAiming || ClientRenderHooks.isAimingScope)) {
            offsetPitch = gunType.recoilPitch;
            offsetPitch += ((gunType.randomRecoilPitch * 2) - gunType.randomRecoilPitch);
            offsetPitch *= (recoilPitchGripFactor * recoilPitchBarrelFactor * recoilPitchStockFactor * recoilPitchLaserFactor * recoilPistolgripFactor * recoilPitchHandguardFactor);


            offsetYaw = gunType.recoilYaw;
            offsetYaw *= new Random().nextFloat() * (gunType.randomRecoilYaw * 2) - gunType.randomRecoilYaw;
            offsetYaw *= recoilYawGripFactor * recoilYawBarrelFactor * recoilYawStockFactor * recoilYawLaserFactor * recoilYawPistolgripFactor * recoilYawHandguardFactor;
            offsetYaw *= RenderParameters.rate * (isCrawling ? 0.2f : 1.0f);
            offsetYaw *= RenderParameters.phase ? 1 : -1;
        } else {
            offsetPitch = gunType.recoilPitch;
            offsetPitch += ((gunType.randomRecoilPitch * 2) - gunType.randomRecoilPitch);
            offsetPitch *= (recoilPitchGripFactor * recoilPitchBarrelFactor * recoilPitchStockFactor * recoilPitchLaserFactor * recoilPistolgripFactor * recoilPitchHandguardFactor);
            offsetPitch *= gunType.recoilAimReducer;

            offsetYaw = gunType.recoilYaw;
            offsetYaw *= new Random().nextFloat() * (gunType.randomRecoilYaw * 2) - gunType.randomRecoilYaw;
            offsetYaw *= recoilYawGripFactor * recoilYawBarrelFactor * recoilYawStockFactor * recoilYawLaserFactor * recoilYawPistolgripFactor * recoilYawHandguardFactor;
            offsetYaw *= RenderParameters.rate * (isCrawling ? 0.2f : 1.0f);
            offsetYaw *= gunType.recoilAimReducer;
            offsetYaw *= RenderParameters.phase ? 1 : -1;
        }
        if(ModularWarfare.isLoadedModularMovements) {
            if(ClientListener.clientPlayerState.isCrawling) {
                offsetPitch*=gunType.recoilCrawlPitchFactor;
                offsetYaw*=gunType.recoilCrawlYawFactor;
            }
        }
        offsetYaw*=PlayerStateManager.clientPlayerState.recoilYawFactor;
        offsetPitch*=PlayerStateManager.clientPlayerState.recoilPitchFactor;

        if (RenderParameters.playerRecoilYaw < 0.1F && RenderParameters.playerRecoilPitch < 0.1F) {
            ClientTickHandler.startAntiRecoilTime = System.currentTimeMillis();
        }

        RenderParameters.playerRecoilPitch += offsetPitch;
        if (Math.random() > 0.5f) {
            RenderParameters.playerRecoilYaw += offsetYaw;
        } else {
            RenderParameters.playerRecoilYaw -= offsetYaw;
        }
        RenderParameters.playerAntiRecoilFactor = gunType.antiRecoilFactor;
        RenderParameters.playerAntiRecoilStartTime = gunType.antiRecoilStartTime;
        RenderParameters.antiRecoilPitch = 0;
        RenderParameters.antiRecoilYaw = 0;
        RenderParameters.phase = !RenderParameters.phase;
    }

    public static boolean checkCanFireClient(EntityPlayer entityPlayer, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode) {
        if(entityPlayer.isSpectator()) {
            return false;
        }
        if(itemGun.type.animationType==WeaponAnimationType.BASIC) {
            if(ItemGun.isClientReloading(entityPlayer)) {
                return false;
            }
        }
        if (ItemGun.isOnShootCooldown(entityPlayer.getUniqueID())
                || ClientRenderHooks.getAnimMachine(entityPlayer).attachmentMode
                || (!itemGun.type.allowSprintFiring && entityPlayer.isSprinting())
                || !itemGun.type.hasFireMode(fireMode)) {
            return false;
        }
        if (AnimationController.getController(entityPlayer, null) != null) {
            if(!AnimationController.getController(entityPlayer, null).isCouldShoot()) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean verifShot(Entity user, float rotationPitch, float rotationYaw, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, final int clientFireTickDelay, final float recoilPitch, final float recoilYaw, final float recoilAimReducer, final float bulletSpread) {
        boolean failed = false;
        GunType gunType = itemGun.type;
        ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
        if (bulletItem == null) {
            failed = true;
        }
        if (user instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer)user;
            if (!ShotValidation.verifShot(entityPlayer, gunStack, itemGun, fireMode, clientFireTickDelay, recoilPitch, recoilYaw, recoilAimReducer, bulletSpread)) {
                failed = true;
                if (ModConfig.INSTANCE.general.modified_pack_server_kick) {
                    ((EntityPlayerMP)entityPlayer).connection.disconnect(new TextComponentString("[ModularWarfare] Kicked for client-side modified content-pack. (Bad RPM/Recoil for the gun: " + itemGun.type.internalName + ") [RPM should be: " + itemGun.type.roundsPerMin + "]"));
                }
            }
            if (!ItemGun.hasNextShot(gunStack)) {
                failed = true;
                if (ItemGun.canDryFire) {
                    gunType.playSound(entityPlayer, WeaponSoundType.DryFire, gunStack);
                    ItemGun.canDryFire = false;
                }
                if (fireMode == WeaponFireMode.BURST) {
                    gunStack.getTagCompound().setInteger("shotsremaining", 0);
                }
            }
        }
        return failed;
    }
    
    private static void doGunSound(GunType gunType, ItemStack gunStack, Entity user) {
        if (user instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer)user;
            if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Barrel) != null) {
                gunType.playSound(entityPlayer, WeaponSoundType.FireSuppressed, gunStack, entityPlayer);
            } else if (GunType.isPackAPunched(gunStack)) {
                gunType.playSound(entityPlayer, WeaponSoundType.Punched, gunStack, entityPlayer);
                gunType.playSound(entityPlayer, WeaponSoundType.Fire, gunStack, entityPlayer);
            } else {
                gunType.playSound(entityPlayer, WeaponSoundType.Fire, gunStack, entityPlayer);
            }
        }
    }
    
    private static int computePellet(GunType gunType, ItemStack gunStack, @Nullable Entity user) {
        int numBullets = gunType.numBullets;
        ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
        if (bulletItem.type.isSlug) {
            numBullets = 1;
        }
        return numBullets;
    }
    
    private static int computeShotCount(GunType gunType, ItemStack gunStack, WeaponFireMode fireMode, @Nullable Entity user) {
        return fireMode == WeaponFireMode.BURST ? gunStack.getTagCompound().getInteger("shotsremaining") > 0 ? gunStack.getTagCompound().getInteger("shotsremaining") : gunType.numBurstRounds : 1;
    }
    
    private static void handleFireRayGun(EntityPlayer entityPlayer, float rotationPitch, float rotationYaw, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, final int clientFireTickDelay, final float recoilPitch, final float recoilYaw, final float recoilAimReducer, final float bulletSpread, final int weaponRange) {
        GunType gunType = itemGun.type;
        int shotCount = computeShotCount(gunType, gunStack, fireMode, entityPlayer);
        int numBullets = computePellet(gunType, gunStack, entityPlayer);
        ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
        List<BulletHit> rayTraceList = new ArrayList<>();
        for (int i = 0; i < numBullets; i++) {
            List<BulletHit> rayTrace = RayUtil.standardEntityRayTrace(Side.SERVER, world, rotationPitch, rotationYaw, entityPlayer, weaponRange, itemGun, GunType.isPackAPunched(gunStack));
            if (rayTrace == null) {
                continue;
            }
            rayTraceList.addAll(rayTrace);
        }

        boolean headshot = false;
        Iterator<BulletHit> rayTraceIterator = rayTraceList.iterator();
        while (rayTraceIterator.hasNext() && !world.isRemote) {
            BulletHit rayTrace = rayTraceIterator.next();
            if (rayTrace instanceof PlayerHit) {
                final EntityPlayer victim = ((PlayerHit)rayTrace).getEntity();
                if (victim == null || victim.isDead || victim.getHealth() <= 0.f) {
                    rayTraceIterator.remove();
                    continue;
                }
                gunType.playSoundPos(victim.getPosition(), world, WeaponSoundType.Penetration);
                headshot = ((PlayerHit)rayTrace).hitbox.type.equals(EnumHitboxType.HEAD);
                if (entityPlayer instanceof EntityPlayerMP) {
                    ModularWarfare.NETWORK.sendTo(new PacketPlayHitmarker(headshot), (EntityPlayerMP)entityPlayer);
                    ModularWarfare.NETWORK.sendTo(new PacketPlaySound(victim.getPosition(), "flyby", 1f, 1f), (EntityPlayerMP)victim);
                    if (ModConfig.INSTANCE.hud.snap_fade_hit) {
                        ModularWarfare.NETWORK.sendTo(new PacketPlayerHit(), (EntityPlayerMP)victim);
                    }
                }
                continue;
            }
            Entity targetEnt = rayTrace.getEntity();
            if (targetEnt == null) {
                rayTraceIterator.remove();
                continue;
            }
            if (targetEnt instanceof EntityGrenade) {
                ((EntityGrenade)targetEnt).explode();
                continue;
            }
            if (targetEnt instanceof EntityLivingBase) {
                final EntityLivingBase victim = (EntityLivingBase)targetEnt;
                gunType.playSoundPos(victim.getPosition(), world, WeaponSoundType.Penetration);
                headshot = ItemGun.canEntityGetHeadshot(victim) && rayTrace.rayTraceResult.hitVec.y >= victim.getPosition().getY() + victim.getEyeHeight() - 0.15f;
                if (entityPlayer instanceof EntityPlayerMP) {
                    ModularWarfare.NETWORK.sendTo(new PacketPlayHitmarker(headshot), (EntityPlayerMP)entityPlayer);
                }
                continue;
            }
            if (rayTrace.rayTraceResult != null && rayTrace.rayTraceResult.hitVec != null) {
                BlockPos blockPos = rayTrace.rayTraceResult.getBlockPos();
                ItemGun.playImpactSound(world, rayTrace.rayTraceResult, gunType);
                gunType.playSoundPos(blockPos, world, WeaponSoundType.Crack, entityPlayer, 1.0f, false);
                ItemGun.doHit(rayTrace.rayTraceResult, entityPlayer);
                ItemGun.playHitEffect(world, rayTrace.rayTraceResult);
            }
        }
        if (postFireEvent.getHits() != null && !postFireEvent.getHits().isEmpty()) {
            List<BulletHit> hits = postFireEvent.getHits();
            for (BulletHit bulletHit : hits) {
                if (bulletHit == null) {
                    continue;
                }
                Entity targetEntity = bulletHit.getEntity();
                if (targetEntity == null || targetEntity == entityPlayer) {
                    continue;
                }

                // 获取碰撞箱名称
                String hitboxName = "";
                if (bulletHit instanceof PlayerHit) {
                    PlayerHit playerHit = (PlayerHit)bulletHit;
                    hitboxName = playerHit.hitbox.type.name();
                } else if (bulletHit instanceof OBBHit) {
                    OBBHit obbHit = (OBBHit)bulletHit;
                    hitboxName = obbHit.box.name;
                }

                // Weapon pre hit event
                WeaponHitEvent.Pre preHitEvent = new WeaponHitEvent.Pre(entityPlayer, gunStack, itemGun, headshot, postFireEvent.getDamage(), bulletHit.remainingPenetrate, bulletHit.remainingBlockPenetrate, targetEntity, bulletHit.distance, hitboxName);
                MinecraftForge.EVENT_BUS.post(preHitEvent);
                if (preHitEvent.isCanceled())
                    return;

                if (headshot) {
                    preHitEvent.setDamage(preHitEvent.getDamage() + gunType.gunDamageHeadshotBonus);
                }
                if (gunType.gunPenetrationDamageFalloff && preHitEvent.getPenetrateDamageFactor() > 0) {
                    preHitEvent.setDamage(preHitEvent.getDamage() * preHitEvent.getPenetrateDamageFactor());
                }
                if (gunType.gunPenetrateBlocksDamageFalloffFactor > 0 && preHitEvent.getPenetrateBlockDamageFactor() > 0 && preHitEvent.getPenetrateBlockDamageFactor() < 1) {
                    preHitEvent.setDamage(preHitEvent.getDamage() * preHitEvent.getPenetrateBlockDamageFactor() * gunType.gunPenetrateBlocksDamageFalloffFactor);
                }
                if (preHitEvent.getDistance() > gunType.weaponEffectiveRange) {
                    preHitEvent.setDamage((float)(preHitEvent.getDamage() * (1 - (preHitEvent.getDistance() - gunType.weaponEffectiveRange) / (gunType.weaponMaxRange - gunType.weaponEffectiveRange))));
                } else if (preHitEvent.getDistance() >= gunType.weaponMaxRange) {
                    preHitEvent.setDamage((float)(preHitEvent.getDamage() * 0));
                }

                if (targetEntity instanceof EntityLivingBase) {
                    EntityLivingBase targetELB = (EntityLivingBase)targetEntity;
                    if (bulletItem.type != null) {
                        preHitEvent.setDamage(preHitEvent.getDamage() * bulletItem.type.bulletDamageFactor);
                        if (bulletItem.type.bulletProperties != null) {
                            if (!bulletItem.type.bulletProperties.isEmpty()) {
                                BulletProperty bulletProperty = bulletItem.type.bulletProperties.get(targetELB.getName()) != null ? bulletItem.type.bulletProperties.get(targetELB.getName()) : bulletItem.type.bulletProperties.get("All");
                                if (bulletProperty.potionEffects != null) {
                                    for (PotionEntry potionEntry : bulletProperty.potionEffects) {
                                        targetELB.addPotionEffect(new PotionEffect(potionEntry.potionEffect.getPotion(), potionEntry.duration, potionEntry.level));
                                    }
                                }
                            }
                        }
                    }
                }

                if (bulletHit instanceof PlayerHit && ((PlayerHit)bulletHit).hitbox.type.equals(EnumHitboxType.BODY) && targetEntity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer)targetEntity;
                    if (player.hasCapability(CapabilityExtra.CAPABILITY, null)) {
                        final IExtraItemHandler extraSlots = player.getCapability(CapabilityExtra.CAPABILITY, null);
                        if (extraSlots != null) {
                            final ItemStack plate = extraSlots.getStackInSlot(1);
                            if (plate != null && plate.getItem() instanceof ItemSpecialArmor) {
                                ArmorType armorType = ((ItemSpecialArmor)plate.getItem()).type;
                                float damage = preHitEvent.getDamage();
                                preHitEvent.setDamage((float)(damage - (damage * armorType.defense)));
                            }
                        }
                    }
                }

                if (!ModConfig.INSTANCE.shots.knockback_entity_damage) {
                    RayUtil.attackEntityWithoutKnockback(targetEntity, DamageSource.causePlayerDamage(entityPlayer).setProjectile(), preHitEvent.getDamage());
                } else {
                    targetEntity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer).setProjectile(), preHitEvent.getDamage());
                }
                targetEntity.hurtResistantTime = 0;

                // Weapon pre hit event
                WeaponHitEvent.Post postHitEvent = new WeaponHitEvent.Post(entityPlayer, gunStack, itemGun, postFireEvent.getHits(), preHitEvent.getDamage());
                MinecraftForge.EVENT_BUS.post(postHitEvent);
            }
        }
    }

    @Deprecated
    public static void fireServer(EntityPlayer entityPlayer, float rotationPitch, float rotationYaw, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, final int clientFireTickDelay, final float recoilPitch, final float recoilYaw, final float recoilAimReducer, final float bulletSpread) {
        GunType gunType = itemGun.type;
        boolean verifShot = verifShot(entityPlayer, rotationPitch, rotationYaw, world, gunStack, itemGun, fireMode, clientFireTickDelay, recoilPitch, recoilYaw, recoilAimReducer, bulletSpread);
        if (verifShot) {
            return;
        }
        WeaponFireEvent.PreServer preFireEvent = new WeaponFireEvent.PreServer(entityPlayer, gunStack, itemGun, gunType.weaponMaxRange);
        boolean isDeny = preFireEvent.getResult() == Result.DENY;
        if (MinecraftForge.EVENT_BUS.post(preFireEvent) || isDeny) {
            return;
        }
        doGunSound(gunType, gunStack, entityPlayer);
        switch (gunType.weaponType) {
            case Launcher: {
                ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
                final float accuracy = RayUtil.calculateAccuracy(itemGun, entityPlayer);
                EntityExplosiveProjectile projectile = new EntityExplosiveProjectile(world, entityPlayer, bulletItem.type.impactDamage, accuracy, bulletItem.type.projectileVelocity, bulletItem.type.internalName, bulletItem.type.gravity, bulletItem.type.isSmoke, bulletItem.type.isExplosion);
                world.spawnEntity(projectile);
                break;
            }
            case Thrower: {
                ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
                final float accuracy = RayUtil.calculateAccuracy(itemGun, entityPlayer);
                EntityThrowerProjectile projectile = new EntityThrowerProjectile(world, entityPlayer, bulletItem.type.impactDamage, accuracy, bulletItem.type.projectileVelocity, bulletItem.type.internalName, bulletItem.type.gravity, bulletItem.type.isSmoke);
                world.spawnEntity(projectile);
                break;
            }
            default: {
                handleFireRayGun(entityPlayer, rotationPitch, rotationYaw, world, gunStack, itemGun, fireMode, clientFireTickDelay, recoilPitch, recoilYaw, recoilAimReducer, bulletSpread, preFireEvent.getWeaponRange());
                break;
            }
        }

        // Burst Stuff
        int shotCount = computeShotCount(gunType, gunStack, fireMode, entityPlayer);
        if (fireMode == WeaponFireMode.BURST) {
            shotCount = shotCount - 1;
            gunStack.getTagCompound().setInteger("shotsremaining", shotCount);
        }
        ItemGun.consumeShot(gunStack);
        
        // Hands upwards when shooting
        if (ServerTickHandler.playerAimShootCooldown.get(entityPlayer.getUniqueID()) == null) {
            ModularWarfare.NETWORK.sendToAll(new PacketAimingResponse(entityPlayer.getUniqueID(), true));
        }
        ServerTickHandler.playerAimShootCooldown.put(entityPlayer.getUniqueID(), 60);
        
        WeaponFireEvent.Post postFireEvent = new WeaponFireEvent.Post(entityPlayer, gunStack, itemGun, rayTraceList);
        MinecraftForge.EVENT_BUS.post(postFireEvent);
    }

    public static class AimingData {
        public float pitch;
        public float yaw;
        public List<BulletHit> rayTraceList = new ArrayList<>();
        public long lastUpdateTime;
        private static boolean showBulletTrajectory = true; // 是否显示弹道-调试用
        
        public void update(EntityPlayer player, ItemGun itemGun) {
            // 添加服务端检查，如果是服务端则直接返回
            if (player.world != null && !player.world.isRemote) {
                return;
            }
            
            if(System.currentTimeMillis() - lastUpdateTime < 50) {
                if(OBBPlayerManager.debug && showBulletTrajectory && player.world.isRemote) {
                    renderDebugLine(player, itemGun);
                }
                return;
            }
            updateForced(player, itemGun);
        }

        private void renderDebugLine(EntityPlayer player, ItemGun itemGun) {
            if(!player.world.isRemote) return;

            if(!showBulletTrajectory) return;
            
            List<OBBDebugObject> lines = new ArrayList<>();
            Vec3d origin = player.getPositionEyes(ClientProxy.renderHooks.partialTicks);
            
            // 渲染所有命中点的射线
            for(BulletHit hit : rayTraceList) {
                if(hit.rayTraceResult != null && hit.rayTraceResult.hitVec != null) {
                    addRayRender(lines, origin, hit.rayTraceResult.hitVec, (float)hit.distance);
                    
                    // 添加命中点标记
                    lines.add(new OBBDebugObject(new Vector3f(hit.rayTraceResult.hitVec)));
                    
                    // 如果是OBB命中,显示命中的OBB
                    if(hit instanceof OBBHit) {
                        lines.add(new OBBDebugObject(((OBBHit)hit).box));
                    }
                }
            }


            if(rayTraceList.isEmpty()) {
                Vec3d forward = RayUtil.getGunAccuracy(pitch, yaw, 0, player.world.rand, player);
                Vec3d endVec = origin.add(forward.scale(itemGun.type.weaponMaxRange));
                addRayRender(lines, origin, endVec, (float)itemGun.type.weaponMaxRange);
            }

            // 更新渲染列表
            OBBPlayerManager.lines.clear();
            OBBPlayerManager.lines.addAll(lines);
        }

        private void addRayRender(List<OBBDebugObject> lines, Vec3d origin, Vec3d end, float distance) {
            Vector3f rayVec = new Vector3f((float)(end.x - origin.x), (float)(end.y - origin.y), (float)(end.z - origin.z));
            Vector3f normaliseVec = rayVec.normalise(null);
            

            OBBModelBox ray = new OBBModelBox();
            float pitchRad = (float) Math.asin(normaliseVec.y);
            normaliseVec.y = 0;
            normaliseVec = normaliseVec.normalise(null);
            float yawRad = (float)Math.asin(normaliseVec.x);
            if(normaliseVec.z < 0) {
                yawRad = (float) (Math.PI-yawRad);
            }
            
            Matrix4f matrix = new Matrix4f();
            matrix.rotate(yawRad, new Vector3f(0, 1, 0));
            matrix.rotate(pitchRad, new Vector3f(-1, 0, 0));
            
            ray.center = new Vector3f((float)(origin.x + end.x) * 0.5f, (float)(origin.y + end.y) * 0.5f, (float)(origin.z + end.z) * 0.5f);
            ray.axis.x = new Vector3f(0, 0, 0);
            ray.axis.y = new Vector3f(0, 0, 0);
            ray.axis.z = Matrix4f.transform(matrix, new Vector3f(0, 0, distance/2), null);
            ray.axisNormal.x = Matrix4f.transform(matrix, new Vector3f(1, 0, 0), null);
            ray.axisNormal.y = Matrix4f.transform(matrix, new Vector3f(0, 1, 0), null);
            ray.axisNormal.z = Matrix4f.transform(matrix, new Vector3f(0, 0, 1), null);

            lines.add(new OBBDebugObject(ray));
            lines.add(new OBBDebugObject(new Vector3f((float)origin.x, (float)origin.y, (float)origin.z), 
                                       new Vector3f((float)end.x, (float)end.y, (float)end.z)));
        }

        private void updateForced(EntityPlayer player, ItemGun itemGun) {
            lastUpdateTime = System.currentTimeMillis();
            if(player.world.isRemote) {

                ItemStack heldItem = player.getHeldItemMainhand();
                if(heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemGun)) {
                    rayTraceList.clear();
                    return;
                }
                
                Minecraft mc = Minecraft.getMinecraft();
                Entity entity = mc.getRenderViewEntity();
                pitch = player.prevRotationPitch + (player.rotationPitch-player.prevRotationPitch) * ClientProxy.renderHooks.partialTicks;
                yaw = player.prevRotationYaw + (player.rotationYaw-player.prevRotationYaw) * ClientProxy.renderHooks.partialTicks;
                
                if(ClientProxy.shoulderSurfingLoaded) {
                    if(ShoulderInstance.getInstance().doShoulderSurfing()) {
                        Vec3d eye = entity.getPositionEyes(ClientProxy.renderHooks.partialTicks);
                        double posX = eye.x;
                        double posY = eye.y;
                        double posZ = eye.z;
                        RayTraceResult r = getMouseOver(ClientProxy.renderHooks.partialTicks);
                        posX = r.hitVec.x-posX;
                        posY = r.hitVec.y-posY;
                        posZ = r.hitVec.z-posZ;
                        pitch = (float)-Math.toDegrees(Math.atan(posY/Math.sqrt(posX*posX+posZ*posZ)));
                        yaw = (float)Math.toDegrees(Math.acos((posX*0+posZ*1)/Math.sqrt(posX*posX+posZ*posZ)));
                        if(posX>0) {
                            yaw = -yaw;
                        }
                    }
                }
                
                int numBullets = itemGun.type.numBullets;
                ItemBullet bulletItem = ItemGun.getUsedBullet(heldItem, itemGun.type);
                boolean isSlug = false;
                if (bulletItem != null && bulletItem.type.isSlug) {
                    isSlug = true;
                }
                
                rayTraceList.clear();
                if (isSlug || numBullets <= 1) {
                    List<BulletHit> rayTrace = RayUtil.standardEntityRayTrace(Side.CLIENT, player.world, pitch, yaw, player, itemGun.type.weaponMaxRange, itemGun, false);
                    if(rayTrace != null) {
                        rayTraceList.addAll(rayTrace);
                    }
                } else {
                    for (int i = 0; i < numBullets; i++) {
                        List<BulletHit> rayTrace = RayUtil.standardEntityRayTrace(Side.CLIENT, player.world, pitch, yaw, player, itemGun.type.weaponMaxRange, itemGun, false);
                        if(rayTrace != null) {
                            rayTraceList.addAll(rayTrace);
                        }
                    }
                }

                // 添加调试渲染
                if(OBBPlayerManager.debug) {
                    renderDebugLine(player, itemGun);
                }
            }
        }
    }

    private static Map<UUID, AimingData> playerAimingData = new HashMap<>();

    public static AimingData getAimingData(EntityPlayer player) {
        UUID id = player.getUniqueID();
        if(!playerAimingData.containsKey(id)) {
            playerAimingData.put(id, new AimingData());
        }
        return playerAimingData.get(id);
    }

    public static void fireClientSide(EntityPlayer entityPlayer, ItemGun itemGun) {
        if (entityPlayer.world.isRemote) {
            // 重置子弹索引，确保每次射击都从第一颗子弹开始
            RayUtil.resetBulletIndex();
            
            AimingData aimData = getAimingData(entityPlayer);
            // 强制更新瞄准数据,确保使用最新的数据
            aimData.updateForced(entityPlayer, itemGun);
            
            Vec3d origin = entityPlayer.getPositionEyes(ClientProxy.renderHooks.partialTicks);
            Vec3d endVec = null;
            
            // 获取最近的命中点作为尾迹终点
            if(!aimData.rayTraceList.isEmpty()) {
                BulletHit firstHit = aimData.rayTraceList.get(0);
                if(firstHit.rayTraceResult != null && firstHit.rayTraceResult.hitVec != null) {
                    endVec = firstHit.rayTraceResult.hitVec;
                }
            }
            
            // 如果没有命中点，使用最大射程
            if(endVec == null) {
                float accuracy = RayUtil.calculateAccuracy(itemGun, entityPlayer);
                Vec3d forward = RayUtil.getGunAccuracy(aimData.pitch, aimData.yaw, accuracy, entityPlayer.world.rand, entityPlayer);
                endVec = origin.add(forward.scale(itemGun.type.weaponMaxRange));
            }
            
            // 发送尾迹渲染请求
            Vec3d direction = endVec.subtract(origin).normalize();

            // 获取子弹配置
            String model = null;
            String tex = null;
            boolean glow = false;

            ItemStack gunStack = entityPlayer.getHeldItemMainhand();
            if (!gunStack.isEmpty() && gunStack.hasTagCompound()) {
                ItemStack bulletStack = null;
                if (itemGun.type.acceptedBullets != null) {
                    if (gunStack.getTagCompound().hasKey("bullet")) {
                        bulletStack = new ItemStack(gunStack.getTagCompound().getCompoundTag("bullet"));
                    }
                } else {
                    if (gunStack.getTagCompound().hasKey("ammo")) {
                        ItemStack stackAmmo = new ItemStack(gunStack.getTagCompound().getCompoundTag("ammo"));
                        if(stackAmmo != null && !stackAmmo.isEmpty() && stackAmmo.hasTagCompound() && stackAmmo.getTagCompound().hasKey("bullet")) {
                            bulletStack = new ItemStack(stackAmmo.getTagCompound().getCompoundTag("bullet"));  
                        }
                    }
                }

                if (bulletStack != null && !bulletStack.isEmpty() && bulletStack.getItem() instanceof ItemBullet) {
                    BulletType bulletType = ((ItemBullet)bulletStack.getItem()).type;
                    if (bulletType != null) {
                        model = bulletType.trailModel;
                        tex = bulletType.trailTex;
                        glow = bulletType.trailGlow;
                    }
                }
            }

            // 如果子弹没有配置尾迹，使用枪械配置
            if(model == null) model = itemGun.type.customTrailModel;
            if(tex == null) tex = itemGun.type.customTrailTexture;
            if(!glow) glow = itemGun.type.customTrailGlow;

            // 判断是否使用特斯拉效果
            if(itemGun.type.useTeslaTrails) {
                // 添加特斯拉效果
                ModularWarfare.NETWORK.sendToServer(new PacketTeslaTrailAskServer(
                    origin.x, origin.y, origin.z,
                    endVec.x, endVec.y, endVec.z,
                    10f,
                    itemGun.type
                ));
            } else {
                // 使用普通尾迹效果
                ModularWarfare.NETWORK.sendToServer(new PacketGunTrailAskServer(
                    itemGun.type,
                    model,
                    tex,
                    glow,
                    origin.x, origin.y, origin.z,
                    entityPlayer.motionX, entityPlayer.motionZ,
                    direction.x, direction.y, direction.z,
                    origin.distanceTo(endVec),
                    10,
                    false
                ));
            }

            ModularWarfare.NETWORK.sendToServer(new PacketExpShot(entityPlayer.getEntityId(), itemGun.type.internalName));

            boolean headshot = false;
            for (BulletHit rayTrace : aimData.rayTraceList) {
                if (rayTrace instanceof OBBHit) {
                    final EntityLivingBase victim = ((OBBHit) rayTrace).entity;
                    if (victim != null && !victim.isDead && victim.getHealth() > 0.0f) {
                        ModularWarfare.NETWORK.sendToServer(new PacketExpGunFire(victim.getEntityId(), itemGun.type.internalName, ((OBBHit) rayTrace).box.name, itemGun.type.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw, itemGun.type.recoilAimReducer, itemGun.type.bulletSpread, rayTrace.remainingPenetrate, rayTrace.remainingBlockPenetrate, rayTrace.distance, rayTrace.rayTraceResult.hitVec.x, rayTrace.rayTraceResult.hitVec.y, rayTrace.rayTraceResult.hitVec.z));
                    }
                } else {
                    if (rayTrace.rayTraceResult != null && rayTrace.rayTraceResult.hitVec != null) {
                        if(rayTrace.rayTraceResult.entityHit != null) {
                            headshot = ItemGun.canEntityGetHeadshot(rayTrace.rayTraceResult.entityHit) && rayTrace.rayTraceResult.hitVec.y >= rayTrace.rayTraceResult.entityHit.getPosition().getY() + rayTrace.rayTraceResult.entityHit.getEyeHeight() - 0.15f;
                            ModularWarfare.NETWORK.sendToServer(new PacketExpGunFire(rayTrace.rayTraceResult.entityHit.getEntityId(), itemGun.type.internalName, (headshot? "head":""), itemGun.type.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw, itemGun.type.recoilAimReducer, itemGun.type.bulletSpread, rayTrace.remainingPenetrate, rayTrace.remainingBlockPenetrate, rayTrace.distance, rayTrace.rayTraceResult.hitVec.x, rayTrace.rayTraceResult.hitVec.y, rayTrace.rayTraceResult.hitVec.z));
                        } else {
                            ModularWarfare.NETWORK.sendToServer(new PacketExpGunFire(-1, itemGun.type.internalName, "", itemGun.type.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw, itemGun.type.recoilAimReducer, itemGun.type.bulletSpread, rayTrace.remainingPenetrate, rayTrace.remainingBlockPenetrate, rayTrace.distance, rayTrace.rayTraceResult.hitVec.x, rayTrace.rayTraceResult.hitVec.y, rayTrace.rayTraceResult.hitVec.z,rayTrace.rayTraceResult.sideHit));
                        }
                    }
                }
            }
        }
    }

    public static RayTraceResult getMouseOver(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity entity = mc.getRenderViewEntity();
        RayTraceResult objectMouseOver = null;
        if (entity != null)
          if (mc.world != null) {
            objectMouseOver = entity.rayTrace(128.0D, partialTicks);
            Vec3d vec3d = ShoulderHelper.shoulderSurfingLook(entity, partialTicks, 128).cameraPos();
            double d1 = 128.0D;
            if (objectMouseOver != null)
              d1 = objectMouseOver.hitVec.distanceTo(vec3d);
            Vec3d vec3d1 = entity.getLook(1.0F);
            Vec3d vec3d2 = vec3d.add(vec3d1.x * d1, vec3d1.y * d1, vec3d1.z * d1);
            Entity pointedEntity = null;
            Vec3d vec3d3 = null;
            float f = 1.0F;
            List<Entity> list = mc.world.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(vec3d1.x * d1, vec3d1.y * d1, vec3d1.z * d1).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, new Predicate<Entity>() {
                    public boolean apply(@Nullable Entity p_apply_1_) {
                      return (p_apply_1_ != null && p_apply_1_.canBeCollidedWith());
                    }
                  }));
            double d2 = d1;
            for (int j = 0; j < list.size(); j++) {
              Entity entity1 = list.get(j);
              AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
              RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);
              if (axisalignedbb.contains(vec3d)) {
                if (d2 >= 0.0D) {
                  pointedEntity = entity1;
                  vec3d3 = (raytraceresult == null) ? vec3d : raytraceresult.hitVec;
                  d2 = 0.0D;
                }
              } else if (raytraceresult != null) {
                double d3 = vec3d.distanceTo(raytraceresult.hitVec);
                if (d3 < d2 || d2 == 0.0D)
                  if (entity1.getLowestRidingEntity() == entity.getLowestRidingEntity() && !entity1.canRiderInteract()) {
                    if (d2 == 0.0D) {
                      pointedEntity = entity1;
                      vec3d3 = raytraceresult.hitVec;
                    }
                  } else {
                    pointedEntity = entity1;
                    vec3d3 = raytraceresult.hitVec;
                    d2 = d3;
                  }
              }
            }
            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null))
              objectMouseOver = new RayTraceResult(pointedEntity, vec3d3);
          }
        return objectMouseOver;
      }
      
    /**
     * 为实体设计的服务端射击方法
     * 移除了玩家特有的验证，保留武器射击类型和伤害判定
     * 
     * @param entity 射击实体
     * @param rotationPitch 俯仰角度
     * @param rotationYaw 偏航角度
     * @param world 世界
     * @param gunStack 武器堆栈
     * @param itemGun 武器对象
     * @param fireMode 射击模式
     * @param clientFireTickDelay 客户端射击延迟
     * @param recoilPitch 后坐力俯仰
     * @param recoilYaw 后坐力偏航
     * @param recoilAimReducer 瞄准后坐力减少
     * @param bulletSpread 子弹散射
     * @param useHeldWeapon 是否使用手中武器
     */
    public static boolean fireServerForEntity(EntityLivingBase entity, float rotationPitch, float rotationYaw, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, final int clientFireTickDelay, final float recoilPitch, final float recoilYaw, final float recoilAimReducer, final float bulletSpread, boolean useHeldWeapon, float customDamage, float customHeadshotBonus) {
        GunType gunType = itemGun.type;
        
        if (!validateEntityShot(entity, gunStack, itemGun, fireMode, useHeldWeapon)) {
            return false;
        }

        // Weapon pre fire event
        WeaponFireEvent.PreServer preFireEvent = new WeaponFireEvent.PreServer(entity, gunStack, itemGun, gunType.weaponMaxRange);
        MinecraftForge.EVENT_BUS.post(preFireEvent);
        if (preFireEvent.isCanceled())
            return false;
            
        int shotCount = fireMode == WeaponFireMode.BURST ? gunStack.getTagCompound().getInteger("shotsremaining") > 0 ? gunStack.getTagCompound().getInteger("shotsremaining") : gunType.numBurstRounds : 1;

        if (preFireEvent.getResult() == Event.Result.DEFAULT || preFireEvent.getResult() == Event.Result.ALLOW) {
            if (useHeldWeapon) {
                if (!ItemGun.hasNextShot(gunStack)) {
                    if (ItemGun.canDryFire) {
                        gunType.playSound(entity, WeaponSoundType.DryFire, gunStack);
                        ItemGun.canDryFire = false;
                    }
                    if (fireMode == WeaponFireMode.BURST) gunStack.getTagCompound().setInteger("shotsremaining", 0);
                    return false;
                }
            }
        }

        // Sound
        if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Barrel) != null) {
            gunType.playSound(entity, WeaponSoundType.FireSuppressed, gunStack, entity instanceof EntityPlayer ? (EntityPlayer) entity : null);
        } else if (GunType.isPackAPunched(gunStack)) {
            gunType.playSound(entity, WeaponSoundType.Punched, gunStack, entity instanceof EntityPlayer ? (EntityPlayer) entity : null);
            gunType.playSound(entity, WeaponSoundType.Fire, gunStack, entity instanceof EntityPlayer ? (EntityPlayer) entity : null);
        } else {
            gunType.playSound(entity, WeaponSoundType.Fire, gunStack, entity instanceof EntityPlayer ? (EntityPlayer) entity : null);
        }
        
        int numBullets = gunType.numBullets;
        ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
        if (bulletItem == null) {
            return false;
        }
        
        if (bulletItem.type.isSlug) {
            numBullets = 1;
        }

        if(gunType.weaponType != WeaponType.Launcher && gunType.weaponType != WeaponType.Thrower) {
            List<BulletHit> rayTraceList = new ArrayList<>();
            
            for (int i = 0; i < numBullets; i++) {
                List<BulletHit> rayTrace = RayUtil.standardEntityRayTraceForEntity(Side.SERVER, world, rotationPitch, rotationYaw, entity, preFireEvent.getWeaponRange(), itemGun, GunType.isPackAPunched(gunStack), gunStack);
                if (rayTrace == null) {
                    continue;
                }
                rayTraceList.addAll(rayTrace);
            }

            Vec3d origin = entity.getPositionEyes(1.0f);
            Vec3d endVec = null;
            
            if (!rayTraceList.isEmpty()) {
                BulletHit firstHit = rayTraceList.get(0);
                if (firstHit.rayTraceResult != null && firstHit.rayTraceResult.hitVec != null) {
                    endVec = firstHit.rayTraceResult.hitVec;
                }
            }
            
            if (endVec == null) {
                float accuracy = EntityShootingAPI.calculateServerAccuracy(itemGun, entity);
                Vec3d forward = EntityShootingAPI.getServerDefaultAccuracy(rotationPitch, rotationYaw, accuracy, world.rand);
                endVec = origin.add(forward.scale(gunType.weaponMaxRange));
            }
            
            Vec3d direction = endVec.subtract(origin).normalize();
            
            String model = gunType.customTrailModel;
            String tex = gunType.customTrailTexture;
            boolean glow = gunType.customTrailGlow;
            
            if (model == null) model = "";
            if (tex == null) tex = "";
            
            if (gunType.useTeslaTrails) {
                ModularWarfare.NETWORK.sendToDimension(new PacketTeslaTrail(
                    origin.x, origin.y, origin.z,
                    endVec.x, endVec.y, endVec.z,
                    10f,
                    gunType.internalName
                ), entity.dimension);
            } else {
                ModularWarfare.NETWORK.sendToDimension(new PacketGunTrail(
                    gunType.internalName, model, tex, glow,
                    origin.x, origin.y, origin.z,
                    entity.motionX, entity.motionZ,
                    direction.x, direction.y, direction.z,
                    origin.distanceTo(endVec),
                    10,
                    GunType.isPackAPunched(gunStack)
                ), entity.dimension);
            }

            boolean headshot = false;
            Iterator<BulletHit> rayTraceIterator = rayTraceList.iterator();
            while (rayTraceIterator.hasNext() && !world.isRemote) {
                BulletHit rayTrace = rayTraceIterator.next();
                if (rayTrace instanceof PlayerHit) {
                    final EntityPlayer victim = ((PlayerHit) rayTrace).getEntity();
                    if (victim == null || victim.isDead || victim.getHealth() <= 0.f) {
                        rayTraceIterator.remove();
                        continue;
                    }
                    gunType.playSoundPos(victim.getPosition(), world, WeaponSoundType.Penetration);
                    headshot = ((PlayerHit) rayTrace).hitbox.type.equals(EnumHitboxType.HEAD);
                    if (entity instanceof EntityPlayerMP) {
                        ModularWarfare.NETWORK.sendTo(new PacketPlayHitmarker(headshot), (EntityPlayerMP) entity);
                        ModularWarfare.NETWORK.sendTo(new PacketPlaySound(victim.getPosition(), "flyby", 1f, 1f), (EntityPlayerMP) victim);
                        if (ModConfig.INSTANCE.hud.snap_fade_hit) {
                            ModularWarfare.NETWORK.sendTo(new PacketPlayerHit(), (EntityPlayerMP) victim);
                        }
                    }
                    continue;
                }
                
                // 检查是否为方块命中，这里是特殊处理
                if (rayTrace.rayTraceResult != null && rayTrace.rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
                    BlockPos blockPos = rayTrace.rayTraceResult.getBlockPos();
                    ItemGun.playImpactSound(world, rayTrace.rayTraceResult, gunType);
                    gunType.playSoundPos(blockPos, world, WeaponSoundType.Crack, entity instanceof EntityPlayer ? (EntityPlayer) entity : null, 1.0f, false);
                    if (entity instanceof EntityPlayer) {
                        ItemGun.doHit(rayTrace.rayTraceResult, (EntityPlayer) entity);
                    } else {
                        ItemGun.doHit(rayTrace.rayTraceResult, null);
                    }
                    ItemGun.playHitEffect(world, rayTrace.rayTraceResult);
                    continue;
                }
                
                Entity targetEnt = rayTrace.getEntity();
                if (targetEnt == null) {
                    rayTraceIterator.remove();
                    continue;
                }
                if (targetEnt instanceof EntityGrenade) {
                    ((EntityGrenade) targetEnt).explode();
                    continue;
                }
                if (targetEnt instanceof EntityLivingBase) {
                    final EntityLivingBase victim = (EntityLivingBase) targetEnt;
                    gunType.playSoundPos(victim.getPosition(), world, WeaponSoundType.Penetration);
                    headshot = ItemGun.canEntityGetHeadshot(victim) && rayTrace.rayTraceResult.hitVec.y >= victim.getPosition().getY() + victim.getEyeHeight() - 0.15f;
                    if (entity instanceof EntityPlayerMP) {
                        ModularWarfare.NETWORK.sendTo(new PacketPlayHitmarker(headshot), (EntityPlayerMP) entity);
                    }
                    continue;
                }
            }

            // Weapon post fire event
            WeaponFireEvent.Post postFireEvent = new WeaponFireEvent.Post(entity instanceof EntityPlayer ? (EntityPlayer) entity : null, gunStack, itemGun, rayTraceList, customDamage);
            MinecraftForge.EVENT_BUS.post(postFireEvent);

            if (postFireEvent.getHits() != null && !postFireEvent.getHits().isEmpty()) {
                List<BulletHit> hits = postFireEvent.getHits();
                for (BulletHit bulletHit : hits) {
                    if (bulletHit == null) {
                        continue;
                    }
                    Entity targetEntity = bulletHit.getEntity();
                    if (targetEntity == null || targetEntity == entity) {
                        continue;
                    }

                    // 获取碰撞箱名称
                    String hitboxName = "";
                    if (bulletHit instanceof PlayerHit) {
                        PlayerHit playerHit = (PlayerHit) bulletHit;
                        hitboxName = playerHit.hitbox.type.name();
                    } else if (bulletHit instanceof OBBHit) {
                        OBBHit obbHit = (OBBHit) bulletHit;
                        hitboxName = obbHit.box.name;
                    }

                    // Weapon pre hit event
                    WeaponHitEvent.Pre preHitEvent = new WeaponHitEvent.Pre((EntityLivingBase)preFireEvent.getWeaponUser(), gunStack, itemGun, headshot, postFireEvent.getDamage(), bulletHit.remainingPenetrate, bulletHit.remainingBlockPenetrate, targetEntity, bulletHit.distance, hitboxName);
                    MinecraftForge.EVENT_BUS.post(preHitEvent);
                    if (preHitEvent.isCanceled()) {
                        return false;
                    }

                    if (headshot) {
                        float headshotBonus = customHeadshotBonus >= 0 ? customHeadshotBonus : gunType.gunDamageHeadshotBonus;
                        preHitEvent.setDamage(preHitEvent.getDamage() + headshotBonus);
                    }
                    if (gunType.gunPenetrationDamageFalloff && preHitEvent.getPenetrateDamageFactor() > 0) {
                        preHitEvent.setDamage(preHitEvent.getDamage() * preHitEvent.getPenetrateDamageFactor());
                    }
                    if (gunType.gunPenetrateBlocksDamageFalloffFactor > 0 && preHitEvent.getPenetrateBlockDamageFactor() > 0 && preHitEvent.getPenetrateBlockDamageFactor() < 1) {
                        preHitEvent.setDamage(preHitEvent.getDamage() * preHitEvent.getPenetrateBlockDamageFactor() * gunType.gunPenetrateBlocksDamageFalloffFactor);
                    }
                    if (preHitEvent.getDistance() > gunType.weaponEffectiveRange) {
                        preHitEvent.setDamage((float) (preHitEvent.getDamage() * (1 - (preHitEvent.getDistance() - gunType.weaponEffectiveRange) / (gunType.weaponMaxRange - gunType.weaponEffectiveRange))));
                    }  else if (preHitEvent.getDistance() >= gunType.weaponMaxRange) {
                        preHitEvent.setDamage((float) (preHitEvent.getDamage() * 0));
                    }

                    if (targetEntity instanceof EntityLivingBase) {
                        EntityLivingBase targetELB = (EntityLivingBase) targetEntity;
                        if (bulletItem.type != null) {
                            preHitEvent.setDamage(preHitEvent.getDamage() * bulletItem.type.bulletDamageFactor);
                            if (bulletItem.type.bulletProperties != null) {
                                if (!bulletItem.type.bulletProperties.isEmpty()) {
                                    BulletProperty bulletProperty = bulletItem.type.bulletProperties.get(targetELB.getName()) != null ? 
                                        bulletItem.type.bulletProperties.get(targetELB.getName()) : 
                                        bulletItem.type.bulletProperties.get("All");
                                    if (bulletProperty.potionEffects != null) {
                                        for (PotionEntry potionEntry : bulletProperty.potionEffects) {
                                            targetELB.addPotionEffect(new PotionEffect(potionEntry.potionEffect.getPotion(), potionEntry.duration, potionEntry.level));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (bulletHit instanceof PlayerHit && ((PlayerHit) bulletHit).hitbox.type.equals(EnumHitboxType.BODY) && targetEntity instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) targetEntity;
                        if (player.hasCapability(CapabilityExtra.CAPABILITY, null)) {
                            final IExtraItemHandler extraSlots = player.getCapability(CapabilityExtra.CAPABILITY, null);
                            if (extraSlots != null) {
                                final ItemStack plate = extraSlots.getStackInSlot(1);
                                if (plate != null && plate.getItem() instanceof ItemSpecialArmor) {
                                    ArmorType armorType = ((ItemSpecialArmor) plate.getItem()).type;
                                    float damage = preHitEvent.getDamage();
                                    preHitEvent.setDamage((float) (damage - (damage * armorType.defense)));
                                }
                            }
                        }
                    }

                    if (!ModConfig.INSTANCE.shots.knockback_entity_damage) {
                        RayUtil.attackEntityWithoutKnockback(targetEntity, DamageSource.causeMobDamage(preFireEvent.getWeaponUser()).setProjectile(), preHitEvent.getDamage());
                    } else {
                        targetEntity.attackEntityFrom(DamageSource.causeMobDamage(preFireEvent.getWeaponUser()).setProjectile(), preHitEvent.getDamage());
                    }
                    targetEntity.hurtResistantTime = 0;

                    // Weapon pre hit event
                    WeaponHitEvent.Post postHitEvent = new WeaponHitEvent.Post((EntityLivingBase)preFireEvent.getWeaponUser(), gunStack, itemGun, postFireEvent.getHits(), preHitEvent.getDamage());
                    MinecraftForge.EVENT_BUS.post(postHitEvent);
                }
            }
        } else if (gunType.weaponType == WeaponType.Launcher){
            //抛射物玩家参数过多，后续再调整
            if (entity instanceof EntityPlayer) {
                final float accuracy = EntityShootingAPI.calculateServerAccuracy(itemGun, entity);
                EntityExplosiveProjectile projectile = new EntityExplosiveProjectile(world, (EntityPlayer) entity, bulletItem.type.impactDamage, accuracy, bulletItem.type.projectileVelocity, bulletItem.type.internalName, bulletItem.type.gravity, bulletItem.type.isSmoke, bulletItem.type.isExplosion);
                world.spawnEntity(projectile);
            }
        } else if (gunType.weaponType == WeaponType.Thrower){
            //抛射物玩家参数过多，后续再调整
            if (entity instanceof EntityPlayer) {
                final float accuracy = EntityShootingAPI.calculateServerAccuracy(itemGun, entity);
                EntityThrowerProjectile projectile = new EntityThrowerProjectile(world, (EntityPlayer) entity, bulletItem.type.impactDamage, accuracy, bulletItem.type.projectileVelocity, bulletItem.type.internalName, bulletItem.type.gravity, bulletItem.type.isSmoke);
                world.spawnEntity(projectile);
            }
        }

        // Burst Stuff
        if (fireMode == WeaponFireMode.BURST) {
            if (useHeldWeapon) {
                shotCount = shotCount - 1;
                gunStack.getTagCompound().setInteger("shotsremaining", shotCount);
            }
        }

        if (preFireEvent.getResult() == Event.Result.DEFAULT || preFireEvent.getResult() == Event.Result.ALLOW) {
            if (useHeldWeapon) {
                ItemGun.consumeShot(gunStack);
            }
        }

        return true;
    }
    
    /**
     * 验证实体射击的有效性
     */
    private static boolean validateEntityShot(EntityLivingBase entity, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, boolean useHeldWeapon) {
        GunType gunType = itemGun.type;
        
        if (entity == null || entity.isDead || entity.getHealth() <= 0) {
            return false;
        }
        
        if (gunStack == null || gunStack.isEmpty() || !(gunStack.getItem() instanceof ItemGun)) {
            return false;
        }
        
        if (!gunType.hasFireMode(fireMode)) {
            return false;
        }
        
        if (useHeldWeapon) {
            if (!ItemGun.hasNextShot(gunStack)) {
                return false;
            }
        }
        
        return true;
    }
}
