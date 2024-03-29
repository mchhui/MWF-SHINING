package com.modularwarfare.client.fpp.enhanced.animation;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.animations.ReloadType;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.gui.GuiGunModify;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.guns.WeaponSoundType;
import com.modularwarfare.utility.RayUtil;
import com.modularwarfare.utility.maths.Interpolation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.input.Mouse;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.GUN_CHANGE_Y;

public class AnimationController {

    public final EntityLivingBase player;

    private GunEnhancedRenderConfig config;

    private ActionPlayback playback;

    public double DEFAULT;
    public double DRAW;
    public double ADS;
    public double RELOAD;
    public double SPRINT;
    public double SPRINT_LOOP;
    public double SPRINT_RANDOM;
    public double INSPECT=1;
    public double FIRE;
    public double MODE_CHANGE;
    
    public long sprintCoolTime=0;
    public long sprintLoopCoolTime=0;

    public int oldCurrentItem;
    public ItemStack oldItemstack;
    public boolean isJumping=false;
    
    public boolean nextResetDefault=false;

    public boolean hasPlayedDrawSound = true;
    public ISound inspectSound=null;

    private static AnimationType[] RELOAD_TYPE =
        new AnimationType[] {AnimationType.PRE_LOAD, AnimationType.LOAD, AnimationType.POST_LOAD,
            AnimationType.PRE_UNLOAD, AnimationType.UNLOAD, AnimationType.POST_UNLOAD, AnimationType.PRE_RELOAD,
            AnimationType.PRE_RELOAD_EMPTY, AnimationType.RELOAD_FIRST, AnimationType.RELOAD_SECOND,
            AnimationType.RELOAD_FIRST_EMPTY, AnimationType.RELOAD_SECOND_EMPTY, AnimationType.RELOAD_FIRST_QUICKLY,
            AnimationType.RELOAD_SECOND_QUICKLY, AnimationType.POST_RELOAD, AnimationType.POST_RELOAD_EMPTY,};
    
    private static AnimationType[] FIRE_TYPE=new AnimationType[] {
            AnimationType.FIRE,
            AnimationType.PRE_FIRE, AnimationType.POST_FIRE, 
    };

    public AnimationController(EntityLivingBase player,GunEnhancedRenderConfig config){
        this.config = config;
        this.playback = new ActionPlayback(config);
        this.playback.action = AnimationType.DEFAULT;
        this.player = player;
    }
    
    public void reset(boolean resetSprint) {
        DEFAULT=0;
        DRAW=0;
        hasPlayedDrawSound = false;
        ADS=0;
        RELOAD=0;
        if(resetSprint) {
            SPRINT=0;
        }
        SPRINT_LOOP=0;
        INSPECT=1;
        if(inspectSound!=null) {
            Minecraft.getMinecraft().getSoundHandler().stopSound(inspectSound);
            inspectSound=null;
        }
        FIRE=0;
        MODE_CHANGE=1;
        updateActionAndTime();
    }
    
    public void resetView() {
        INSPECT=1;
        MODE_CHANGE=1;
    }

    public void onTickRender(float stepTick) {
        if(config==null) {
            return;
        }
        long time=System.currentTimeMillis();
        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);
        float moveDistance=player.distanceWalkedModified-player.prevDistanceWalkedModified;
        /** DEFAULT **/
        double defaultSpeed = config.animations.get(AnimationType.DEFAULT).getSpeed(config.FPS) * stepTick;
        if(playback.action==AnimationType.DEFAULT_EMPTY) {
            defaultSpeed = config.animations.get(AnimationType.DEFAULT_EMPTY).getSpeed(config.FPS) * stepTick;
        }
        if(DEFAULT==0&&DRAW==1) {
            if (player.getHeldItemMainhand().getItem() instanceof ItemGun&&player instanceof EntityPlayer) {
                GunType type=((ItemGun)player.getHeldItemMainhand().getItem()).type;
                if(playback.action==AnimationType.DEFAULT_EMPTY) {
                    type.playClientSound((EntityPlayer)player, WeaponSoundType.IdleEmpty);
                }else {
                    type.playClientSound((EntityPlayer)player, WeaponSoundType.Idle);
                }
            }
        }
        DEFAULT = Math.max(0F,DEFAULT + defaultSpeed);
        if(DEFAULT>1) {
            DEFAULT=0;
        }
        
        /** DRAW **/
        double drawSpeed = config.animations.get(AnimationType.DRAW).getSpeed(config.FPS) * stepTick;
        DRAW = Math.max(0, DRAW + drawSpeed);
        if(DRAW>1F) {
            DRAW=1F;
        }

        /** INSPECT **/
        if (INSPECT == 0) {
            if (player.getHeldItemMainhand().getItem() instanceof ItemGun&&player instanceof EntityPlayer) {
                GunType type = ((ItemGun)player.getHeldItemMainhand().getItem()).type;
                SoundEvent se = type.getSound((EntityPlayer)player, WeaponSoundType.Inspect);
                if(se!=null) {
                    inspectSound=PositionedSoundRecord.getRecord(se, 1, 1);
                    Minecraft.getMinecraft().getSoundHandler().playSound(inspectSound);  
                }
            }
        }
        if(INSPECT == 1) {
            if(inspectSound!=null) {
                Minecraft.getMinecraft().getSoundHandler().stopSound(inspectSound);
                inspectSound=null;
            }
        }
        if(!config.animations.containsKey (AnimationType.INSPECT)) {
            INSPECT=1;
        }else {
            double modeChangeVal = config.animations.get(AnimationType.INSPECT).getSpeed(config.FPS) * stepTick;
            INSPECT+=modeChangeVal;
            if(INSPECT>=1) {
                INSPECT=1;
            }
        }

        /** ADS **/
        boolean aimChargeMisc = ClientRenderHooks.getEnhancedAnimMachine(player).reloading;
        double adsSpeed = config.animations.get(AnimationType.AIM).getSpeed(config.FPS) * stepTick;
        double val = 0;
        if (RenderParameters.collideFrontDistance == 0 && Minecraft.getMinecraft().inGameHasFocus
            && Mouse.isButtonDown(1) && !aimChargeMisc && INSPECT == 1F) {
            val = ADS + adsSpeed * (2 - ADS);
        } else {
            val = ADS - adsSpeed * (1 + ADS);
        }
        RenderParameters.adsSwitch = (float)ADS;
        
        if(!isDrawing()) {
            ADS = Math.max(0, Math.min(1, val));
        }else {
            ADS = 0;
        }
        
        if(!anim.shooting) {
            FIRE=0;
        }
        
        if(!anim.reloading) {
            RELOAD=0;
        }

        /**
         * Sprinting
         */
        double sprintSpeed = Math.sin(SPRINT * 3.14) * 0.09f;
        if (sprintSpeed < 0.03f) {
            sprintSpeed = 0.03f;
        }
        sprintSpeed *= stepTick;
        double sprintValue = 0;
        
        if(player instanceof EntityPlayerSP) {
            if(((EntityPlayerSP)player).movementInput.jump) {
                isJumping=true;
            }else if(player.onGround) {
                isJumping=false;
            }  
        }

        boolean flag=(player.onGround||player.fallDistance<2f)&&!isJumping;

        if (player.isSprinting() && moveDistance > 0.05 && flag) {
            if (time > sprintCoolTime) {
                sprintValue = SPRINT + sprintSpeed;
            }
        } else {
            sprintCoolTime = time + 100;
            sprintValue = SPRINT - sprintSpeed;
        }
        if (anim.gunRecoil > 0.1F || ADS > 0.8 || RELOAD > 0 || INSPECT < 1) {
            sprintValue = SPRINT - sprintSpeed * 2.5f;
        }

        SPRINT = Math.max(0, Math.min(1, sprintValue));

        /** SPRINT_LOOP **/
        if (!config.animations.containsKey(AnimationType.SPRINT)) {
            SPRINT_LOOP = 0;
            SPRINT_RANDOM = 0;
        } else {
            double sprintLoopSpeed = config.animations.get(AnimationType.SPRINT).getSpeed(config.FPS) * stepTick
                    * (moveDistance / 0.15f);
            boolean flagSprintRand = false;
            if (flag) {
                if (time > sprintLoopCoolTime) {
                    if (player.isSprinting()) {
                        SPRINT_LOOP += sprintLoopSpeed;
                        SPRINT_RANDOM += sprintLoopSpeed;
                        flagSprintRand = true;
                    }
                }
            } else {
                sprintLoopCoolTime = time + 100;
            }
            if (!flagSprintRand) {
                SPRINT_RANDOM -= config.animations.get(AnimationType.SPRINT).getSpeed(config.FPS) * 3 * stepTick;
            }
            if (SPRINT_LOOP > 1) {
                SPRINT_LOOP = 0;
            }
            if (SPRINT_RANDOM > 1) {
                SPRINT_RANDOM = 0;
            }
            if (SPRINT_RANDOM < 0) {
                SPRINT_RANDOM = 0;
            }
            if (Double.isNaN(SPRINT_RANDOM)) {
                SPRINT_RANDOM = 0;
            }
        }
        /** MODE CHANGE **/
        if(!config.animations.containsKey (AnimationType.MODE_CHANGE)) {
            MODE_CHANGE=1;
        }else {
            double modeChangeVal = config.animations.get(AnimationType.MODE_CHANGE).getSpeed(config.FPS) * stepTick;
            MODE_CHANGE+=modeChangeVal;
            if(MODE_CHANGE>=1) {
                MODE_CHANGE=1;
            }
        }
        
        ClientRenderHooks.getEnhancedAnimMachine(player).onRenderTickUpdate(stepTick);  

        updateActionAndTime();
    }
    
    public AnimationType getPlayingAnimation() {
        return this.playback.action;
    }
    
    public void updateCurrentItem() {
        if(config==null) {
            return;
        }
        if(!(player instanceof EntityPlayer)) {
            return;
        }
        ItemStack stack = player.getHeldItemMainhand();
        Item item = stack.getItem();
        if (item instanceof ItemGun) {
            GunType type = ((ItemGun) item).type;
            if (!type.allowAimingSprint && ADS > 0.2f) {
                player.setSprinting(false);
            }
            if (!type.allowReloadingSprint && RELOAD > 0f) {
                player.setSprinting(false);
            }
            if (!type.allowFiringSprint && FIRE > 0f) {
                player.setSprinting(false);
            }
            if (ADS == 1) {
                if (!ClientRenderHooks.isAiming) {
                    ClientRenderHooks.isAiming = true;
                }
            } else {
                if (ClientRenderHooks.isAiming) {
                    ClientRenderHooks.isAiming = false;
                }
            }
        }
        boolean resetFlag=false;
        if(oldCurrentItem != ((EntityPlayer)player).inventory.currentItem){
            resetFlag=true;
            oldCurrentItem = ((EntityPlayer)player).inventory.currentItem;
        }
        if(oldItemstack != player.getHeldItemMainhand()) {
            if(oldItemstack==null||oldItemstack.isEmpty()) {
                resetFlag=true;
            }
            oldItemstack=player.getHeldItemMainhand();
        }
        if(resetFlag) {
            reset(true);
        }
    }
    
    public void updateAction() {
        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);
        boolean flag=nextResetDefault;
        nextResetDefault=false;
        if (DRAW < 1F) {
            if(!hasPlayedDrawSound){
                Item item = player.getHeldItemMainhand().getItem();
                if (item instanceof ItemGun) {
                    if((!(Minecraft.getMinecraft().currentScreen instanceof GuiGunModify))&&player instanceof EntityPlayer) {
                        ((ItemGun) item).type.playClientSound(((EntityPlayer)player), WeaponSoundType.Draw);  
                    }
                    hasPlayedDrawSound = true;
                }
            }
            this.playback.action = AnimationType.DRAW;
        }else if (RELOAD > 0F) {
            resetView();
            this.playback.action = anim.getReloadAnimationType();
        }else if(FIRE>0F) {
            resetView();
            this.playback.action = anim.getShootingAnimationType();
        } else if (INSPECT  < 1) {
            this.playback.action = AnimationType.INSPECT;
        } else if (MODE_CHANGE  < 1) {
            this.playback.action = AnimationType.MODE_CHANGE;
        } else if (this.playback.hasPlayed||(this.playback.action != AnimationType.DEFAULT&&this.playback.action !=AnimationType.DEFAULT_EMPTY)) {
            if(flag) {
                this.playback.action = AnimationType.DEFAULT;
                if(!ItemGun.hasNextShot(player.getHeldItemMainhand())) {
                    if(((GunEnhancedRenderConfig)config).animations.containsKey(AnimationType.DEFAULT_EMPTY)) {
                        this.playback.action = AnimationType.DEFAULT_EMPTY;  
                    }
                }
            }
            nextResetDefault=true;
        }
    }


    public void updateTime() {
        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);
        if(this.playback.action==null) {
            return;
        }
        switch (this.playback.action){
            case DEFAULT:
                this.playback.updateTime(DEFAULT);
                break;
            case DEFAULT_EMPTY:
                this.playback.updateTime(DEFAULT);
                break;
            case DRAW:
                this.playback.updateTime(DRAW);
                break;
            case INSPECT:
                this.playback.updateTime(INSPECT);
                break;
            case MODE_CHANGE:
                this.playback.updateTime(MODE_CHANGE);
                break;
        default:
            break;
        }
        for(AnimationType reloadType:RELOAD_TYPE) {
            if(this.playback.action==reloadType) {
                this.playback.updateTime(RELOAD);
                break;
            }  
        }
        for(AnimationType fireType:FIRE_TYPE) {
            if(this.playback.action==fireType) {
                this.playback.updateTime(FIRE);
                break;
            }  
        }
    }
    
    public void updateActionAndTime() {
        updateAction();
        updateTime();
    }

    public float getTime(){
        //return (280+(330-280)*(System.currentTimeMillis()%5000/5000f))/24f;
        return (float)playback.time;
    }
    
    public float getSprintTime(){
        if(config.animations.get(AnimationType.SPRINT)==null) {
            return 0;
        }
        double startTime = config.animations.get(AnimationType.SPRINT).getStartTime(config.FPS);
        double endTime = config.animations.get(AnimationType.SPRINT).getEndTime(config.FPS);
        double result=Interpolation.LINEAR.interpolate(startTime, endTime, SPRINT_LOOP);
        if(Double.isNaN(result)) {
            return 0;
        }
        return(float) result;
    }

    public void setConfig(GunEnhancedRenderConfig config){
        this.config = config;
    }

    public GunEnhancedRenderConfig getConfig(){
        return this.config;
    }
    
    public boolean isDrawing() {
        if(player==null) {
            return false;
        }
        Item item = player.getHeldItemMainhand().getItem();
        if (item instanceof ItemGun) {
            if (((ItemGun) item).type.animationType.equals(WeaponAnimationType.ENHANCED)) {
                return this.playback.action == AnimationType.DRAW;
            }
        }
        return false;
    }
    
    public boolean isCouldReload() {
        if(player==null) {
            return true;
        }
        Item item = player.getHeldItemMainhand().getItem();
        if (item instanceof ItemGun) {
            if (((ItemGun) item).type.animationType.equals(WeaponAnimationType.ENHANCED)) {
                if (isDrawing()) {
                    return false;
                }
                if(ClientRenderHooks.getEnhancedAnimMachine(player).reloading) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public boolean isCouldShoot() {
        if(player==null) {
            return true;
        }
        Item item = player.getHeldItemMainhand().getItem();
        if (item instanceof ItemGun) {
            if (((ItemGun) item).type.animationType.equals(WeaponAnimationType.ENHANCED)) {
                if (isDrawing()) {
                    return false;
                }
                if(ClientRenderHooks.getEnhancedAnimMachine(player).reloading) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public ItemStack getRenderAmmo(ItemStack ammo) {
        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);
        if(anim.reloading) {
            AnimationType reloadAni=anim.getReloadAnimationType();
            if (anim.getReloadType() == ReloadType.Full && (reloadAni == AnimationType.PRE_RELOAD
                    || reloadAni == AnimationType.RELOAD_FIRST || reloadAni == AnimationType.RELOAD_FIRST_QUICKLY)) {
                return ammo;
            }
            if (reloadAni == AnimationType.PRE_UNLOAD || reloadAni == AnimationType.UNLOAD|| reloadAni == AnimationType.POST_UNLOAD) {
                return ammo;
            }  
        }
        if (ClientTickHandler.reloadEnhancedPrognosisAmmoRendering != null
                && !ClientTickHandler.reloadEnhancedPrognosisAmmoRendering.isEmpty()) {
            return ClientTickHandler.reloadEnhancedPrognosisAmmoRendering;
        }
        return ammo;
    }
    
    public boolean shouldRenderAmmo() {
        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);
        if(anim.reloading) {
            if(anim.getReloadAnimationType()==AnimationType.POST_UNLOAD) {
                return false;
            }
            return true;
        }
        if (ClientTickHandler.reloadEnhancedPrognosisAmmoRendering != null
                && !ClientTickHandler.reloadEnhancedPrognosisAmmoRendering.isEmpty()) {
            return true;
        }
        return ItemGun.hasAmmoLoaded(player.getHeldItemMainhand());
    }

}
