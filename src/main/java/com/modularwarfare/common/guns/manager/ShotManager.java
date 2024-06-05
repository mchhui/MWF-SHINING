package com.modularwarfare.common.guns.manager;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.WeaponFireEvent;
import com.modularwarfare.api.WeaponHitEvent;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.common.armor.ArmorType;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;
import com.modularwarfare.common.entity.EntityExplosiveProjectile;
import com.modularwarfare.common.entity.decals.EntityShell;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.hitbox.hits.BulletHit;
import com.modularwarfare.common.hitbox.hits.OBBHit;
import com.modularwarfare.common.hitbox.hits.PlayerHit;
import com.modularwarfare.common.hitbox.maths.EnumHitboxType;
import com.modularwarfare.common.network.*;
import com.modularwarfare.utility.RayUtil;
import com.teamderpy.shouldersurfing.client.ShoulderHelper;
import com.teamderpy.shouldersurfing.client.ShoulderInstance;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

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
                    //((ClientProxy)ModularWarfare.PROXY).playSound(new MWSound(entityPlayer.getPosition(), "defemptyclick", 1.0f, 1.0f));
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
                gunType.playClientSound(entityPlayer, WeaponSoundType.Fire);
            }
        } else if (GunType.isPackAPunched(gunStack)) {
            gunType.playClientSound(entityPlayer, WeaponSoundType.Punched);
            gunType.playClientSound(entityPlayer, WeaponSoundType.Fire);
        } else {
            gunType.playClientSound(entityPlayer, WeaponSoundType.Fire);
        }

        if (gunType.weaponType == WeaponType.BoltSniper || gunType.weaponType == WeaponType.Shotgun) {
            gunType.playClientSound(entityPlayer, WeaponSoundType.Pump);
        }

        // Burst Stuff
        if (fireMode == WeaponFireMode.BURST) {
            shotCount = shotCount - 1;
            gunStack.getTagCompound().setInteger("shotsremaining", shotCount);
        }

        ClientTickHandler.playerShootCooldown.put(entityPlayer.getUniqueID(), gunType.fireTickDelay);


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
        if (gunType.weaponType == WeaponType.Launcher) {
            ModularWarfare.NETWORK.sendToServer(new PacketGunFire(gunType.internalName, gunType.fireTickDelay, gunType.recoilPitch, gunType.recoilYaw, gunType.recoilAimReducer, gunType.bulletSpread, entityPlayer.rotationPitch, entityPlayer.rotationYaw));
        } else {
            fireClientSide(entityPlayer, itemGun);
        }
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
        if (ClientProxy.gunEnhancedRenderer.getController(entityPlayer, null) != null) {
            if(!ClientProxy.gunEnhancedRenderer.getController(entityPlayer, null).isCouldShoot()) {
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public static void fireServer(EntityPlayer entityPlayer, float rotationPitch, float rotationYaw, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, final int clientFireTickDelay, final float recoilPitch, final float recoilYaw, final float recoilAimReducer, final float bulletSpread) {
        GunType gunType = itemGun.type;
        // Can fire checks
        if (ShotValidation.verifShot(entityPlayer, gunStack, itemGun, fireMode, clientFireTickDelay, recoilPitch, recoilYaw, recoilAimReducer, bulletSpread)) {

            // Weapon pre fire event
            WeaponFireEvent.PreServer preFireEvent = new WeaponFireEvent.PreServer(entityPlayer, gunStack, itemGun, gunType.weaponMaxRange);
            MinecraftForge.EVENT_BUS.post(preFireEvent);
            if (preFireEvent.isCanceled())
                return;
            int shotCount = fireMode == WeaponFireMode.BURST ? gunStack.getTagCompound().getInteger("shotsremaining") > 0 ? gunStack.getTagCompound().getInteger("shotsremaining") : gunType.numBurstRounds : 1;

            if (preFireEvent.getResult() == Event.Result.DEFAULT || preFireEvent.getResult() == Event.Result.ALLOW) {
                if (!ItemGun.hasNextShot(gunStack)) {
                    if (ItemGun.canDryFire) {
                        gunType.playSound(entityPlayer, WeaponSoundType.DryFire, gunStack);
                        ItemGun.canDryFire = false;
                    }
                    if (fireMode == WeaponFireMode.BURST) gunStack.getTagCompound().setInteger("shotsremaining", 0);
                    return;
                }
            }

            // Sound
            if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Barrel) != null) {
                gunType.playSound(entityPlayer, WeaponSoundType.FireSuppressed, gunStack, entityPlayer);
            } else if (GunType.isPackAPunched(gunStack)) {
                gunType.playSound(entityPlayer, WeaponSoundType.Punched, gunStack, entityPlayer);
                gunType.playSound(entityPlayer, WeaponSoundType.Fire, gunStack, entityPlayer);
            } else {
                gunType.playSound(entityPlayer, WeaponSoundType.Fire, gunStack, entityPlayer);
            }
            int numBullets = gunType.numBullets;
            ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
            if (bulletItem == null) {
                return;
            }
            if (bulletItem.type.isSlug) {
                numBullets = 1;
            }

            if(gunType.weaponType != WeaponType.Launcher) {
                List<BulletHit> rayTraceList = new ArrayList<>();
                for (int i = 0; i < numBullets; i++) {
                    List<BulletHit> rayTrace = RayUtil.standardEntityRayTrace(Side.SERVER, world, rotationPitch, rotationYaw, entityPlayer, preFireEvent.getWeaponRange(), itemGun, GunType.isPackAPunched(gunStack));
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
                        final EntityPlayer victim = ((PlayerHit) rayTrace).getEntity();
                        if (victim == null || victim.isDead || victim.getHealth() <= 0.f) {
                            rayTraceIterator.remove();
                            continue;
                        }
                        gunType.playSoundPos(victim.getPosition(), world, WeaponSoundType.Penetration);
                        headshot = ((PlayerHit) rayTrace).hitbox.type.equals(EnumHitboxType.HEAD);
                        if (entityPlayer instanceof EntityPlayerMP) {
                            ModularWarfare.NETWORK.sendTo(new PacketPlayHitmarker(headshot), (EntityPlayerMP) entityPlayer);
                            ModularWarfare.NETWORK.sendTo(new PacketPlaySound(victim.getPosition(), "flyby", 1f, 1f), (EntityPlayerMP) victim);
                            if (ModConfig.INSTANCE.hud.snap_fade_hit) {
                                ModularWarfare.NETWORK.sendTo(new PacketPlayerHit(), (EntityPlayerMP) victim);
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
                        ((EntityGrenade) targetEnt).explode();
                        continue;
                    }
                    if (targetEnt instanceof EntityLivingBase) {
                        final EntityLivingBase victim = (EntityLivingBase) targetEnt;
                        gunType.playSoundPos(victim.getPosition(), world, WeaponSoundType.Penetration);
                        headshot = ItemGun.canEntityGetHeadshot(victim) && rayTrace.rayTraceResult.hitVec.y >= victim.getPosition().getY() + victim.getEyeHeight() - 0.15f;
                        if (entityPlayer instanceof EntityPlayerMP) {
                            ModularWarfare.NETWORK.sendTo(new PacketPlayHitmarker(headshot), (EntityPlayerMP) entityPlayer);
                        }
                        continue;
                    }
                    if (rayTrace.rayTraceResult.hitVec != null) {
                        BlockPos blockPos = rayTrace.rayTraceResult.getBlockPos();
                        ItemGun.playImpactSound(world, blockPos, gunType);
                        gunType.playSoundPos(blockPos, world, WeaponSoundType.Crack, entityPlayer, 1.0f);
                        ItemGun.doHit(rayTrace.rayTraceResult, entityPlayer);
                    }
                }

                // Weapon post fire event
                WeaponFireEvent.Post postFireEvent = new WeaponFireEvent.Post(entityPlayer, gunStack, itemGun, rayTraceList);
                MinecraftForge.EVENT_BUS.post(postFireEvent);

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

                        // Weapon pre hit event
                        WeaponHitEvent.Pre preHitEvent = new WeaponHitEvent.Pre(entityPlayer, gunStack, itemGun, headshot, postFireEvent.getDamage(), bulletHit.remainingPenetrate, targetEntity);
                        MinecraftForge.EVENT_BUS.post(preHitEvent);
                        if (preHitEvent.isCanceled())
                            return;

                        if (headshot) {
                            preHitEvent.setDamage(preHitEvent.getDamage() + gunType.gunDamageHeadshotBonus);
                        }
                        if (gunType.gunPenetrationDamageFalloff && preHitEvent.getPenetrateDamageFactor() > 0) {
                            preHitEvent.setDamage(preHitEvent.getDamage() * preHitEvent.getPenetrateDamageFactor());
                        }

                        if (targetEntity instanceof EntityLivingBase) {
                            EntityLivingBase targetELB = (EntityLivingBase) targetEntity;
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
                            RayUtil.attackEntityWithoutKnockback(targetEntity, DamageSource.causePlayerDamage(preFireEvent.getWeaponUser()).setProjectile(), preHitEvent.getDamage());
                        } else {
                            targetEntity.attackEntityFrom(DamageSource.causePlayerDamage(preFireEvent.getWeaponUser()).setProjectile(), preHitEvent.getDamage());
                        }
                        targetEntity.hurtResistantTime = 0;

                        // Weapon pre hit event
                        WeaponHitEvent.Post postHitEvent = new WeaponHitEvent.Post(entityPlayer, gunStack, itemGun, postFireEvent.getHits(), preHitEvent.getDamage());
                        MinecraftForge.EVENT_BUS.post(postHitEvent);
                    }
                }
            } else {
                EntityExplosiveProjectile projectile = new EntityExplosiveProjectile(world, entityPlayer, 0.5f, 3f, 2.5f, bulletItem.type.internalName);
                world.spawnEntity(projectile);
            }

            // Burst Stuff
            if (fireMode == WeaponFireMode.BURST) {
                shotCount = shotCount - 1;
                gunStack.getTagCompound().setInteger("shotsremaining", shotCount);
            }

            if (preFireEvent.getResult() == Event.Result.DEFAULT || preFireEvent.getResult() == Event.Result.ALLOW) {
                ItemGun.consumeShot(gunStack);
            }

            //Hands upwards when shooting
            if (ServerTickHandler.playerAimShootCooldown.get(entityPlayer.getUniqueID()) == null) {
                ModularWarfare.NETWORK.sendToAll(new PacketAimingResponse(entityPlayer.getUniqueID(), true));
            }
            ServerTickHandler.playerAimShootCooldown.put(entityPlayer.getUniqueID(), 60);
        } else {
            if (ModConfig.INSTANCE.general.modified_pack_server_kick) {
                ((EntityPlayerMP) entityPlayer).connection.disconnect(new TextComponentString("[ModularWarfare] Kicked for client-side modified content-pack. (Bad RPM/Recoil for the gun: " + itemGun.type.internalName + ") [RPM should be: " + itemGun.type.roundsPerMin + "]"));
            }
        }
    }


    public static void fireClientSide(EntityPlayer entityPlayer, ItemGun itemGun){
        if (entityPlayer.world.isRemote) {
            int numBullets = itemGun.type.numBullets;
            ItemBullet bulletItem = ItemGun.getUsedBullet(entityPlayer.getHeldItemMainhand(), itemGun.type);
            if (bulletItem != null) {
                if (bulletItem.type.isSlug) {
                    numBullets = 1;
                }
            }


            Minecraft mc = Minecraft.getMinecraft();
            Entity entity = mc.getRenderViewEntity();
            float pitch=entityPlayer.rotationPitch;
            float yaw=entityPlayer.rotationYaw;
            if(ClientProxy.shoulderSurfingLoaded) {
                if(ShoulderInstance.getInstance().doShoulderSurfing()) {
                    Vec3d eye=entity.getPositionEyes(ClientProxy.renderHooks.partialTicks);
                    double posX=eye.x;
                    double posY=eye.y;
                    double posZ=eye.z;
//                    if(ModularWarfare.isLoadedModularMovements) {
//                        if (entity instanceof EntityPlayer) {
//                            eye= ModularMovementsHooks.onGetPositionEyes((EntityPlayer) entity, ClientProxy.renderHooks.partialTicks);
//                        }
//                    }
                    RayTraceResult r=getMouseOver(ClientProxy.renderHooks.partialTicks);
                    posX=r.hitVec.x-posX;
                    posY=r.hitVec.y-posY;
                    posZ=r.hitVec.z-posZ;
                    pitch=(float)-Math.toDegrees(Math.atan(posY/Math.sqrt(posX*posX+posZ*posZ)));
                    yaw=(float)Math.toDegrees(Math.acos((posX*0+posZ*1)/Math.sqrt(posX*posX+posZ*posZ)));
                    if(posX>0) {
                        yaw=-yaw;
                    }  
                }
            }
            ArrayList<BulletHit> rayTraceList = new ArrayList<BulletHit>();
            for (int i = 0; i < numBullets; i++) {
                List<BulletHit> rayTrace = RayUtil.standardEntityRayTrace(Side.CLIENT, entityPlayer.world, pitch, yaw, entityPlayer, itemGun.type.weaponMaxRange, itemGun, false);
                rayTraceList.addAll(rayTrace);
            }

            ModularWarfare.NETWORK.sendToServer(new PacketExpShot(entityPlayer.getEntityId(), itemGun.type.internalName));

            boolean headshot = false;
            for (BulletHit rayTrace : rayTraceList) {
                if (rayTrace instanceof OBBHit) {
                    final EntityLivingBase victim = ((OBBHit) rayTrace).entity;
                    if (victim != null) {
                        if (!victim.isDead && victim.getHealth() > 0.0f) {
                            //Send server player hit + hitbox
                            //entityPlayer.sendMessage(new TextComponentString(((OBBHit) rayTrace).box.name));
                            ModularWarfare.NETWORK.sendToServer(new PacketExpGunFire(victim.getEntityId(), itemGun.type.internalName, ((OBBHit) rayTrace).box.name, itemGun.type.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw, itemGun.type.recoilAimReducer, itemGun.type.bulletSpread, rayTrace.remainingPenetrate, rayTrace.rayTraceResult.hitVec.x, rayTrace.rayTraceResult.hitVec.y, rayTrace.rayTraceResult.hitVec.z));
                        }
                    }
                } else {
                    if (rayTrace.rayTraceResult != null) {
                        if (rayTrace.rayTraceResult.hitVec != null) {
                            if(rayTrace.rayTraceResult.entityHit != null){
                                //Normal entity hit
                                headshot = ItemGun.canEntityGetHeadshot(rayTrace.rayTraceResult.entityHit) && rayTrace.rayTraceResult.hitVec.y >= rayTrace.rayTraceResult.entityHit.getPosition().getY() + rayTrace.rayTraceResult.entityHit.getEyeHeight() - 0.15f;
                                ModularWarfare.NETWORK.sendToServer(new PacketExpGunFire(rayTrace.rayTraceResult.entityHit.getEntityId(), itemGun.type.internalName, (headshot? "head":""), itemGun.type.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw, itemGun.type.recoilAimReducer, itemGun.type.bulletSpread, rayTrace.remainingPenetrate, rayTrace.rayTraceResult.hitVec.x, rayTrace.rayTraceResult.hitVec.y, rayTrace.rayTraceResult.hitVec.z));
                            } else {
                                //Crack hit block packet
                                ModularWarfare.NETWORK.sendToServer(new PacketExpGunFire(-1, itemGun.type.internalName, "", itemGun.type.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw, itemGun.type.recoilAimReducer, itemGun.type.bulletSpread, rayTrace.remainingPenetrate, rayTrace.rayTraceResult.hitVec.x, rayTrace.rayTraceResult.hitVec.y, rayTrace.rayTraceResult.hitVec.z,rayTrace.rayTraceResult.sideHit));                            }
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
            Vec3d vec3d2 = vec3d.addVector(vec3d1.x * d1, vec3d1.y * d1, vec3d1.z * d1);
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
}
