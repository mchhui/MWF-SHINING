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

import com.modularwarfare.utility.OptifineHelper;

public class LaserDotRenderer {
    
    public static void renderLaserDots() {
        boolean shadersEnabled = OptifineHelper.isShadersEnabled();
        int prevProgram = -1;
        
        if(shadersEnabled) {
            // 保存当前程序ID而不是使用push/pop
            prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            Shaders.useProgram(Shaders.ProgramNone);
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
        }
        // 保存所有需要修改的状态
        GlStateManager.pushAttrib();
    
        // 保存矩阵状态
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        float x = scaledResolution.getScaledWidth() / 2;
        float y = scaledResolution.getScaledHeight() / 2;
        GL11.glOrtho(0.0D, scaledResolution.getScaledWidth_double(), scaledResolution.getScaledHeight_double(), 0.0D, -1000.0D, 1000.0D);
    
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        
        try {
            // 渲染点
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            
            for(LaserRenderManager.LaserDotInfo dot : LaserRenderManager.getInstance().getLaserDots()) {

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


                // 外圈
                GL11.glPointSize(dotSize * 1.25f);
                GlStateManager.color(
                    dot.color[0] * 0.5f,
                    dot.color[1] * 0.5f,
                    dot.color[2] * 0.5f,
                    dot.alpha * 0.5f
                );
                GL11.glBegin(GL11.GL_POINTS);
                GL11.glVertex2f(x, y);
                GL11.glEnd();
                
                // 主体
                GL11.glPointSize(dotSize);
                GlStateManager.color(
                    dot.color[0],
                    dot.color[1],
                    dot.color[2],
                    dot.alpha * 0.8f
                );
                GL11.glBegin(GL11.GL_POINTS);
                GL11.glVertex2f(x, y);
                GL11.glEnd();
                
                // 中心点
                GL11.glPointSize(dotSize * 0.25f);
                GlStateManager.color(
                    dot.color[0],
                    dot.color[1],
                    dot.color[2],
                    dot.alpha
                );
                GL11.glBegin(GL11.GL_POINTS);
                GL11.glVertex2f(x, y);
                GL11.glEnd();
            }
            
            // 恢复之前的状态
            GL11.glDisable(GL11.GL_POINT_SMOOTH);
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            
        } finally {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();

            GlStateManager.popAttrib();
        }
        
        LaserRenderManager.getInstance().clearLaserDots();

        if(shadersEnabled) {
            // 直接恢复到之前的程序
            GL20.glUseProgram(prevProgram);
            
            if(OptifineHelper.isRenderingDfb()) {
                OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, MWFOptifineShadesHelper.getDFB());
                Shaders.setDrawBuffers(MWFOptifineShadesHelper.getDFBDrawBuffers());
            }
        }
    }
}