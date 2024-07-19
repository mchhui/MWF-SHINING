package com.modularwarfare.client.fpp.enhanced.animation;

import java.util.concurrent.atomic.AtomicInteger;

import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.AnimationType.AnimationTypeJsonAdapter;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.utility.maths.Interpolation;

public class ActionPlayback {

    public AnimationType action;

    public double time;
    
    private double lastTime=0;
    private AnimationType lastAction;

    public boolean hasPlayed;
    
    private AnimationController animationController;

    private GunEnhancedRenderConfig config;
    

    public ActionPlayback(AnimationController animationController, GunEnhancedRenderConfig config){
        this.animationController=animationController;
        this.config = config;
    }

    public void updateTime(double alpha){
        if (action == AnimationType.CUSTOM) {
            double startTime = animationController.startTime * 1/config.FPS;
            double endTime = animationController.endTime * 1/config.FPS; 
            try {
                AnimationType type=AnimationTypeJsonAdapter.fromString(animationController.customAnimation);  
                startTime = config.animations.get(type).getStartTime(config.FPS);
                endTime = config.animations.get(type).getEndTime(config.FPS);
            }catch(Exception e) {
            }
            this.time = Interpolation.LINEAR.interpolate(startTime, endTime, alpha);
            if(alpha>=1) {
                hasPlayed=true;
            }else {
                hasPlayed=false;
            }
        } else {
            if (config.animations.get(action) == null) {
                return;
            }
            double startTime = config.animations.get(action).getStartTime(config.FPS);
            double endTime = config.animations.get(action).getEndTime(config.FPS);
            this.time = Interpolation.LINEAR.interpolate(startTime, endTime, alpha);
            checkPlayed();
        }
        /**
         * 特殊帧触发器 BEGIN
         * */
        if(config.animations.get(this.action)!=null) {
            if(this.lastTime!=this.time) {
                if(this.lastAction!=this.action) {
                    this.lastTime=config.animations.get(this.action).getStartTime(config.FPS);
                }
                if(config.specialEffect.ejectionGroups!=null) {
                    config.specialEffect.ejectionGroups.forEach((group)->{
                        double testTime=group.throwShellFrame/config.FPS;
                        if(this.lastTime<=testTime&&this.time>testTime) {
                            RenderGunEnhanced.addEjectShell(group,0.15f);
                        }
                    });
                }
            }  
        }
        this.lastTime=time;
        this.lastAction=action;
        /**
         * 特殊帧触发器 END
         * */
    }
    
    public boolean checkPlayed() {
        double endTime = config.animations.get(action).getEndTime(config.FPS);
        if(this.time >= endTime){
            this.hasPlayed = true;
        } else {
            this.hasPlayed = false;
        }
        return hasPlayed;
    }
}