package com.modularwarfare.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import cloud.siz.atomic.api.render.AtomicGBufferCompat;

/**
 * Soft-depend helpers for SIZ Atomic Shader G-buffer / shadow / emissive.
 * Mirrors HE {@code AtomicShaderCompat}; MWF already compileOnly-depends on the Atomic API jar.
 */
@SideOnly(Side.CLIENT)
public final class AtomicShaderCompat {

    public static final String MODID = "siz_atomicshader";

    private static final ThreadLocal<ShadowDepthSession> SHADOW_SESSION = new ThreadLocal<>();
    private static final java.nio.FloatBuffer LIGHT_VP_UPLOAD =
            org.lwjgl.BufferUtils.createFloatBuffer(16);

    private static final class ShadowDepthSession {
        final Runnable rebindDepth;
        final float[] lightViewProj;
        final float originX, originY, originZ;

        ShadowDepthSession(Runnable rebindDepth, float[] lightViewProj, float ox, float oy, float oz) {
            this.rebindDepth = rebindDepth;
            this.lightViewProj = lightViewProj;
            this.originX = ox;
            this.originY = oy;
            this.originZ = oz;
        }
    }

    private static ResourceLocation currentFillAlbedo;

    private AtomicShaderCompat() {
    }

    public static boolean isAtomicLoaded() {
        return Loader.isModLoaded(MODID);
    }

    public static boolean isAvailable() {
        return isAtomicLoaded() && AtomicGBufferCompat.isAvailable();
    }

    /** World entity fill OR first-person hand fill. */
    public static boolean isGBufferFillActive() {
        return isAvailable() && AtomicGBufferCompat.isGBufferFillActive();
    }

    public static boolean isShadowDepthActive() {
        return isAvailable() && AtomicGBufferCompat.isShadowDepthActive();
    }

    public static boolean isPipelineTakingOverExternalMeshes() {
        return isAvailable() && AtomicGBufferCompat.isPipelineTakingOverExternalMeshes();
    }

    /**
     * When Atomic owns deferred mesh: skip color draws outside fill/shadow
     * (avoids fullbright forward dual-paint).
     */
    public static boolean shouldSkipLegacyColorDraw() {
        if (!isAtomicLoaded()) {
            return false;
        }
        if (!isAvailable()) {
            return false;
        }
        if (isGBufferFillActive() || isShadowDepthActive()) {
            return false;
        }
        return true;
    }

    /** True when only opaque depth should be submitted (sun / custom-light mid-fill). */
    public static boolean shouldDrawShadowDepthOnly() {
        return isShadowDepthActive();
    }

    public static void rebindFillIfActive() {
        if (!isAvailable()) {
            return;
        }
        AtomicGBufferCompat.rebindFillIfActive();
    }

    public static void beginShadowDepthExternal(Runnable rebind, float[] lightViewProj, float originX,
            float originY, float originZ) {
        float[] vpCopy = null;
        if (lightViewProj != null && lightViewProj.length >= 16) {
            vpCopy = new float[16];
            System.arraycopy(lightViewProj, 0, vpCopy, 0, 16);
        }
        SHADOW_SESSION.set(new ShadowDepthSession(rebind, vpCopy, originX, originY, originZ));
    }

    public static void endShadowDepthExternal() {
        SHADOW_SESSION.remove();
    }

    public static void beginShadowDepthDrawBatch() {
        ShadowDepthSession s = SHADOW_SESSION.get();
        if (s == null) {
            return;
        }
        if (s.rebindDepth != null) {
            try {
                s.rebindDepth.run();
            } catch (Throwable ignored) {
            }
        }
        applyShadowFixedPipeline(s, true);
    }

    public static void restoreShadowFixedPipelineAfterMorph() {
        ShadowDepthSession s = SHADOW_SESSION.get();
        if (s == null) {
            return;
        }
        if (s.rebindDepth != null) {
            try {
                s.rebindDepth.run();
            } catch (Throwable ignored) {
            }
        }
        applyShadowFixedPipeline(s, false);
    }

    private static void applyShadowFixedPipeline(ShadowDepthSession s, boolean resetModelviewToOrigin) {
        GL20.glUseProgram(0);
        GlStateManager.matrixMode(5889);
        if (s.lightViewProj != null) {
            LIGHT_VP_UPLOAD.clear();
            LIGHT_VP_UPLOAD.put(s.lightViewProj, 0, 16);
            LIGHT_VP_UPLOAD.flip();
            GL11.glLoadMatrix(LIGHT_VP_UPLOAD);
        }
        GlStateManager.matrixMode(5888);
        if (resetModelviewToOrigin) {
            GlStateManager.loadIdentity();
            GlStateManager.translate(-s.originX, -s.originY, -s.originZ);
        }
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableLighting();
    }

    /** After glTF skin/morph compute: restore fill MRT or shadow fixed pipeline. */
    public static void rebindAtomicCaptureIfActive() {
        if (isShadowDepthActive()) {
            restoreShadowFixedPipelineAfterMorph();
            return;
        }
        rebindFillIfActive();
    }

    public static void setEmissiveFlat(float amount) {
        if (!isAvailable() || !isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        AtomicGBufferCompat.setEmissiveFlat(amount);
    }

    public static void setEmissiveMap(int glTexId, boolean tintAlbedo) {
        if (!isAvailable() || !isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        AtomicGBufferCompat.setEmissiveMap(glTexId, tintAlbedo);
    }

    public static void clearEmissive() {
        if (!isAvailable()) {
            return;
        }
        AtomicGBufferCompat.clearEmissive();
    }

    /**
     * After albedo {@code TextureManager.bindTexture} during Hand/Entity fill: rebind fill
     * then bind the same albedo again so Atomic {@code EntityPbrTextureCache.onAlbedoBound}
     * runs with {@code g_buffer_fill} active.
     * <p>
     * Atomic currently skips {@code onAlbedoBound} (and does not update {@code lastAlbedo})
     * when the fill program is not bound — common in FP after morph / FBO / {@code glUseProgram(0)}.
     * TP entity fill usually keeps the program; FP often does not. No new Atomic API required.
     */
    public static void ensurePbrMapsForBoundAlbedo(ResourceLocation albedo) {
        if (albedo == null || !isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        currentFillAlbedo = albedo;
        rebindFillIfActive();
        // EntityPbrTextureCache.uploadRgba may leave TEX0 unbound (bindTexture(0));
        // re-bind albedo with fill program active so _n/_s hot-swap runs, then restore filter.
        Minecraft.getMinecraft().getTextureManager().bindTexture(albedo);
        TextureSamplingRegistry.restoreAlbedoSampling(albedo);
    }

    /**
     * After morph / FBO steal: restore fill MRT <b>and</b> re-bind the last gun albedo so
     * {@code EntityPbrTextureCache} re-applies {@code _n/_s} (reapplyLast alone is not enough
     * when lastAlbedo was overwritten by hand skin / glow probes).
     */
    public static void rebindFillAndGunPbr() {
        if (!isGBufferFillActive() && !isShadowDepthActive()) {
            return;
        }
        rebindAtomicCaptureIfActive();
        TextureSamplingRegistry.restoreDefaultTexUnit();
        if (currentFillAlbedo != null && isGBufferFillActive() && !isShadowDepthActive()) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(currentFillAlbedo);
            TextureSamplingRegistry.restoreAlbedoSampling(currentFillAlbedo);
        }
    }

    public static void clearCurrentFillAlbedo() {
        currentFillAlbedo = null;
    }

    /**
     * Soft alpha FX (muzzle flash / smoke) must not {@code enableBlend} into Hand/Entity MRT:
     * SrcA blend fades RGB into clear(0) → black fringes. Use replace + alpha discard instead.
     */
    public static void beginCutoutEmissiveFx() {
        if (!isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        beginFlatEmissiveHighlight();
    }

    public static void endCutoutEmissiveFx() {
        endFlatEmissiveHighlight();
        TextureSamplingRegistry.restoreDefaultTexUnit();
        if (currentFillAlbedo != null) {
            TextureSamplingRegistry.restoreAlbedoSampling(currentFillAlbedo);
        }
    }

    /**
     * If Atomic fill is active and a glow map exists, bind emissive on TEX4 and return true
     * (caller draws albedo once, then {@link #clearEmissive()}). Otherwise return false for legacy pass.
     */
    public static boolean prepareGlowMapEmissive(String type, String fileName) {
        if (!isGBufferFillActive() || isShadowDepthActive() || type == null || fileName == null) {
            return false;
        }
        int id = resolveGlowGlTextureId(type, fileName);
        if (id <= 0) {
            return false;
        }
        setEmissiveMap(id, true);
        TextureSamplingRegistry.restoreDefaultTexUnit();
        return true;
    }

    /**
     * Resolve {@code skins/<type>/<file>_glow.png} GL id without leaving it on TEX0.
     * Does <b>not</b> eager-load missing glows (avoids SimpleTexture side effects / filter leaks).
     */
    public static int resolveGlowGlTextureId(String type, String fileName) {
        ResourceLocation loc = new ResourceLocation(com.modularwarfare.ModularWarfare.MOD_ID,
                String.format("skins/%s/%s_glow.png", type, fileName));
        Minecraft mc = Minecraft.getMinecraft();
        ITextureObject tex = mc.getTextureManager().getTexture(loc);
        if (tex == null || tex == TextureUtil.MISSING_TEXTURE) {
            return 0;
        }
        try {
            if (tex.getGlTextureId() == TextureUtil.MISSING_TEXTURE.getGlTextureId()) {
                return 0;
            }
        } catch (Throwable t) {
            return 0;
        }
        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int prevTex0 = 0;
        int prevTex5 = 0;
        try {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GlStateManager.setActiveTexture(GL13.GL_TEXTURE5);
            prevTex5 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GlStateManager.bindTexture(tex.getGlTextureId());
            return tex.getGlTextureId();
        } catch (Throwable t) {
            return 0;
        } finally {
            try {
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE5);
                GlStateManager.bindTexture(prevTex5);
            } catch (Throwable ignored) {
            }
            try {
                OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.bindTexture(prevTex0);
            } catch (Throwable ignored) {
            }
            try {
                OpenGlHelper.setActiveTexture(prevActive);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Flat emissive for flash / panel style fullbright parts during fill. */
    public static void beginFlatEmissiveHighlight() {
        setEmissiveFlat(1f);
    }

    public static void endFlatEmissiveHighlight() {
        clearEmissive();
    }
}
