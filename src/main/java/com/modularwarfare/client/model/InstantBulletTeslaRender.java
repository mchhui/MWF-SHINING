package com.modularwarfare.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.MathHelper;

import org.lwjgl.opengl.GL11;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;
import java.util.List;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.vector.Vector3f;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.textures.TextureEnumType;
import safx.client.render.SARenderHelper.RenderType;

public class InstantBulletTeslaRender extends InstantBulletRenderer {
    private static TextureManager textureManager;
    private static CopyOnWriteArrayList<TeslaTrail> teslaTrails = new CopyOnWriteArrayList<>();
    
    public static void AddTeslaTrail(TeslaTrail trail) {
        if(trail != null && teslaTrails.size() < 40) teslaTrails.add(trail);
    }

    public static void RenderAllTeslaTrails(float partialTicks) {
        if(teslaTrails != null && teslaTrails.size() < 40) {
            for (TeslaTrail trail : teslaTrails) {
                trail.Render(partialTicks);
            }
        }
    }

    public static void UpdateAllTeslaTrails() {
        for (int i = teslaTrails.size() - 1; i >= 0; i--) {
            if (teslaTrails.get(i).Update()) {
                teslaTrails.remove(i);
            }
        }
    }

    public static class TeslaTrail {
        private Vector3f origin;
        private Vector3f hitPos;
        private float width;
        private float length;
        private float distanceToTarget;
        private float bulletSpeed;
        private int ticksExisted;
        private ResourceLocation texture;
        private int time;
        private float alpha = 1.0f;
        private static final float FADE_SPEED = 0.1f;
        private static final int FADE_START_TICK = 5;
        private static final int MAX_LIFETIME = 20;
        
        final static int SIN_COUNT = 6;
        final static double WIDTH = 1.5;
        final static double SIN_DISTANCE = 20.0;
        private double offset = 1.20;

        public TeslaTrail(Vector3f origin, Vector3f hitPos, float bulletSpeed, GunType gunType) {
            this.ticksExisted = 0;
            this.bulletSpeed = bulletSpeed;
            this.origin = origin;
            this.hitPos = hitPos;
            this.length = 8.0f * new Random().nextFloat();
            
            if(gunType != null && gunType.teslaType != null && !gunType.teslaType.resourceLocations.isEmpty()) {
                this.texture = gunType.teslaType.resourceLocations.get(0);
            } else {
                this.texture = new ResourceLocation(ModularWarfare.MOD_ID, "trail/textures/tesla.png");
            }
            
            this.width = 0.2f;
            this.alpha = 1.0f;

            Vector3f dPos = Vector3f.sub(hitPos, origin, null);
            List<RayTraceResult> results = ModularWarfare.INSTANCE.RAY_CASTING.rayTraceBlocks(Minecraft.getMinecraft().world, origin.toVec3(), hitPos.toVec3(), 100f, 1f, true, true, false);
            if (results != null && !results.isEmpty()) {
                RayTraceResult result = results.get(results.size() - 1);
                if (result.hitVec != null) {
                    dPos = Vector3f.sub(new Vector3f(result.hitVec), origin, null);
                }
            }
            
            this.distanceToTarget = dPos.length();
            if (Math.abs(distanceToTarget) > 300.0f) {
                distanceToTarget = 300.0f;
            }
        }

        public boolean Update() {
            ticksExisted++;
            
            if(ticksExisted >= MAX_LIFETIME) {
                return true;
            }
            
            if(ticksExisted > FADE_START_TICK) {
                alpha = Math.max(0, alpha - FADE_SPEED);
            }
            
            return alpha <= 0;
        }

        public void Render(float partialTicks) {
            if(alpha <= 0) {
                return;
            }

            float x_ = OpenGlHelper.lastBrightnessX;
            float y_ = OpenGlHelper.lastBrightnessY;
            
            GlStateManager.pushAttrib();

            if (textureManager == null) textureManager = Minecraft.getMinecraft().renderEngine;
            textureManager.bindTexture(texture);

            GlStateManager.color(0.5F, 0.8F, 1.0F, alpha);
            GlStateManager.pushMatrix();

            Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
            double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTicks;
            double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks;
            double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTicks;

            GL11.glTranslatef(-(float) x, -(float) y + (0.1F), -(float) z);

            float parametric = ((float) (ticksExisted) + partialTicks) * bulletSpeed;

            Vector3f dPos = Vector3f.sub(hitPos, origin, null);
            dPos.normalise();

            GlStateManager.pushMatrix();
            
            Vector3f dVec = new Vector3f(hitPos.x-origin.x, 0, hitPos.z-origin.z);
            dVec = (Vector3f)dVec.normalise();
            
            GlStateManager.translate(origin.x, origin.y - 0.1f, origin.z);
            float yaw = (float)Math.acos(dVec.z)/3.1415f*180;
            if(dVec.x < 0) {
                yaw = -yaw;
            }
            dVec = new Vector3f(hitPos.x-origin.x, hitPos.y-origin.y, hitPos.z-origin.z);
            dVec = (Vector3f)dVec.normalise();
            float pitch = (float)Math.asin(dVec.y)/3.1415f*180;
            
            GlStateManager.rotate(yaw, 0, 1, 0);
            GlStateManager.rotate(pitch, -1, 0, 0);
            
            ++time;
            
            GlStateManager.enableRescaleNormal();
            GlStateManager.depthMask(false);
            GlStateManager.disableCull();
            GlStateManager.pushMatrix();
            
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE,  // RGB混合
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA  // Alpha混合
            );
            
            float progress = parametric / 100F;
            double distance = this.distanceToTarget;
            Random rand = new Random(ticksExisted);
            double[] dY = new double[SIN_COUNT];
            double[] dZ = new double[SIN_COUNT];
            
            for (int i = 0; i < SIN_COUNT; i++) {
                dY[i] = (0.5-rand.nextDouble())*3D;
                dZ[i] = (0.5-rand.nextDouble())*3D;
            }
            
            int count = (int) Math.round(distance / offset);
            offset = (distance / (double) count);
            float xOffset = 0.0f;
            int xreps = Math.max(1, (int) Math.round(distance / SIN_DISTANCE));
            
            double xprev = 0, yprev = 0, zprev = 0, widthprev = 1.0;
            
            for (int i = 0; i <= count; i++) {
                double d = (double)i/(double)count;
                double x1 = xOffset + (double)i*offset;
                double y1 = 0;
                double z1 = 0;
                
                if (i > 1) {
                    for (int j = 1; j <= SIN_COUNT; j++) {
                        double yfactor = ((rand.nextDouble()-0.5) + (progress*dY[j-1] *1.0)) * (2.0/(double)j);
                        double zfactor = ((rand.nextDouble()-0.5) + (progress*dZ[j-1] *1.0)) * (2.0/(double)j);
                        y1 += Math.sin((d * Math.PI) * (double)(j * xreps))*yfactor;
                        z1 += Math.sin((d * Math.PI) * (double)(j * xreps))*zfactor;
                    }
                }
                
                double pulse = 1.0 - Math.sqrt(Math.abs(progress-d)*2.0);
                double width = Math.max(0.0, WIDTH*pulse);
                
                if (i >= 1) {
                    drawTeslaSegment(xprev, yprev, zprev, x1, y1, z1, widthprev, width, alpha, alpha);
                }
                
                widthprev = width;
                xprev = x1;
                yprev = y1;
                zprev = z1;
            }
            
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
            GlStateManager.disableRescaleNormal();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x_, y_);
            
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
            
            GlStateManager.popAttrib();
        }
    }
    
    private static void drawTeslaSegment(double x1, double y1, double z1, double x2, double y2, double z2, double width1, double width2, double a1, double a2) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        double scale = 0.5;
        y1 *= scale;
        y2 *= scale;
        z1 *= scale;
        z2 *= scale;
        width1 *= scale;
        width2 *= scale;
        
        GlStateManager.pushMatrix();
        GlStateManager.rotate(-90F, 0, 1, 0);
        GlStateManager.rotate(45.0f, 1.0f, 0.0f, 0.0f);
        
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(x1, y1-width1, z1).tex(0, 0).color(1.0f, 1.0f, 1.0f, (float)a1).endVertex();
        bufferbuilder.pos(x1, y1+width1, z1).tex(0, 1).color(1.0f, 1.0f, 1.0f, (float)a1).endVertex();
        bufferbuilder.pos(x2, y2+width2, z2).tex(1, 1).color(1.0f, 1.0f, 1.0f, (float)a1).endVertex();
        bufferbuilder.pos(x2, y2-width2, z2).tex(1, 0).color(1.0f, 1.0f, 1.0f, (float)a1).endVertex();
        tessellator.draw();
        
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(x1, y1, z1-width1).tex(0, 0).color(1.0f, 1.0f, 1.0f, (float)a1).endVertex();
        bufferbuilder.pos(x1, y1, z1+width1).tex(0, 1).color(1.0f, 1.0f, 1.0f, (float)a1).endVertex();
        bufferbuilder.pos(x2, y2, z2+width2).tex(1, 1).color(1.0f, 1.0f, 1.0f, (float)a1).endVertex();
        bufferbuilder.pos(x2, y2, z2-width2).tex(1, 0).color(1.0f, 1.0f, 1.0f, (float)a1).endVertex();
        tessellator.draw();
        
        GlStateManager.popMatrix();
    }
} 