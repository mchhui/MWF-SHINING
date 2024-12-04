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
        // 保存所有渲染状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();
        
        // 保存着色器状态
        boolean shadersEnabled = OptifineHelper.isShadersEnabled();
        int prevProgram = -1;
        if(shadersEnabled) {
            prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        }
        
        try {
            // 设置正交投影
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, scaledResolution.getScaledWidth_double(), 
                            scaledResolution.getScaledHeight_double(), 0.0D, -1.0D, 1.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            
            // 设置渲染状态
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            
            if(shadersEnabled) {
                Shaders.useProgram(Shaders.ProgramNone);
                OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
            }
            
            // 渲染激光点
            for(LaserRenderManager.LaserDotInfo dot : LaserRenderManager.getInstance().getLaserDots()) {
                renderDot(dot, scaledResolution);
            }
            
        } finally {
            // 恢复矩阵状态
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); 
            GlStateManager.popMatrix();
            
            // 恢复渲染状态
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            
            // 恢复着色器状态
            if(shadersEnabled) {
                GL20.glUseProgram(prevProgram);
                
                if(OptifineHelper.isRenderingDfb()) {
                    OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, MWFOptifineShadesHelper.getDFB());
                    Shaders.setDrawBuffers(MWFOptifineShadesHelper.getDFBDrawBuffers());
                }
            }
            
            // 恢复所有状态
            GlStateManager.popMatrix();
            GL11.glPopAttrib();
        }
        
        LaserRenderManager.getInstance().clearLaserDots();
    }

    private static void renderDot(LaserDotInfo dot, ScaledResolution resolution) {
        float x = resolution.getScaledWidth() / 2f;
        float y = resolution.getScaledHeight() / 2f;

        EntityPlayerSP player = (EntityPlayerSP) Minecraft.getMinecraft().player;
        Vec3d origin = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        Vec3d endVec = origin.add(look.scale(dot.maxDistance));

        RayTraceResult ray = player.world.rayTraceBlocks(origin, endVec, false, true, false);
        if (ray == null) {
            return;
        }
        double distance = origin.distanceTo(ray.hitVec);
        if (distance > dot.maxDistance) {
            return;
        }
            
        float dotSize = (float) (dot.dotSize * (1.0f - (float)(distance / dot.maxDistance)));
        
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        
        // 外圈
        GL11.glPointSize(dotSize * 1.25f);
        GlStateManager.color(dot.color[0] * 0.5f, dot.color[1] * 0.5f, dot.color[2] * 0.5f, dot.alpha * 0.5f);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
        
        // 主体
        GL11.glPointSize(dotSize);
        GlStateManager.color(dot.color[0], dot.color[1], dot.color[2], dot.alpha * 0.8f);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
        
        // 中心点
        GL11.glPointSize(dotSize * 0.25f);
        GlStateManager.color(dot.color[0], dot.color[1], dot.color[2], dot.alpha);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(x, y);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_POINT_SMOOTH);
    }
}