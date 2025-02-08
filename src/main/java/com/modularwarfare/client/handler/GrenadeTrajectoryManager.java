package com.modularwarfare.client.handler;

import com.modularwarfare.common.grenades.GrenadeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.MWFOptifineShadesHelper;
import com.modularwarfare.utility.OptifineHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.List;

public class GrenadeTrajectoryManager {
    private static final GrenadeTrajectoryManager INSTANCE = new GrenadeTrajectoryManager();
    private static final float NEAR_DISTANCE = 1.0f;
    private static final int SPHERE_DETAIL = 16;
    private static final float SPHERE_RADIUS = 0.15f;
    
    private List<Vec3d> trajectoryPoints = new ArrayList<>();
    private Vec3d collisionPoint = null;
    private Vec3d collisionNormal = null;
    
    public static GrenadeTrajectoryManager getInstance() {
        return INSTANCE;
    }
    
    public void calculateTrajectory(EntityPlayer player, GrenadeType type, float partialTicks) {
        trajectoryPoints.clear();
        collisionPoint = null;
        collisionNormal = null;
        
        Vec3d startPos = player.getPositionEyes(partialTicks);
        float strength = GrenadeEnhancedHandler.isThrowLow ? type.throwStrengthLow : type.throwStrength;
        
        Vec3d vec;
        if (!GrenadeEnhancedHandler.isThrowLow) {
            // 正常投掷增加15度仰角
            float pitch = player.rotationPitch - 15;
            float yaw = player.rotationYaw;
            float f = 0.017453292F;
            
            double x = -Math.sin(yaw * f) * Math.cos(pitch * f);
            double y = -Math.sin(pitch * f);
            double z = Math.cos(yaw * f) * Math.cos(pitch * f);
            
            vec = new Vec3d(x, y, z);
        } else {
            vec = player.getLookVec();
        }
        
        double modifier = player.isSprinting() ? 1.25 : 1.0;
        
        double motionX = ((vec.x * 1.5) * modifier) * strength;
        double motionY = ((vec.y * 1.5) * modifier) * strength;
        double motionZ = ((vec.z * 1.5) * modifier) * strength;
        
        double posX = startPos.x;
        double posY = startPos.y - 0.10;
        double posZ = startPos.z;
        
        Vec3d currentPos = new Vec3d(posX, posY, posZ);
        double distanceFromStart = 0;
        
        boolean onGround = false;
        
        for(int tick = 0; tick < 100; tick++) {
            Vec3d prevPos = currentPos;
            
            if (Math.abs(motionX) < 0.1 && Math.abs(motionZ) < 0.1) {
                motionX = 0;
                motionZ = 0;
            }
            
            motionX *= 0.98D;
            motionY = motionY * 0.98D - 0.04D;
            motionZ *= 0.98D;
            
            if(onGround) {
                motionX *= 0.8D;
                motionZ *= 0.8D;
            }
            
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            currentPos = new Vec3d(posX, posY, posZ);
            
            distanceFromStart = startPos.distanceTo(currentPos);
            
            if(distanceFromStart > NEAR_DISTANCE) {
                trajectoryPoints.add(currentPos);
            }
            
            RayTraceResult rayTrace = player.world.rayTraceBlocks(prevPos, currentPos, false, true, false);
            if(rayTrace != null) {
                collisionPoint = rayTrace.hitVec;
                collisionNormal = new Vec3d(rayTrace.sideHit.getDirectionVec());
                onGround = true;
                break;
            }
        }
    }
    
    public void renderTrajectory(EntityPlayer player, float partialTicks) {
        if(trajectoryPoints.isEmpty()) return;
        
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
            // 设置渲染状态
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableLighting();
            
            if(shadersEnabled) {
                Shaders.useProgram(Shaders.ProgramNone);
                OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
            }
            
            double interpPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
            double interpPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
            double interpPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
            
            GlStateManager.translate(-interpPosX, -interpPosY, -interpPosZ);
            
            GL11.glLineWidth(2.0F);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            
            // 渲染抛物线
            buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for(int i = 0; i < trajectoryPoints.size(); i++) {
                Vec3d point = trajectoryPoints.get(i);
                float alpha = 0.8f - (0.6f * i / trajectoryPoints.size());
                buffer.pos(point.x, point.y, point.z).color(1f, 1f, 1f, alpha).endVertex();
            }
            tessellator.draw();
            
            // 渲染落点球体
            if(collisionPoint != null) {
                renderCollisionSphere(tessellator, buffer, collisionPoint);
            }
            
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(1.0F);
            
        } finally {
            // 恢复渲染状态
            GlStateManager.enableTexture2D();
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
    }
    
    private void renderCollisionSphere(Tessellator tessellator, BufferBuilder buffer, Vec3d center) {
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        for(int i = 0; i <= SPHERE_DETAIL; i++) {
            double lat0 = Math.PI * (-0.5 + (double)(i - 1) / SPHERE_DETAIL);
            double lat1 = Math.PI * (-0.5 + (double)i / SPHERE_DETAIL);
            
            for(int j = 0; j <= SPHERE_DETAIL; j++) {
                double lng = 2 * Math.PI * (double)j / SPHERE_DETAIL;
                double x, y, z;
                
                x = Math.cos(lat0) * Math.cos(lng);
                y = Math.sin(lat0);
                z = Math.cos(lat0) * Math.sin(lng);
                buffer.pos(center.x + x * SPHERE_RADIUS, 
                          center.y + y * SPHERE_RADIUS, 
                          center.z + z * SPHERE_RADIUS)
                      .color(1f, 0f, 0f, 0.6f)
                      .endVertex();
                
                x = Math.cos(lat1) * Math.cos(lng);
                y = Math.sin(lat1);
                z = Math.cos(lat1) * Math.sin(lng);
                buffer.pos(center.x + x * SPHERE_RADIUS, 
                          center.y + y * SPHERE_RADIUS, 
                          center.z + z * SPHERE_RADIUS)
                      .color(1f, 0f, 0f, 0.6f)
                      .endVertex();
            }
        }
        
        tessellator.draw();
    }
    
    public void clearTrajectory() {
        trajectoryPoints.clear();
        collisionPoint = null;
        collisionNormal = null;
    }
} 