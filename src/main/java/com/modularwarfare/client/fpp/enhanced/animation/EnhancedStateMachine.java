package com.modularwarfare.client.fpp.enhanced.animation;

import java.util.Random;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.Passer;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.animations.ReloadType;
import com.modularwarfare.client.fpp.basic.animations.StateEntry;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.Animation;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAmmo;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponSoundType;
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.network.PacketGunReloadEnhancedStop;
import com.modularwarfare.common.network.PacketGunReloadSound;
import com.modularwarfare.common.playerstate.PlayerStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class EnhancedStateMachine {

    public AnimationController controller;
    
    /**
     * RELOAD
     */
    public float reloadTime;
    private ReloadType reloadType;
    public boolean reloading = false;
    public int reloadCount = 0;
    public int reloadMaxCount = 0;

    /**
     * Recoil
     */
    public float gunRecoil = 0F, lastGunRecoil = 0F;
    public float recoilSide = 0F;
    /**
     * Slide
     */
    public float gunSlide = 0F, lastGunSlide = 0F;

    /**
     * Shoot State Machine
     */
    public boolean shooting = false;
    public boolean aimState=false;
    private float shootTime;
    public int flashCount = 0;
    public boolean isFailedShoot = false;

    public ModelEnhancedGun currentModel;
    public Phase reloadPhase = Phase.PRE;
    public Phase lastReloadPhase = null;
    public Phase shootingPhase = Phase.PRE;

    public ItemStack heldItemstStack=ItemStack.EMPTY;
    public int lastItemIndex=0;
    
    public float devotionSpeed=1;

    public static enum Phase {
        PRE, FIRST, SECOND, POST
    }

    public void reset() {
        reloadTime = 0;
        reloadType = null;
        reloading = false;
        reloadCount = 0;
        reloadMaxCount = 0;
        gunRecoil = 0;
        lastGunRecoil = 0;
        recoilSide = 0;
        gunSlide = 0;
        lastGunSlide = 0;
        shooting = false;
        aimState=false;
        shootTime = 0;
        flashCount = 0;
        isFailedShoot = false;
        currentModel = null;
        reloadPhase = Phase.PRE;
        lastReloadPhase = null;
        shootingPhase = Phase.PRE;
        RenderGunEnhanced.postSmokeTp=0;
    }

    public void triggerShoot(AnimationController controller,ModelEnhancedGun model, GunType gunType, int fireTickDelay) {
        triggerShoot(controller,model, gunType, fireTickDelay, false);
    }

    public void triggerShoot(AnimationController controller,ModelEnhancedGun model, GunType gunType, int fireTickDelay, boolean isFailed) {
        lastGunRecoil = gunRecoil = 1F;
        lastGunSlide = gunSlide = 1F;

        shooting = true;
        shootTime = fireTickDelay;
        aimState=ClientRenderHooks.isAiming || ClientRenderHooks.isAimingScope;
        recoilSide = (float) (-1F + Math.random() * (1F - (-1F)));
        if (isFailed) {
            recoilSide = 0;
            lastGunRecoil = gunRecoil = 0F;
            lastGunSlide = gunSlide = 0F;
        }
        isFailedShoot = isFailed;
        this.shootingPhase = Phase.PRE;
        this.currentModel = model;
        this.controller=controller;
        
        
        if(!isFailed) {
            devotionSpeed+=gunType.devotionSpeed;
        }
        if(!isFailed&&this.controller.EJECTION_SMOKE==0) {
            this.controller.EJECTION_SMOKE=1;
        }
        if(!isFailed&&this.controller.POST_SMOKE==0||this.controller.POST_SMOKE>1) {
            if(this.controller.POST_SMOKE>1) {
                this.controller.POST_SMOKE=1.2f;
            }
            RenderGunEnhanced.postSmokeAlpha+=0.05f*controller.getConfig().specialEffect.postSmokeFactor;
            if(RenderGunEnhanced.postSmokeAlpha>1) {
                RenderGunEnhanced.postSmokeAlpha=1;
            }  
        }
    }

    public void triggerReload(AnimationController controller,EntityLivingBase entity,int reloadTime, int reloadCount, ModelEnhancedGun model, ReloadType reloadType) {
        reset();
        updateCurrentItem(entity);
        this.reloadTime = reloadType != ReloadType.Full ? reloadTime * 0.65f : reloadTime;
        this.reloadCount = reloadCount;
        Item item = heldItemstStack.getItem();
        if (item instanceof ItemGun) {
            GunType type = ((ItemGun) item).type;
            if(reloadType==ReloadType.Unload) {
                this.reloadCount-=type.modifyUnloadBullets;  
            }
        }
        this.reloadMaxCount = reloadCount;
        this.reloadType = reloadType;
        this.reloadPhase = Phase.PRE;
        this.lastReloadPhase = null;
        this.reloading = true;
        this.currentModel = model;
        
        this.controller=controller;
    }

    public void onTickUpdate() {
        // Recoil
        lastGunRecoil = gunRecoil;
        if (gunRecoil > 0)
            gunRecoil *= 0.5F;
    }

    public ReloadType getReloadType() {
        return this.reloadType;
    }

    public AnimationType getReloadAnimationType() {
        AnimationType aniType = null;
        if (reloadType == ReloadType.Load) {
            ItemStack stack = heldItemstStack;
            Item item = stack.getItem();
            if (item instanceof ItemGun) {
                GunType type = ((ItemGun) item).type;
                if (type.acceptedAmmo != null) {
                    if (reloadPhase == Phase.FIRST) {
                        aniType = AnimationType.LOAD;
                    } else if (reloadPhase == Phase.POST) {
                        aniType = AnimationType.POST_LOAD;
                    } else if (reloadPhase == Phase.PRE) {
                        aniType = AnimationType.PRE_LOAD;
                    }
                } else {
                    //使用子弹的枪械
                    if (reloadPhase == Phase.FIRST) {
                        if (!ItemGun.hasNextShot(heldItemstStack) && ((GunEnhancedRenderConfig)currentModel.config).animations.containsKey(AnimationType.RELOAD_FIRST_EMPTY)) {
                            aniType = AnimationType.RELOAD_FIRST_EMPTY;
                        } else {
                            aniType = AnimationType.RELOAD_FIRST;
                        }
                    } else if (reloadPhase == Phase.SECOND) {
                        if (!ItemGun.hasNextShot(heldItemstStack) && ((GunEnhancedRenderConfig)currentModel.config).animations.containsKey(AnimationType.RELOAD_SECOND_EMPTY)) {
                            aniType = AnimationType.RELOAD_SECOND_EMPTY;
                        } else {
                            aniType = AnimationType.RELOAD_SECOND;
                        }
                    } else if (reloadPhase == Phase.POST) {
                        if (!ItemGun.hasNextShot(heldItemstStack) && ((GunEnhancedRenderConfig)currentModel.config).animations.containsKey(AnimationType.POST_RELOAD_EMPTY)) {
                            aniType = AnimationType.POST_RELOAD_EMPTY;
                        } else {
                            aniType = AnimationType.POST_RELOAD;
                        }
                    } else {
                        if (!ItemGun.hasNextShot(heldItemstStack) && ((GunEnhancedRenderConfig)currentModel.config).animations.containsKey(AnimationType.PRE_RELOAD_EMPTY)) {
                            aniType = AnimationType.PRE_RELOAD_EMPTY;
                        } else {
                            aniType = AnimationType.PRE_RELOAD;
                        }
                    }
                }
            }
        } else if (reloadType == ReloadType.Unload) {
            if (reloadPhase == Phase.FIRST) {
                aniType = AnimationType.UNLOAD;
            } else if (reloadPhase == Phase.POST) {
                aniType = AnimationType.POST_UNLOAD;
            } else if (reloadPhase == Phase.PRE) {
                aniType = AnimationType.PRE_UNLOAD;
            }
        } else if (reloadType == ReloadType.Full) {
            if (reloadPhase == Phase.FIRST) {
                if (!ItemGun.hasNextShot(heldItemstStack) && ((GunEnhancedRenderConfig)currentModel.config).animations.containsKey(AnimationType.RELOAD_FIRST_EMPTY)) {
                    aniType = AnimationType.RELOAD_FIRST_EMPTY;
                } else {
                    aniType = AnimationType.RELOAD_FIRST;
                }
            } else if (reloadPhase == Phase.SECOND) {
                if (!ItemGun.hasNextShot(heldItemstStack) && ((GunEnhancedRenderConfig)currentModel.config).animations.containsKey(AnimationType.RELOAD_SECOND_EMPTY)) {
                    aniType = AnimationType.RELOAD_SECOND_EMPTY;
                } else {
                    aniType = AnimationType.RELOAD_SECOND;
                }
            } else if (reloadPhase == Phase.POST) {
                if (!ItemGun.hasNextShot(heldItemstStack) && ((GunEnhancedRenderConfig)currentModel.config).animations.containsKey(AnimationType.POST_RELOAD_EMPTY)) {
                    aniType = AnimationType.POST_RELOAD_EMPTY;
                } else {
                    aniType = AnimationType.POST_RELOAD;
                }
            } else {
                if (!ItemGun.hasNextShot(heldItemstStack) && ((GunEnhancedRenderConfig)currentModel.config).animations.containsKey(AnimationType.PRE_RELOAD_EMPTY)) {
                    aniType = AnimationType.PRE_RELOAD_EMPTY;
                } else {
                    aniType = AnimationType.PRE_RELOAD;
                }
            }
        }
        return aniType;
    }

    public AnimationType getShootingAnimationType() {
        AnimationType aniType = AnimationType.PRE_FIRE;

        GunEnhancedRenderConfig config = (GunEnhancedRenderConfig) currentModel.config;

        boolean isAiming = aimState;

        boolean isLastShot = !ItemGun.hasNextShot(heldItemstStack);
    
        switch (shootingPhase) {
            case FIRST:
                aniType = getFirstPhaseAnimation(config, isAiming, isLastShot);
                break;
    
            case POST:
                aniType = getPostPhaseAnimation(config, isAiming, isLastShot);
                break;
            
            case PRE:
                aniType = getPrePhaseAnimation(isAiming);
                break;
    
            default:
                break;
        }
    
        if (isFailedShoot && shootingPhase != Phase.PRE) {
            return AnimationType.DEFAULT;
        }
    
        return aniType;
    }
    
    private AnimationType getFirstPhaseAnimation(GunEnhancedRenderConfig config, boolean isAiming, boolean isLastShot) {
        if (isAiming) {
            if (isLastShot && config.animations.containsKey(AnimationType.FIRE_LAST_ADS)) {
                return AnimationType.FIRE_LAST_ADS;
            } else if (isLastShot && config.animations.containsKey(AnimationType.FIRE_LAST)) {
                return AnimationType.FIRE_LAST;
            } else if (config.animations.containsKey(AnimationType.FIRE_ADS)) {
                return AnimationType.FIRE_ADS;
            } else {
                return AnimationType.FIRE;
            }
        } else {
            return isLastShot && config.animations.containsKey(AnimationType.FIRE_LAST)
                   ? AnimationType.FIRE_LAST
                   : AnimationType.FIRE;
        }
    }
    
    private AnimationType getPostPhaseAnimation(GunEnhancedRenderConfig config, boolean isAiming, boolean isLastShot) {
        if (isAiming) {
            if (isLastShot && config.animations.containsKey(AnimationType.POST_FIRE_ADS_EMPTY)) {
                return AnimationType.POST_FIRE_ADS_EMPTY;
            } else if (isLastShot && config.animations.containsKey(AnimationType.POST_FIRE_EMPTY)) {
                return AnimationType.POST_FIRE_EMPTY;
            } else if (config.animations.containsKey(AnimationType.POST_FIRE_ADS)) {
                return AnimationType.POST_FIRE_ADS;
            } else {
                return AnimationType.POST_FIRE;
            }
        } else {
            return isLastShot && config.animations.containsKey(AnimationType.POST_FIRE_EMPTY)
                   ? AnimationType.POST_FIRE_EMPTY
                   : AnimationType.POST_FIRE;
        }
    }

    private AnimationType getPrePhaseAnimation(boolean isAiming) {
        if (isAiming) {
            return AnimationType.PRE_FIRE_ADS;
        }
        return AnimationType.PRE_FIRE;
    }
    
    public float getReloadSppedFactor() {
        ItemStack stack = heldItemstStack;
        Item item = stack.getItem();
        if (controller != null) {
            if (item instanceof ItemGun) {
                GunType type = ((ItemGun) item).type;
                if (ItemGun.hasAmmoLoaded(stack)) {
                    ItemStack stackAmmo = new ItemStack(stack.getTagCompound().getCompoundTag("ammo"));
                    stackAmmo = controller.getRenderAmmo(stackAmmo);
                    if (stackAmmo != null && stackAmmo.getItem() instanceof ItemAmmo) {
                        ItemAmmo itemAmmo = (ItemAmmo) stackAmmo.getItem();
                        return itemAmmo.type.reloadSpeedFactor;
                    }
                }
            }
        }
        return 1;
    }

    public void updateCurrentItem(EntityLivingBase player) {
        int index=0;
        if(player==Minecraft.getMinecraft().player) {
            index=Minecraft.getMinecraft().player.inventory.currentItem;
            if(devotionSpeed<0||!(ItemGun.fireButtonHeld&&ItemGun.lastFireButtonHeld)) {
                devotionSpeed=1;
            }
            PlayerStateManager.clientPlayerState.devetionRoundsPerMinFactor=devotionSpeed;
        }
        if ((!ItemStack.areItemStacksEqualUsingNBTShareTag(heldItemstStack,player.getHeldItemMainhand()))||index!=lastItemIndex) {
            if (reloading) {
                stopReload();
            }
            if (!shooting) {
                reset();
            }
            //ClientTickHandler.reloadEnhancedPrognosisAmmo=ItemStack.EMPTY;
        }
        heldItemstStack = player.getHeldItemMainhand();
        
        lastItemIndex=index;
    }

    public void onRenderTickUpdate(float partialTick) {
        if(controller==null) {
            return;
        }
        ItemStack stack = heldItemstStack;
        Item item = stack.getItem();
        if (item instanceof ItemGun) {
            GunType type = ((ItemGun) item).type;
            if (reloading) {
                /** RELOAD **/
                AnimationType aniType = getReloadAnimationType();
                Passer<Phase> phase = new Passer(reloadPhase);
                Passer<Double> progess = new Passer(controller.RELOAD);
                reloading = phaseUpdate(aniType, partialTick, getReloadSppedFactor(), phase, progess,()->{
                    if(reloadCount>0) {
                        phase.set(Phase.FIRST);  
                    }else {
                        phase.set(Phase.POST);
                    }
                }, () -> {
                    reloadCount--;
                    if (type.acceptedAmmo != null) {
                        phase.set(Phase.SECOND);
                    } else {
                        if (reloadCount <= 0) {
                            phase.set(Phase.POST);
                        } else {
                            phase.set(Phase.SECOND);
                        }
                    }
                }, () -> {
                    if (reloadCount <= 0) {
                        phase.set(Phase.POST);
                    } else {
                        phase.set(Phase.FIRST);
                    }
                });
                if (reloadPhase != lastReloadPhase && aniType!=null) {
                    switch (aniType) {
                    case PRE_LOAD:
                        playReloadSound(WeaponSoundType.PreLoad);
                        break;
                    case LOAD:
                        playReloadSound(WeaponSoundType.Load);
                        break;
                    case POST_LOAD:
                        playReloadSound(WeaponSoundType.PostLoad);
                        break;
                    case PRE_UNLOAD:
                        playReloadSound(WeaponSoundType.PreUnload);
                        break;
                    case UNLOAD:
                        playReloadSound(WeaponSoundType.Unload);
                        break;
                    case POST_UNLOAD:
                        playReloadSound(WeaponSoundType.PostUnload);
                        break;
                    case PRE_RELOAD:
                        playReloadSound(WeaponSoundType.PreReload);
                        break;
                    case PRE_RELOAD_EMPTY:
                        playReloadSound(WeaponSoundType.PreReloadEmpty);
                        break;
                    case RELOAD_FIRST:
                        playReloadSound(WeaponSoundType.Reload);
                        break;
                    case RELOAD_SECOND:
                        playReloadSound(WeaponSoundType.ReloadSecond);
                        break;
                    case RELOAD_FIRST_QUICKLY:
                        playReloadSound(WeaponSoundType.Reload);
                        break;
                    case RELOAD_SECOND_QUICKLY:
                        playReloadSound(WeaponSoundType.ReloadSecond);
                        break;
                    case RELOAD_FIRST_EMPTY:
                        playReloadSound(WeaponSoundType.ReloadEmpty);
                        break;
                    case RELOAD_SECOND_EMPTY:
                        playReloadSound(WeaponSoundType.ReloadSecondEmpty);
                        break;
                    case POST_RELOAD:
                        playReloadSound(WeaponSoundType.PostReload);
                        break;
                    case POST_RELOAD_EMPTY:
//                        ModularWarfare.NETWORK.sendToServer(new PacketGunReloadSound(WeaponSoundType.PostReloadEmpty));
                        playReloadSound(WeaponSoundType.PostReloadEmpty);
                        break;
                    default:
                        break;
                    }
                }
                lastReloadPhase=reloadPhase;
                reloadPhase = phase.get();
                //System.out.println(reloadPhase+":"+getReloadAnimationType());
                controller.RELOAD = progess.get();
                if (!reloading) {
                    controller.updateActionAndTime();
                    stopReload();
                }
            }
            if (shooting) {
                /*
                shootProgress += 1F / shootTime;
                
                if (shootProgress >= 1F) {
                    shooting = false;
                    shootProgress = 0f;
                }
                */
                AnimationType aniType = getShootingAnimationType();
                Passer<Phase> phase = new Passer(shootingPhase);
                Passer<Double> progess = new Passer(controller.FIRE);
                shooting = phaseUpdate(aniType, partialTick, 1, phase, progess,()->{
                    phase.set(Phase.FIRST);
                }, () -> {
                    phase.set(Phase.POST);
                }, null);
                shootingPhase = phase.get();
                controller.FIRE = progess.get();
                if (!shooting) {
                    if((controller.POST_SMOKE==0||controller.POST_SMOKE>1)) {
                        controller.POST_SMOKE=1.2f;
                        RenderGunEnhanced.postSmokeWind=(float)(Math.random()-0.5f);
                        for(int i=0;i<=RenderGunEnhanced.diversion;i++) {
                            RenderGunEnhanced.forward_joint[i]=0;
                            RenderGunEnhanced.strafing_joint[i]=0;
                        }
//                        System.out.println(RenderGunEnhanced.postSmokeAlpha);
                    }
                    controller.updateActionAndTime();
                }
            }
        }
    }
    
    public void playReloadSound(WeaponSoundType soundType) {
        EntityPlayer entityPlayer=Minecraft.getMinecraft().player;
        if (entityPlayer.getHeldItemMainhand() != null) {
            if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun) {
                ItemStack gunStack = entityPlayer.getHeldItemMainhand();
                ItemGun itemGun = (ItemGun) entityPlayer.getHeldItemMainhand().getItem();
                GunType gunType = itemGun.type;

                if (soundType != null) {
                    gunType.playClientSound(entityPlayer, soundType);
                }
            }
        }
        ModularWarfare.NETWORK.sendToServer(new PacketGunReloadSound(soundType));
    }

    public boolean phaseUpdate(AnimationType aniType, float partialTick,float speedFactor, Passer<Phase> phase, Passer<Double> progress,
            Runnable preCall,Runnable firstCall, Runnable secondCall) {
        boolean flag = true;
        Animation ani = null;
        if (aniType != null) {
            ani = ((GunEnhancedRenderConfig)currentModel.config).animations.get(aniType);
        }
//        System.out.println(aniType+":"+ani);
        if (ani != null) {
            double speed = ani.getSpeed(currentModel.config.FPS) * speedFactor * partialTick;
            double val = progress.get() + speed;
            progress.set(val);
            if (progress.get() > 1) {
                progress.set(1D);
            } else if (progress.get() < 0) {
                progress.set(0D);
            }
        } else {
            progress.set(1D);
        }
        if (progress.get() >= 1F) {
            if (phase.get() == Phase.POST) {
                flag = false;
                progress.set(0D);
            } else if (phase.get() == Phase.FIRST) {
                progress.set(Double.MIN_VALUE);
                if (firstCall != null) {
                    firstCall.run();
                }
            } else if (phase.get() == Phase.SECOND) {
                progress.set(Double.MIN_VALUE);
                if (secondCall != null) {
                    secondCall.run();
                }
            } else if (phase.get() == Phase.PRE) {
                progress.set(Double.MIN_VALUE);
                if (preCall != null) {
                    preCall.run();
                }
            }
        }
        return flag;
    }

    public void stopReload() {
        PacketGunReloadEnhancedStop packet = null;
        ItemStack stack = heldItemstStack;
        Item item = stack.getItem();
        if (item instanceof ItemGun) {
            GunType type = ((ItemGun) item).type;
            
            AnimationType reloadAni=getReloadAnimationType();
            
            if (reloadType == ReloadType.Load) {
                if (type.acceptedAmmo != null) {
                    if (reloadPhase == Phase.PRE) {
                        packet = new PacketGunReloadEnhancedStop(0, false, false);
                    } else {
                        packet = new PacketGunReloadEnhancedStop(0, true, true);
                    }
                } else {
                    if (reloadPhase == Phase.PRE) {
                        packet = new PacketGunReloadEnhancedStop(0, false, false);
                    } else {
                        packet = new PacketGunReloadEnhancedStop(reloadMaxCount - reloadCount, true, true);
                    }
                }
            } else if (reloadType == ReloadType.Full) {
                if (type.acceptedAmmo != null) {
                    if (reloadPhase == Phase.POST || reloadPhase == Phase.SECOND) {
                        packet = new PacketGunReloadEnhancedStop(0, true, true);
                    } else if (reloadPhase == Phase.FIRST) {
                        packet = new PacketGunReloadEnhancedStop(0, true, false);
                    } else {
                        packet = new PacketGunReloadEnhancedStop(0, false, false);
                    }
                } else {
                    if (reloadPhase == Phase.PRE) {
                        packet = new PacketGunReloadEnhancedStop(0, false, false);
                    } else {
                        packet = new PacketGunReloadEnhancedStop(reloadMaxCount - reloadCount, true, true);
                    }
                }
            } else if (reloadType == ReloadType.Unload) {
                if (reloadPhase == Phase.PRE) {
                    packet = new PacketGunReloadEnhancedStop(0, false, false);
                } else {
                    if (type.acceptedAmmo != null) {
                        packet = new PacketGunReloadEnhancedStop(0, true, false);
                    } else {
                        packet = new PacketGunReloadEnhancedStop(reloadMaxCount - reloadCount, true, false);
                    }
                }
            }
            if(packet!=null) {
                if(type.acceptedAmmo!=null) {
                    if(packet.loaded) {
                        ItemStack ammoStack=ClientTickHandler.reloadEnhancedPrognosisAmmoRendering;
                        if(ammoStack!=null&&!ammoStack.isEmpty()) {
                            ammoStack.setItemDamage(0);
                            if(reloadAni==AnimationType.RELOAD_FIRST||reloadAni==AnimationType.RELOAD_FIRST_QUICKLY||reloadAni==AnimationType.UNLOAD) {
                                ammoStack=ItemStack.EMPTY;
                            }
                            if(ammoStack.getItem() instanceof ItemAmmo) {
                                heldItemstStack.getTagCompound().setTag("ammo", ammoStack.writeToNBT(new NBTTagCompound()));
                            }  
                        }
                    }else if(packet.unloaded) {
                        heldItemstStack.getTagCompound().removeTag("ammo");
                    }
                }else{
                    if(packet.loaded) {
                        ItemStack bulletStack = ClientTickHandler.reloadEnhancedPrognosisAmmoRendering;
                        if(bulletStack!=null&&!bulletStack.isEmpty()) {
                            bulletStack.setItemDamage(0);
                            int offset = getAmmoCountOffset(true);
                            int ammoCount = heldItemstStack.getTagCompound().getInteger("ammocount");
                            heldItemstStack.getTagCompound().setInteger("ammocount", ammoCount + offset);
                            heldItemstStack.getTagCompound().setTag("bullet", bulletStack.writeToNBT(new NBTTagCompound()));  
                        }
                    }else if(packet.unloaded) {
                        heldItemstStack.getTagCompound().setInteger("ammocount", 0);
                        heldItemstStack.getTagCompound().removeTag("bullet");  
                    }
                }  
                ModularWarfare.NETWORK.sendToServer(packet);  
                ClientTickHandler.reloadEnhancedPrognosisAmmo=null;
                ClientTickHandler.reloadEnhancedPrognosisAmmoRendering=null;
            }
            //System.out.println(reloadType+"-"+reloadPhase+"-"+packet);
        }
    }

    public boolean canSprint() {
        return !shooting && !reloading && controller.ADS < 0.8f;
    }

    public int getAmmoCountOffset(boolean really) {
        ItemStack stack = heldItemstStack;
        if(heldItemstStack!=null) {
            Item item = stack.getItem();
            if (item instanceof ItemGun) {
                GunType type = ((ItemGun) item).type;
                if (reloading) {
                    if (reloadType == ReloadType.Unload) {
                        if(really) {
                            return -(reloadMaxCount - reloadCount);
                        }else {
                            return -(reloadMaxCount - reloadCount-type.modifyUnloadBullets);
                        }
                    } else {
                        return reloadMaxCount - reloadCount;
                    }
                }
            }  
        }
        if (reloadType == ReloadType.Unload) {
            return -reloadMaxCount;
        } else {
            return reloadMaxCount;
        }
    }

}
