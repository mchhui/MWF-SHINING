package com.modularwarfare.common.guns.manager;

import com.google.common.util.concurrent.AtomicDouble;
import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.EntityHeadShotEvent;
import com.modularwarfare.api.WeaponFireEvent;
import com.modularwarfare.api.WeaponRayHitEntityEvent;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
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
import com.modularwarfare.common.network.*;
import com.modularwarfare.common.network.PacketOtherShooterAnimation.AnimationType;
import com.modularwarfare.common.playerstate.PlayerState;
import com.modularwarfare.common.playerstate.PlayerStateManager;
import com.modularwarfare.utility.RayUtil;
import com.modularwarfare.utility.raycast.DefaultRayCasting;
import com.modularwarfare.utility.raycast.hits.BulletHit;
import com.modularwarfare.utility.raycast.hits.OBBHit;

import mchhui.modularmovements.tactical.client.ClientListener;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.relauncher.Side;

import com.modularwarfare.client.model.InstantBulletTeslaRender;

import javax.annotation.Nullable;
import javax.management.RuntimeErrorException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collections;

public class FireManager {

    public static boolean fire(EntityLivingBase shooter, FireData fireData) {
        WeaponFireEvent.Pre pre = new WeaponFireEvent.Pre(shooter, fireData);
        if (MinecraftForge.EVENT_BUS.post(pre)) {
            return false;
        }
        try {
            if (!shooter.world.isRemote) {
                return FireServerPort.fireServer(shooter, fireData);
            } else {
                if (shooter instanceof EntityPlayer) {
                    return FireClientPort.fireClient((EntityPlayer)shooter, fireData);
                } else {
                    throw new RuntimeException("[MWF] FireClientPort.fireClient: shooter is not an EntityPlayer");
                }
            }
        } finally {
            WeaponFireEvent.Post post = new WeaponFireEvent.Post(shooter, fireData);
            MinecraftForge.EVENT_BUS.post(post);
        }
    }

    public static class FireData {
        public final float rotationPitch;
        public final float rotationYaw;
        public final World world;
        public final ItemStack gunStack;
        public final ItemGun itemGun;
        public final GunType gunType;
        public final WeaponFireMode fireMode;
        public final boolean useHeldWeapon;
        public final List<PacketGunFire.Hit> clientSuggetions;

        public float baseDamage;
        public float headshotBonus;
        public double weaponRange;

        public FireData(float rotationPitch, float rotationYaw, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, boolean useHeldWeapon, Float customDamage, Float customHeadshotBonus, List<PacketGunFire.Hit> clientSuggetions) {
            super();
            this.rotationPitch = rotationPitch;
            this.rotationYaw = rotationYaw;
            this.world = world;
            this.gunStack = gunStack;
            this.itemGun = itemGun;
            this.gunType = itemGun.type;
            this.fireMode = fireMode;
            this.useHeldWeapon = useHeldWeapon;
            this.baseDamage = Optional.ofNullable(customDamage).orElse(this.gunType.gunDamage);
            this.headshotBonus = Optional.ofNullable(customHeadshotBonus).orElse(this.gunType.gunDamageHeadshotBonus);
            this.weaponRange = this.gunType.weaponMaxRange;
            this.clientSuggetions = clientSuggetions;
        }

        public static FireData buildClient(ItemGun itemGun, ItemStack gunStack, GunType gunType, WeaponFireMode fireMode, World world) {
            return new FireData(0, 0, world, gunStack, itemGun, fireMode, true, null, null, null);
        }

        public static FireData buildServer(float rotationPitch, float rotationYaw, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, boolean useHeldWeapon, Float customDamage, Float customHeadshotBonus) {
            return new FireData(rotationPitch, rotationYaw, world, gunStack, itemGun, fireMode, useHeldWeapon, customDamage, customHeadshotBonus, null);
        }

        public static FireData buildServer(float rotationPitch, float rotationYaw, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, List<PacketGunFire.Hit> clientSuggetions) {
            return new FireData(rotationPitch, rotationYaw, world, gunStack, itemGun, fireMode, true, null, null, clientSuggetions);
        }
    }

    public static class FireServerPort {
        private static void rayHitOnLivingServer(BulletHit bulletHit, ItemBullet bulletItem, EntityLivingBase shooter, EntityLivingBase victim, FireData fireData) {
            ItemGun itemGun = fireData.itemGun;
            ItemStack gunStack = fireData.gunStack;
            GunType gunType = fireData.gunType;
            WeaponFireMode fireMode = fireData.fireMode;
            boolean useHeldWeapon = fireData.useHeldWeapon;
            float baseDamage = fireData.baseDamage;
            float headHostBonus = fireData.headshotBonus;
            if (victim == null || victim.isDead || victim.getHealth() <= 0) {
                return;
            }
            // itself can't been shot
            if (victim == shooter) {
                return;
            }
            String hitboxName = getHitBoxName(bulletHit);
            if (hitboxName == null) hitboxName = "";

            // calc damage amount
            boolean headshot = false;
            if (!hitboxName.isEmpty()) {
                if (hitboxName.contains("head")) {
                    headshot = true;
                }
            } else {
                headshot = ItemGun.canEntityGetHeadshot(victim) && bulletHit.rayTraceResult.hitVec.y >= victim.getPosition().getY() + victim.getEyeHeight() - 0.15f;
            }
            double penetrateDamageFactor = bulletHit.remainingPenetrate;
            double penetrateBlockDamageFactor = bulletHit.remainingBlockPenetrate;
            double distance = bulletHit.distance;
            AtomicDouble amount = new AtomicDouble(baseDamage);
            if (headshot) {
                amount.set(amount.get() + headHostBonus);

            }
            if (gunType.gunPenetrationDamageFalloff && penetrateDamageFactor > 0) {
                amount.set(amount.get() * penetrateDamageFactor);
            }
            if (gunType.gunPenetrateBlocksDamageFalloffFactor > 0 && penetrateBlockDamageFactor > 0 && penetrateBlockDamageFactor < 1) {
                amount.set(amount.get() * penetrateBlockDamageFactor * gunType.gunPenetrateBlocksDamageFalloffFactor);
            }
            if (distance > gunType.weaponEffectiveRange) {
                amount.set((float)(amount.get() * (1 - (distance - gunType.weaponEffectiveRange) / (gunType.weaponMaxRange - gunType.weaponEffectiveRange))));
            } else if (distance >= gunType.weaponMaxRange) {
                amount.set(0);
            }
            if (bulletItem.type != null) {
                amount.set(amount.get() * bulletItem.type.bulletDamageFactor);
            }
            // shooter player's state
            if (shooter instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer)shooter;
                PlayerState state = PlayerStateManager.getPlayerState(player);
                if (itemGun.type.acceptedBullets != null) {
                    amount.set(amount.get() * state.bulletGunDamageAmplifier);
                } else {
                    amount.set(amount.get() * state.ammoGunDamageAmplifier);
                }
            }

            // victim player's plate and state
            ItemStack plate = null;
            IExtraItemHandler extraSlots = null;
            boolean bodyShot = hitboxName.contains("body");
            if (victim instanceof EntityPlayer && bodyShot) {
                EntityPlayer player = (EntityPlayer)victim;
                PlayerState victimState = PlayerStateManager.getPlayerState(player);
                if (player.hasCapability(CapabilityExtra.CAPABILITY, null)) {
                    extraSlots = player.getCapability(CapabilityExtra.CAPABILITY, null);
                    if (extraSlots != null) {
                        plate = extraSlots.getStackInSlot(1);
                        if (plate != null && plate.getItem() instanceof ItemSpecialArmor) {
                            ArmorType armorType = ((ItemSpecialArmor)plate.getItem()).type;
                            double damage = amount.get();
                            amount.set(damage - (damage * armorType.defense * victimState.bulletproofFactor));
                        }
                    }
                }
            }

            // bullet special effect
            if (victim instanceof EntityLivingBase && bulletItem.type.bulletProperties != null && !bulletItem.type.bulletProperties.isEmpty()) {
                EntityLivingBase targetELB = (EntityLivingBase)victim;
                BulletProperty bulletProperty = bulletItem.type.bulletProperties.get(targetELB.getName()) != null ? bulletItem.type.bulletProperties.get(targetELB.getName()) : bulletItem.type.bulletProperties.get("All");
                if (bulletProperty != null) {
                    if (bulletProperty.potionEffects != null) {
                        for (PotionEntry potionEntry : bulletProperty.potionEffects) {
                            targetELB.addPotionEffect(new PotionEffect(potionEntry.potionEffect.getPotion(), potionEntry.duration, potionEntry.level));
                        }
                    }
                    if (bulletProperty.fireLevel > 0) {
                        targetELB.setFire(bulletProperty.fireLevel);
                    }
                    if (bulletProperty.explosionLevel > 0) {
                        targetELB.world.createExplosion(null, targetELB.posX, targetELB.posY + 1, targetELB.posZ, bulletProperty.explosionLevel, bulletProperty.explosionBroken);
                    }
                    if (bulletProperty.knockLevel > 0) {
                        targetELB.knockBack(shooter, bulletProperty.knockLevel, shooter.posX - targetELB.posX, shooter.posZ - targetELB.posZ);
                    }
                    if (bulletProperty.banShield) {
                        if (targetELB instanceof EntityPlayer) {
                            EntityPlayer ep = (EntityPlayer)targetELB;
                            ItemStack itemstack1 = ep.isHandActive() ? ep.getActiveItemStack() : ItemStack.EMPTY;

                            if ((!itemstack1.isEmpty()) && itemstack1.getItem().isShield(itemstack1, ep)) {
                                ep.getCooldownTracker().setCooldown(itemstack1.getItem(), 100);
                                ep.world.setEntityState(ep, (byte)30);
                            }
                        }
                    }
                }
            }

            // todo 需要一个 MWF射线 专用的DamageSource
            DamageSource damageSource = DamageSource.causeMobDamage(shooter).setProjectile();
            if (shooter instanceof EntityPlayer) {
                damageSource = DamageSource.causePlayerDamage((EntityPlayer)shooter).setProjectile();
            }
            if (bulletItem.type.isFireDamage) {
                damageSource.setFireDamage();
            }
            if (bulletItem.type.isAbsoluteDamage) {
                damageSource.setDamageIsAbsolute();
            }
            if (bulletItem.type.isBypassesArmorDamage) {
                damageSource.setDamageBypassesArmor();
            }
            if (bulletItem.type.isExplosionDamage) {
                damageSource.setExplosion();
            }
            if (bulletItem.type.isMagicDamage) {
                damageSource.setMagicDamage();
            }
            if (amount.get() < 0) {
                amount.set(0);
            }
            boolean damageSuccess = false;
            if (!ModConfig.INSTANCE.shots.knockback_entity_damage) {
                damageSuccess = RayUtil.attackEntityWithoutKnockback(victim, damageSource, (float)amount.get());
            } else {
                damageSuccess = victim.attackEntityFrom(damageSource, (float)amount.get());
            }
            victim.hurtResistantTime = 0;

            // victim player's plate
            if (damageSuccess) {
                if (plate != null) {
                    EntityPlayerMP player = (EntityPlayerMP)victim;
                    plate.attemptDamageItem(1, player.getRNG(), player);
                    if (extraSlots != null) {
                        if (plate.getItemDamage() >= plate.getMaxDamage()) {
                            extraSlots.setStackInSlot(1, ItemStack.EMPTY);
                        } else {
                            extraSlots.setStackInSlot(1, plate);
                        }
                    }
                }
            }

            gunType.playSoundPos(victim.getPosition(), victim.world, WeaponSoundType.Penetration);
            if (shooter instanceof EntityPlayerMP) {
                ModularWarfare.NETWORK.sendTo(new PacketPlayHitmarker(headshot), (EntityPlayerMP)shooter);
            }

            // POST EVENT
            WeaponRayHitEntityEvent gunHitEntityEvent = new WeaponRayHitEntityEvent(shooter, victim, gunType.internalName, hitboxName, bulletHit.rayTraceResult.hitVec.x, bulletHit.rayTraceResult.hitVec.y, bulletHit.rayTraceResult.hitVec.z, baseDamage);
//            System.out.println(gunHitEntityEvent.damage + "," + gunHitEntityEvent.gunId + "," + gunHitEntityEvent.hitbox + "," + gunHitEntityEvent.hitX + "," + gunHitEntityEvent.hitY + "," + gunHitEntityEvent.hitZ);
            if (MinecraftForge.EVENT_BUS.post(gunHitEntityEvent)) {
                return;
            }
            if (victim instanceof EntityLivingBase) {
                if (headshot) {
                    EntityHeadShotEvent headShot = new EntityHeadShotEvent(victim, shooter);
                    MinecraftForge.EVENT_BUS.post(headShot);
                }
            }
        }

        private static void handleFireRayGunServer(EntityLivingBase shooter, FireData fireData) {
            ItemGun itemGun = fireData.itemGun;
            GunType gunType = fireData.gunType;
            ItemStack gunStack = fireData.gunStack;
            WeaponFireMode fireMode = fireData.fireMode;
            World world = fireData.world;
            int shotCount = computeShotCount(gunType, gunStack, fireMode, shooter);
            int numBullets = computePellet(gunType, gunStack, shooter);
            ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
            // fetch hit list
            List<BulletHit> rayTraceList = new ArrayList<>();
            boolean forceServerCalc = false;
            if (shooter instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP)shooter;
                if (player.ping > 100 * 20 && ModConfig.INSTANCE.general.serverShotVerification) {
                    forceServerCalc = true;
                }
            }
            if (fireData.clientSuggetions != null && !forceServerCalc) {
                fireData.clientSuggetions.forEach((hit) -> {
                    RayTraceResult result = null;
                    if (hit.victimEntityId != -1) {
                        Entity target = world.getEntityByID(hit.victimEntityId);
                        if (target != null) {
                            result = new RayTraceResult(target, new Vec3d(hit.hitX, hit.hitY, hit.hitZ));
                        }
                    }
                    if (result == null) {
                        result = new RayTraceResult(new Vec3d(hit.hitX, hit.hitY, hit.hitZ), hit.facing, new BlockPos(hit.hitX, hit.hitY, hit.hitZ));
                    }
                    rayTraceList.add(new BulletHit(result, hit.hitboxType, hit.distance, hit.remainingPenetrate, hit.remainingBlockPenetrate));
                });
            } else {
                for (int i = 0; i < numBullets; i++) {
                    List<BulletHit> rayTrace = RayUtil.standardEntityRayTrace(Side.SERVER, world, fireData.rotationPitch, fireData.rotationYaw, shooter, fireData.weaponRange, itemGun);
                    if (rayTrace == null) {
                        continue;
                    }
                    rayTraceList.addAll(rayTrace);
                }
            }

            customBulletHitPipeline(shooter, fireData, rayTraceList);

            // trail
            drawTrail(rayTraceList.isEmpty() ? null : rayTraceList.get(0), shooter, fireData);

            // hit effect
            ArrayList<Consumer<BulletHit>> hitEffecters = new ArrayList<Consumer<BulletHit>>();
            hitEffecters.add((rayTrace) -> {
                Entity targetEnt = rayTrace.getEntity();
                if (targetEnt instanceof EntityGrenade) {
                    ((EntityGrenade)targetEnt).explode();
                }
            });
            hitEffecters.add((rayTrace) -> {
                if (rayTrace.rayTraceResult.typeOfHit != Type.BLOCK) {
                    return;
                }
                if (rayTrace.rayTraceResult == null || rayTrace.rayTraceResult.hitVec == null) {
                    return;
                }
                BlockPos blockPos = rayTrace.rayTraceResult.getBlockPos();
                ItemGun.playImpactSound(world, rayTrace.rayTraceResult, gunType);
                gunType.playSoundPos(blockPos, world, WeaponSoundType.Crack, null, 1.0f, false);
                ItemGun.doHit(rayTrace.rayTraceResult);
                ItemGun.playHitEffect(world, rayTrace.rayTraceResult);
            });
            hitEffecters.add((rayTrace) -> {
                if (!(rayTrace.getEntity() instanceof EntityPlayer)) {
                    return;
                }
                EntityPlayer victim = (EntityPlayer)rayTrace.getEntity();
                ModularWarfare.NETWORK.sendTo(new PacketPlaySound(victim.getPosition(), "flyby", 1f, 1f), (EntityPlayerMP)victim);
                if (ModConfig.INSTANCE.hud.snap_fade_hit) {
                    ModularWarfare.NETWORK.sendTo(new PacketPlayerHit(), (EntityPlayerMP)victim);
                }
            });
            hitEffecters.add((rayTrace) -> {
                Entity targetEnt = rayTrace.getEntity();
                if (!(targetEnt instanceof EntityLivingBase)) {
                    return;
                }
                final EntityLivingBase victim = (EntityLivingBase)targetEnt;
                rayHitOnLivingServer(rayTrace, bulletItem, shooter, victim, fireData);
            });
            hitEffecters.forEach(rayTraceList::forEach);
        }

        private static boolean fireServer(EntityLivingBase shooter, FireData fireData) {
            ItemGun itemGun = fireData.itemGun;
            GunType gunType = fireData.itemGun.type;
            ItemStack gunStack = fireData.gunStack;
            World world = fireData.world;
            WeaponFireMode fireMode = fireData.fireMode;
            boolean useHeldWeapon = fireData.useHeldWeapon;
            boolean verifShot = verifShot(shooter, fireData);
            if (verifShot) {
                return false;
            }
            doGunSound(gunType, gunStack, shooter);
            switch (gunType.weaponType) {
                case Launcher: {
                    // todo support not player
                    if (shooter instanceof EntityPlayer) {
                        ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
                        final float accuracy = RayUtil.calculateAccuracy(itemGun, shooter);
                        EntityExplosiveProjectile projectile = new EntityExplosiveProjectile(world, (EntityPlayer)shooter, bulletItem.type.impactDamage, accuracy, bulletItem.type.projectileVelocity, bulletItem.type.internalName, bulletItem.type.gravity, bulletItem.type.isSmoke, bulletItem.type.isExplosion);
                        world.spawnEntity(projectile);
                    }
                    break;
                }
                case Thrower: {
                    // todo support not player
                    if (shooter instanceof EntityPlayer) {
                        ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
                        final float accuracy = RayUtil.calculateAccuracy(itemGun, shooter);
                        EntityThrowerProjectile projectile = new EntityThrowerProjectile(world, (EntityPlayer)shooter, bulletItem.type.impactDamage, accuracy, bulletItem.type.projectileVelocity, bulletItem.type.internalName, bulletItem.type.gravity, bulletItem.type.isSmoke);
                        world.spawnEntity(projectile);
                    }
                    break;
                }
                default: {
                    handleFireRayGunServer(shooter, fireData);
                    break;
                }
            }

            // Burst Stuff
            int shotCount = computeShotCount(gunType, gunStack, fireMode, shooter);
            if (fireMode == WeaponFireMode.BURST) {
                shotCount = shotCount - 1;
                gunStack.getTagCompound().setInteger("shotsremaining", shotCount);
            }

            if (useHeldWeapon) {
                ItemGun.consumeShot(gunStack);
            }

            if (shooter instanceof EntityPlayer) {
                // Hands upwards when shooting
                if (ServerTickHandler.playerAimShootCooldown.get(shooter.getUniqueID()) == null) {
                    ModularWarfare.NETWORK.sendToAll(new PacketAimingResponse(shooter.getUniqueID(), true));
                }
                ServerTickHandler.playerAimShootCooldown.put(shooter.getUniqueID(), 60);
                ModularWarfare.NETWORK.sendToAll(new PacketOtherShooterAnimation(shooter.getUniqueID(), AnimationType.FIRE, itemGun.type.internalName, itemGun.type.fireTickDelay, false));
            }
            return true;
        }
    }

    public static class FireClientPort {
        private static boolean fireClient(EntityPlayer entityPlayer, FireData fireData) {
            ItemGun itemGun = fireData.itemGun;
            ItemStack gunStack = fireData.gunStack;
            GunType gunType = fireData.gunType;
            WeaponFireMode fireMode = fireData.fireMode;
            World world = fireData.world;
            if (ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).reloading) {
                if (gunType.allowReloadFiring && 
                    (ItemGun.hasNextShot(gunStack) || 
                     ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).reloadPhase == EnhancedStateMachine.Phase.POST)) {
                    ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).stopReload();
                    ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).reset();
                    ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).updateCurrentItem(entityPlayer);
                }
            }

            // Can fire checks
            if (verifShot(entityPlayer, fireData)) {
                return false;
            }

            int shotCount = computeShotCount(gunType, gunStack, fireMode, entityPlayer);

            ModularWarfare.PROXY.onShootAnimation(entityPlayer, gunType.internalName, gunType.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw);

            // Sound
            doGunSound(gunType, gunStack, entityPlayer);

            // Burst Stuff
            if (fireMode == WeaponFireMode.BURST) {
                shotCount = shotCount - 1;
                gunStack.getTagCompound().setInteger("shotsremaining", shotCount);
            }

            ClientTickHandler.playerNextTime.put(entityPlayer.getUniqueID(), System.currentTimeMillis() + (long)((60f * 1000 / gunType.roundsPerMin) / PlayerStateManager.clientPlayerState.roundsPerMinFactor / PlayerStateManager.clientPlayerState.devetionRoundsPerMinFactor));

            if ((gunType.dropBulletCasing)) {
                /**
                 * Drop casing
                 */
                int numBullets = computePellet(gunType, gunStack, entityPlayer);
                ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
                GunEnhancedRenderConfig cfg = ModularWarfare.getRenderConfig(gunType, GunEnhancedRenderConfig.class);

                EntityShell shell = new EntityShell(world, entityPlayer, gunStack, itemGun, bulletItem);

                shell.setHeadingFromThrower(entityPlayer, entityPlayer.rotationPitch + cfg.extra.shellPitchOffset, entityPlayer.rotationYaw + 110 + cfg.extra.shellYawOffset, 0.0F, 0.2F, 5, 0.1f + cfg.extra.shellForwardOffset);
                world.spawnEntity(shell);
            }

            ItemGun.consumeShot(gunStack);

            /**
             * Hit Register
             */
            if (gunType.weaponType == WeaponType.Launcher || gunType.weaponType == WeaponType.Thrower) {
                ModularWarfare.NETWORK.sendToServer(new PacketGunFire(gunType.internalName, entityPlayer.rotationPitch, entityPlayer.rotationYaw));
            } else {
                DefaultRayCasting.onShot();
                handleFireRayGunClient(entityPlayer, fireData);
            }

            /**
             * recoil
             */

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
                ItemAttachment gripAttachment = (ItemAttachment)GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Grip).getItem();
                recoilPitchGripFactor = gripAttachment.type.grip.recoilPitchFactor;
                recoilYawGripFactor = gripAttachment.type.grip.recoilYawFactor;
            }

            if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel) != null) {
                ItemAttachment barrelAttachment = (ItemAttachment)GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel).getItem();
                recoilPitchBarrelFactor = barrelAttachment.type.barrel.recoilPitchFactor;
                recoilYawBarrelFactor = barrelAttachment.type.barrel.recoilYawFactor;
            }

            if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Stock) != null) {
                ItemAttachment stockAttachment = (ItemAttachment)GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Stock).getItem();
                recoilPitchStockFactor = stockAttachment.type.stock.recoilPitchFactor;
                recoilYawStockFactor = stockAttachment.type.stock.recoilYawFactor;
            }

            if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Laser) != null) {
                ItemAttachment laserAttachment = (ItemAttachment)GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Laser).getItem();
                recoilPitchLaserFactor = laserAttachment.type.laser.recoilPitchFactor;
                recoilYawLaserFactor = laserAttachment.type.laser.recoilYawFactor;
            }

            if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Pistolgrip) != null) {
                ItemAttachment pistolgripAttachment = (ItemAttachment)GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Pistolgrip).getItem();
                recoilPistolgripFactor = pistolgripAttachment.type.pistolgrip.recoilPitchFactor;
                recoilYawPistolgripFactor = pistolgripAttachment.type.pistolgrip.recoilYawFactor;
            }

            if (GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Handguard) != null) {
                ItemAttachment handguardAttachment = (ItemAttachment)GunType.getAttachment(entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Handguard).getItem();
                recoilPitchHandguardFactor = handguardAttachment.type.handguard.recoilPitchFactor;
                recoilYawHandguardFactor = handguardAttachment.type.handguard.recoilYawFactor;
            }

            boolean isCrawling = false;
            if (ModularWarfare.isLoadedModularMovements) {
                if (ClientListener.clientPlayerState.isCrawling) {
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
            if (ModularWarfare.isLoadedModularMovements) {
                if (ClientListener.clientPlayerState.isCrawling) {
                    offsetPitch *= gunType.recoilCrawlPitchFactor;
                    offsetYaw *= gunType.recoilCrawlYawFactor;
                }
            }
            offsetYaw *= PlayerStateManager.clientPlayerState.recoilYawFactor;
            offsetPitch *= PlayerStateManager.clientPlayerState.recoilPitchFactor;

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

            return true;
        }

        public static void handleFireRayGunClient(EntityPlayer entityPlayer, FireData fireData) {
            ItemGun itemGun = fireData.itemGun;
            if (entityPlayer.world.isRemote) {
                // 重置子弹索引，确保每次射击都从第一颗子弹开始
                RayUtil.resetBulletIndex();

                GunKickManager.AimingData aimData = GunKickManager.getAimingData(entityPlayer);
                // 强制更新瞄准数据,确保使用最新的数据
                aimData.updateForced(entityPlayer, itemGun);
                customBulletHitPipeline(entityPlayer, fireData, aimData.rayTraceList);
                // 发送尾迹渲染请求
                drawTrail((aimData.rayTraceList.isEmpty() ? null : aimData.rayTraceList.get(0)), entityPlayer, fireData);
                ArrayList<PacketGunFire.Hit> hits = new ArrayList<PacketGunFire.Hit>();
                for (BulletHit rayTrace : aimData.rayTraceList) {
                    PacketGunFire.Hit hit = new PacketGunFire.Hit();
                    if (rayTrace.rayTraceResult.entityHit != null) {
                        hit.victimEntityId = rayTrace.rayTraceResult.entityHit.getEntityId();
                    } else {
                        hit.victimEntityId = -1;
                    }
                    hit.hitboxType = getHitBoxName(rayTrace);
                    hit.remainingPenetrate = rayTrace.remainingPenetrate;
                    hit.remainingBlockPenetrate = rayTrace.remainingBlockPenetrate;
                    hit.distance = rayTrace.distance;
                    hit.hitX = rayTrace.rayTraceResult.hitVec.x;
                    hit.hitY = rayTrace.rayTraceResult.hitVec.y;
                    hit.hitZ = rayTrace.rayTraceResult.hitVec.z;
                    hit.facing = rayTrace.rayTraceResult.sideHit;
                    hits.add(hit);
                }
                ModularWarfare.NETWORK.sendToServer(new PacketGunFire(itemGun.type.internalName, entityPlayer.rotationPitch, entityPlayer.rotationYaw, hits));
            }
        }
    }

    private static boolean verifShot(EntityLivingBase user, FireData fireData) {
        boolean failed = false;
        ItemGun itemGun = fireData.itemGun;
        ItemStack gunStack = fireData.gunStack;
        GunType gunType = fireData.gunType;
        WeaponFireMode fireMode = fireData.fireMode;
        boolean useHeldWeapon = fireData.useHeldWeapon;
        ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
        if (bulletItem == null) {
            failed = true;
        }

        if (user == null || user.isDead || user.getHealth() <= 0) {
            failed = true;
        }
        if (gunStack == null || gunStack.isEmpty() || !(gunStack.getItem() instanceof ItemGun)) {
            failed = true;
        }

        if (!gunType.hasFireMode(fireMode)) {
            failed = true;
        }

        // PLAYER
        if (user instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer)user;
            if (entityPlayer.isSpectator()) {
                failed = true;
            }
            if (!entityPlayer.world.isRemote) {
                // SERVER
                if (itemGun.type.animationType == WeaponAnimationType.BASIC) {
                    if (ItemGun.isServerReloading(entityPlayer)) {
                        failed = true;
                    }
                }
                if ((!itemGun.type.allowSprintFiring && entityPlayer.isSprinting()) || !itemGun.type.hasFireMode(fireMode)) {
                    failed = true;
                }
                // todo 射速检查
            } else {
                // CLIENT
                if (itemGun.type.animationType == WeaponAnimationType.BASIC) {
                    if (ItemGun.isClientReloading(entityPlayer)) {
                        failed = true;
                    }
                }
                if (ItemGun.isOnShootCooldown(entityPlayer.getUniqueID()) || ClientRenderHooks.getAnimMachine(entityPlayer).attachmentMode || (!itemGun.type.allowSprintFiring && entityPlayer.isSprinting()) || !itemGun.type.hasFireMode(fireMode)) {
                    failed = true;
                }
                if (AnimationController.getController(entityPlayer, null) != null) {
                    if (!AnimationController.getController(entityPlayer, null).isCouldShoot()) {
                        failed = true;
                    }
                }
            }
        } else {
            // NOT PLAYER CAN'T FIRE ON CLIENT
            if (user.world.isRemote) {
                failed = true;
            }
        }
        if (!ItemGun.hasNextShot(gunStack)) {
            if (!user.world.isRemote) {
                // SERVER
                if (useHeldWeapon) {
                    failed = true;
                    // no dry fire on server
                    if (fireMode == WeaponFireMode.BURST) {
                        gunStack.getTagCompound().setInteger("shotsremaining", 0);
                    }
                }
            } else {
                // CLIENT
                if (user instanceof EntityPlayer) {
                    failed = true;
                    EntityPlayer entityPlayer = (EntityPlayer)user;
                    if (fireMode == WeaponFireMode.BURST)
                        gunStack.getTagCompound().setInteger("shotsremaining", 0);
                    if (ItemGun.canDryFireClient) {
                        gunType.playClientSound(entityPlayer, WeaponSoundType.DryFire);
                        ModularWarfare.PROXY.onShootFailedAnimation(entityPlayer, gunType.internalName);
                        ItemGun.canDryFireClient = false;
                    }
                } else {
                    // NOT PLAYER CAN'T FIRE ON CLIENT
                    failed = true;
                }
            }
        }

        return failed;
    }

    private static void doGunSound(GunType gunType, ItemStack gunStack, EntityLivingBase entity) {
        Consumer<WeaponSoundType> handler = (type) -> {
            if (entity.world.isRemote) {
                if (entity instanceof EntityPlayer) {
                    gunType.playClientSound((EntityPlayer)entity, type);
                }
            } else {
                gunType.playSound(entity, type, gunStack, entity instanceof EntityPlayer ? (EntityPlayer)entity : null);
            }
        };
        // FIRE
        if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Barrel) != null) {
            ItemAttachment barrelAttachment = (ItemAttachment)GunType.getAttachment(gunStack, AttachmentPresetEnum.Barrel).getItem();
            if (barrelAttachment.type.barrel.isSuppressor) {
                handler.accept(WeaponSoundType.FireSuppressed);
            } else {
                if (ItemGun.getAmmoCount(gunStack) <= 1 && gunType.weaponSoundMap.containsKey(WeaponSoundType.FireLast)) {
                    handler.accept(WeaponSoundType.FireLast);
                } else {
                    handler.accept(WeaponSoundType.Fire);
                }
            }
        } else {
            if (ItemGun.getAmmoCount(gunStack) <= 1 && gunType.weaponSoundMap.containsKey(WeaponSoundType.FireLast)) {
                handler.accept(WeaponSoundType.FireLast);
            } else {
                handler.accept(WeaponSoundType.Fire);
            }
        }
        // PUMP
        if (gunType.weaponType == WeaponType.BoltSniper || gunType.weaponType == WeaponType.Shotgun) {
            handler.accept(WeaponSoundType.Pump);
        }
    }

    private static void drawTrail(BulletHit baseHit, EntityLivingBase shooter, FireData fireData) {
        GunType gunType = fireData.gunType;
        ItemStack gunStack = fireData.gunStack;
        Vec3d origin = shooter.getPositionEyes(1.0f);
        Vec3d endVec = null;
        if (baseHit != null && baseHit.rayTraceResult != null && baseHit.rayTraceResult.hitVec != null) {
            endVec = baseHit.rayTraceResult.hitVec;
        }

        if (endVec == null) {
            Vec3d forward = shooter.getLookVec();
            endVec = origin.add(forward.scale(gunType.weaponMaxRange));
        }

        Vec3d direction = endVec.subtract(origin).normalize();

        String model = null;
        String tex = null;
        boolean glow = false;
        glow = gunType.customTrailGlow;
        ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
        BulletType bulletType = bulletItem.type;
        if (bulletType != null) {
            model = bulletType.trailModel;
            tex = bulletType.trailTex;
            if (model !=null && tex !=null && !model.isEmpty() && !tex.isEmpty()) {
                glow = bulletType.trailGlow;
            }
        }
        model = Optional.ofNullable(model).orElse(gunType.customTrailModel);
        tex = Optional.ofNullable(tex).orElse(gunType.customTrailTexture);
        if (model == null) {
            model = "";
        }
        if (tex == null) {
            tex = "";
        }

        if (!shooter.world.isRemote) {
            // SERVER
            if (gunType.useTeslaTrails) {
                ModularWarfare.NETWORK.sendToDimension(new PacketTeslaTrail(origin.x, origin.y, origin.z, endVec.x, endVec.y, endVec.z, 10f, gunType.internalName), shooter.dimension);
            } else {
                ModularWarfare.NETWORK.sendToDimension(new PacketGunTrail(gunType.internalName, model, tex, glow, origin.x, origin.y, origin.z, shooter.motionX, shooter.motionZ, direction.x, direction.y, direction.z, origin.distanceTo(endVec), 10), shooter.dimension);
            }
        } else {
            // CLIENT
            if (gunType.useTeslaTrails) {
                ModularWarfare.NETWORK.sendToServer(new PacketTeslaTrailAskServer(origin.x, origin.y, origin.z, endVec.x, endVec.y, endVec.z, 10f, gunType));
            } else {
                ModularWarfare.NETWORK.sendToServer(new PacketGunTrailAskServer(gunType, model, tex, glow, origin.x, origin.y, origin.z, shooter.motionX, shooter.motionZ, direction.x, direction.y, direction.z, origin.distanceTo(endVec), 10));
            }
        }
    }

    private static String getHitBoxName(BulletHit rayTrace) {
        String hitboxName = rayTrace.hitType;
        if (rayTrace instanceof OBBHit) {
            OBBHit obbHit = (OBBHit)rayTrace;
            hitboxName = obbHit.box.name;
        }
        return hitboxName;
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

    private static void customBulletHitPipeline(EntityLivingBase shooter, FireData fireData, List<BulletHit> hits) {
        WeaponFireEvent.BulletHitPipeline pipeline = new WeaponFireEvent.BulletHitPipeline(shooter, fireData, hits);
        MinecraftForge.EVENT_BUS.post(pipeline);
    }
}
