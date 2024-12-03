package com.modularwarfare.client.laser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.MWFOptifineShadesHelper;
import net.optifine.shaders.Shaders;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.modularwarfare.client.laser.LaserRenderManager.LaserDotInfo;
import com.modularwarfare.utility.OptifineHelper;

public class LaserDotRenderer {
    
    public static void renderLaserDots() {
        // 保存当前渲染状态
        GlStateManager.pushAttrib();
        GlStateManager.pushMatrix();
        
        boolean shadersEnabled = OptifineHelper.isShadersEnabled();
        int prevProgram = -1;
        
        if(shadersEnabled) {
            prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            Shaders.useProgram(Shaders.ProgramNone);
        }
        
        try {
            // 设置正交投影
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, scaledResolution.getScaledWidth_double(), 
                            scaledResolution.getScaledHeight_double(), 0.0D, -1.0D, 1.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            
            // 渲染激光点
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            
            for(LaserRenderManager.LaserDotInfo dot : LaserRenderManager.getInstance().getLaserDots()) {
                renderDot(dot, scaledResolution);
            }
            
        } finally {
            // 恢复所有状态
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            
            if(shadersEnabled) {
                GL20.glUseProgram(prevProgram);
            }
            
            GlStateManager.popMatrix();
            GlStateManager.popAttrib();
        }
        
        LaserRenderManager.getInstance().clearLaserDots();
    }

    private static void renderDot(LaserDotInfo dot, ScaledResolution resolution) {
        float x = resolution.getScaledWidth() / 2f;
        float y = resolution.getScaledHeight() / 2f;
        
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glPointSize(dot.dotSize);
        
        GlStateManager.color(dot.color[0], dot.color[1], dot.color[2], dot.alpha);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
        
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
    }
}