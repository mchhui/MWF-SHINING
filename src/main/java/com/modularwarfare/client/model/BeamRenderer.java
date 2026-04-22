package com.modularwarfare.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class BeamRenderer {

    private static final double EPSILON = 1.0E-6D;
    private static final int DEFAULT_RADIAL_SEGMENTS = 10;
    private static final int DEFAULT_AXIAL_SEGMENTS = 12;
    private static final float DEFAULT_UV_TILING_PER_BLOCK = 1.0F;

    private BeamRenderer() {
    }

    public static void renderTexturedCylinderBeam(
            Vec3d start,
            Vec3d end,
            float radius,
            float r,
            float g,
            float b,
            float alpha,
            ResourceLocation texture,
            float startFadeLength,
            float endFadeLength) {
        renderTexturedCylinderBeam(start, end, radius, r, g, b, alpha, texture, startFadeLength, endFadeLength,
                DEFAULT_RADIAL_SEGMENTS, DEFAULT_AXIAL_SEGMENTS, DEFAULT_UV_TILING_PER_BLOCK);
    }

    public static void renderTexturedCylinderBeam(
            Vec3d start,
            Vec3d end,
            float radius,
            float r,
            float g,
            float b,
            float alpha,
            ResourceLocation texture,
            float startFadeLength,
            float endFadeLength,
            int radialSegments,
            int axialSegments,
            float uvTilingPerBlock) {
        Vec3d axis = end.subtract(start);
        double length = axis.length();
        if (length < EPSILON) {
            return;
        }

        if (radius <= 0.0F || alpha <= 0.0F || texture == null) {
            return;
        }

        TextureManager textureManager = Minecraft.getMinecraft().renderEngine;
        textureManager.bindTexture(texture);

        Vec3d axisNorm = axis.normalize();
        Vec3d basisA = perpendicular(axisNorm);
        Vec3d basisB = axisNorm.crossProduct(basisA).normalize();

        radialSegments = Math.max(3, radialSegments);
        axialSegments = Math.max(1, axialSegments);
        uvTilingPerBlock = Math.max(0.0001F, uvTilingPerBlock);

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int i = 0; i < axialSegments; i++) {
            float t0 = i / (float) axialSegments;
            float t1 = (i + 1) / (float) axialSegments;
            float uLen0 = (float) (length * t0) * uvTilingPerBlock;
            float uLen1 = (float) (length * t1) * uvTilingPerBlock;

            Vec3d c0 = start.add(axis.scale(t0));
            Vec3d c1 = start.add(axis.scale(t1));

            float a0 = alpha * fadeFactor((float) (length * t0), (float) length, startFadeLength, endFadeLength);
            float a1 = alpha * fadeFactor((float) (length * t1), (float) length, startFadeLength, endFadeLength);

            for (int j = 0; j < radialSegments; j++) {
                float u0 = j / (float) radialSegments;
                float u1 = (j + 1) / (float) radialSegments;

                double ang0 = (Math.PI * 2.0D) * u0;
                double ang1 = (Math.PI * 2.0D) * u1;

                Vec3d ring0a = radialPoint(c0, basisA, basisB, radius, ang0);
                Vec3d ring0b = radialPoint(c0, basisA, basisB, radius, ang1);
                Vec3d ring1b = radialPoint(c1, basisA, basisB, radius, ang1);
                Vec3d ring1a = radialPoint(c1, basisA, basisB, radius, ang0);

                // U runs continuously along beam length, V runs around cylinder.
                addVertex(buffer, ring0a, uLen0, u0, r, g, b, a0);
                addVertex(buffer, ring0b, uLen0, u1, r, g, b, a0);
                addVertex(buffer, ring1b, uLen1, u1, r, g, b, a1);
                addVertex(buffer, ring1a, uLen1, u0, r, g, b, a1);
            }
        }
        tessellator.draw();

        renderCoreBeam(start, end, axisNorm, radius * 0.35F, r, g, b, alpha, startFadeLength, endFadeLength, axialSegments);
    }

    private static Vec3d perpendicular(Vec3d n) {
        Vec3d p = n.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D));
        if (p.lengthSquared() < EPSILON) {
            p = n.crossProduct(new Vec3d(1.0D, 0.0D, 0.0D));
        }
        return p.normalize();
    }

    private static Vec3d radialPoint(Vec3d center, Vec3d a, Vec3d b, float radius, double ang) {
        double ca = Math.cos(ang) * radius;
        double sa = Math.sin(ang) * radius;
        return center.add(a.scale(ca)).add(b.scale(sa));
    }

    private static void renderCoreBeam(Vec3d start, Vec3d end, Vec3d axisNorm, float coreRadius, float r, float g, float b,
                                       float alpha, float startFadeLength, float endFadeLength, int axialSegments) {
        if (coreRadius <= 0.0F) {
            return;
        }

        Vec3d axis = end.subtract(start);
        double length = axis.length();
        if (length < EPSILON) {
            return;
        }

        Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
        if (camera == null) {
            return;
        }

        Vec3d mid = start.add(end).scale(0.5D);
        Vec3d toCamera = camera.getPositionEyes(1.0F).subtract(mid);
        Vec3d sideA = axisNorm.crossProduct(toCamera);
        if (sideA.lengthSquared() < EPSILON) {
            sideA = perpendicular(axisNorm);
        } else {
            sideA = sideA.normalize();
        }
        Vec3d sideB = axisNorm.crossProduct(sideA).normalize();

        sideA = sideA.scale(coreRadius);
        sideB = sideB.scale(coreRadius);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        drawCoreStrip(buffer, start, axis, sideA, (float) length, r, g, b, alpha, startFadeLength, endFadeLength, axialSegments);
        drawCoreStrip(buffer, start, axis, sideB, (float) length, r, g, b, alpha, startFadeLength, endFadeLength, axialSegments);
        tessellator.draw();
    }

    private static void drawCoreStrip(BufferBuilder buffer, Vec3d start, Vec3d axis, Vec3d side, float totalLength,
                                      float r, float g, float b, float alpha, float startFadeLength, float endFadeLength,
                                      int axialSegments) {
        for (int i = 0; i < axialSegments; i++) {
            float t0 = i / (float) axialSegments;
            float t1 = (i + 1) / (float) axialSegments;
            Vec3d c0 = start.add(axis.scale(t0));
            Vec3d c1 = start.add(axis.scale(t1));
            float a0 = alpha * fadeFactor(totalLength * t0, totalLength, startFadeLength, endFadeLength);
            float a1 = alpha * fadeFactor(totalLength * t1, totalLength, startFadeLength, endFadeLength);

            addVertex(buffer, c0.add(side), 0.0F, 0.0F, r, g, b, a0);
            addVertex(buffer, c0.subtract(side), 1.0F, 0.0F, r, g, b, a0);
            addVertex(buffer, c1.subtract(side), 1.0F, 1.0F, r, g, b, a1);
            addVertex(buffer, c1.add(side), 0.0F, 1.0F, r, g, b, a1);
        }
    }

    private static float fadeFactor(float dist, float totalLen, float startFadeLen, float endFadeLen) {
        float start = 1.0F;
        if (startFadeLen > 0.0F) {
            start = clamp01(dist / startFadeLen);
        }
        float end = 1.0F;
        if (endFadeLen > 0.0F) {
            end = clamp01((totalLen - dist) / endFadeLen);
        }
        return Math.min(start, end);
    }

    private static float clamp01(float v) {
        if (v < 0.0F) {
            return 0.0F;
        }
        if (v > 1.0F) {
            return 1.0F;
        }
        return v;
    }

    private static void addVertex(BufferBuilder buffer, Vec3d pos, float u, float v, float r, float g, float b, float a) {
        buffer.pos(pos.x, pos.y, pos.z).tex(u, v).color(r, g, b, a).endVertex();
    }
}
