package com.modularwarfare.loader;

import java.util.Comparator;
import java.util.HashMap;

import com.google.gson.Gson;
import com.modularwarfare.loader.api.model.ObjModelRenderer;
import com.modularwarfare.loader.blenderani.BlenderAnimation;
import com.modularwarfare.loader.blenderani.BlenderKeyFrame;

import net.minecraft.client.renderer.GlStateManager;
/**
 * @author Hueihuea
 * */
public class ObjModelAnimation {
    public ObjModel model;
    public BlenderAnimation blenderAnimation;
    public int fps;
    public long startTime;
    private int length;
    private long currentTime;
    public static double test=20;

    public ObjModelAnimation(ObjModel model, String aniJson, int fps) {
        this(model, new Gson().fromJson(aniJson, BlenderAnimation.class), fps);
    }
    public ObjModelAnimation(ObjModel model, BlenderAnimation animation, int fps) {
        this.model = model;
        this.blenderAnimation = animation;
        length = blenderAnimation.length;
        this.fps = fps;
        blenderAnimation.bones.values().forEach((bone)->{
            bone.keyframes.sort(new Comparator<BlenderKeyFrame>() {
                @Override
                public int compare(BlenderKeyFrame o1, BlenderKeyFrame o2) {
                    // TODO Auto-generated method stub
                    return o1.frame-o2.frame;
                }
            });
        });
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void updateFrame(long currentTime) {
        this.currentTime=currentTime;
        blenderAnimation.updateAnimation((currentTime - startTime)/1000.0 * (double) fps);
        //blenderAnimation.updateAnimation(test);
    }

    public boolean isFinish() {
        if(currentTime==0) {
            return true;
        }
        return ((currentTime - startTime) /1000.0*fps) >= length;
    }

    public void renderAll(float scale) {
        model.parts.forEach((part) -> {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, scale);
            blenderAnimation.bones.get(part.getName()).setupAnimation();
            part.render(1);
            GlStateManager.popMatrix();
        });
    }

    public void renderPart(String name, float scale) {
        model.parts.forEach((part) -> {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, scale);
            if(name.equals((part.getName()))) {
                blenderAnimation.bones.get(part.getName()).setupAnimation();
                part.render(1);  
            }
            GlStateManager.popMatrix();
        });
    }
}
