package com.modularwarfare.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import cloud.siz.atomic.api.render.AtomicForwardLightingApi;
import cloud.siz.atomic.api.render.AtomicGBufferCompat;

/**
 * Soft-depend helpers for SIZ Atomic Shader G-buffer / shadow / emissive.
 * Mirrors HE {@code AtomicShaderCompat}; MWF already compileOnly-depends on the Atomic API jar.
 */
@SideOnly(Side.CLIENT)
public final class AtomicShaderCompat {

    public static final String MODID = "siz_atomicshader";

    private static ResourceLocation currentFillAlbedo;

    private static final java.util.HashMap<String, Integer> GLOW_GL_ID_CACHE = new java.util.HashMap<>();
    private static boolean atomicLoadedResolved;
    private static boolean atomicLoaded;

    private AtomicShaderCompat() {
    }

    public static void clearGlowMapCache() {
        GLOW_GL_ID_CACHE.clear();
    }

    public static boolean isAtomicLoaded() {
        if (!atomicLoadedResolved) {
            atomicLoaded = Loader.isModLoaded(MODID);
            atomicLoadedResolved = true;
        }
        return atomicLoaded;
    }

    public static boolean isAvailable() {
        return isAtomicLoaded() && AtomicGBufferCompat.isAvailable();
    }

    /** Deferred master switch — when false, all Atomic compat must use vanilla MWF paths. */
    public static boolean isPipelineEnabled() {
        return isAvailable() && AtomicGBufferCompat.isPipelineEnabled();
    }

    /** World entity fill OR first-person hand fill (requires master on). */
    public static boolean isGBufferFillActive() {
        return isPipelineEnabled() && AtomicGBufferCompat.isGBufferFillActive();
    }

    public static boolean isShadowDepthActive() {
        return isPipelineEnabled() && AtomicGBufferCompat.isShadowDepthActive();
    }

    public static boolean isPipelineTakingOverExternalMeshes() {
        return isPipelineEnabled() && AtomicGBufferCompat.isPipelineTakingOverExternalMeshes();
    }

    /** Skip forward color when Atomic owns deferred mesh (except open GUIs). */
    public static boolean shouldSkipLegacyColorDraw() {
        if (!isPipelineEnabled()) {
            return false;
        }
        if (isGBufferFillActive() || isShadowDepthActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.currentScreen != null) {
            return false;
        }
        return true;
    }

    /** True when only opaque depth should be submitted (sun / custom-light mid-fill). */
    public static boolean shouldDrawShadowDepthOnly() {
        return isShadowDepthActive();
    }

    /** TEX1 lightmap + MultiTexCoord1; call before final TEX0 albedo bind. */
    public static void ensureFillLightmapState() {
        if (!isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.entityRenderer != null) {
            try {
                mc.entityRenderer.enableLightmap();
            } catch (Throwable ignored) {
            }
        }
        OpenGlHelper.setLightmapTextureCoords(
                OpenGlHelper.lightmapTexUnit,
                OpenGlHelper.lastBrightnessX,
                OpenGlHelper.lastBrightnessY);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }

    public static void rebindFillIfActive() {
        if (!isPipelineEnabled()) {
            return;
        }
        AtomicGBufferCompat.rebindFillIfActive();
    }

    /**
     * Upload ParticleForward-equivalent sky + custom lights onto a bound peer program.
     * {@code false} when Atomic is absent / master off — keep vanilla lightmap.
     */
    public static boolean applyForwardLighting(int programId) {
        if (!isForwardLightingActive() || programId <= 0) {
            return false;
        }
        AtomicForwardLightingApi.applyToBoundProgram(programId);
        return true;
    }

    /** True when peers should bind Atomic sky + custom-light uniforms instead of vanilla lightmap. */
    public static boolean isForwardLightingActive() {
        return isPipelineEnabled() && AtomicForwardLightingApi.isActive();
    }

    /** Force next fill rebind after raw {@code GL20.glUseProgram} changes. */
    public static void markFillCaptureDirty() {
        if (!isPipelineEnabled()) {
            return;
        }
        AtomicGBufferCompat.markFillCaptureDirty();
    }

    /** Warm decoded PBR cache for an albedo location. */
    public static void warmStandalonePbrMaps(ResourceLocation albedo) {
        if (!isAtomicLoaded() || albedo == null) {
            return;
        }
        AtomicGBufferCompat.warmStandalonePbrMaps(albedo);
    }

    /**
     * After morph during entity-sun / custom shadow fill via {@code renderEntity}:
     * drop compute/skin programs so fixed-pipeline depth writes work.
     */
    public static void restoreShadowFixedPipelineAfterMorph() {
        GL20.glUseProgram(0);
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
        if (!isPipelineEnabled() || !isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        AtomicGBufferCompat.setEmissiveFlat(amount);
    }

    public static void setEmissiveMap(int glTexId, boolean tintAlbedo) {
        if (!isPipelineEnabled() || !isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        AtomicGBufferCompat.setEmissiveMap(glTexId, tintAlbedo);
    }

    public static void clearEmissive() {
        if (!isPipelineEnabled()) {
            return;
        }
        AtomicGBufferCompat.clearEmissive();
    }

    /** FP soft FX window: finish Hand MRT + light. No-op when Atomic off / master off. */
    public static void finishHandDeferredIfActive() {
        if (!isPipelineEnabled()) {
            return;
        }
        AtomicGBufferCompat.finishHandDeferredIfActive();
    }

    /**
     * After albedo {@code TextureManager.bindTexture} during Hand/Entity fill: rebind fill
     * then bind the same albedo again so Atomic {@code EntityPbrTextureCache.onAlbedoBound}
     * runs with {@code g_buffer_fill} active.
     * <p>
     * Skips work when fill is already bound and this albedo is already the current fill albedo.
     */
    public static void ensurePbrMapsForBoundAlbedo(ResourceLocation albedo) {
        if (albedo == null || !isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        if (mchhui.hegltf.GltfFeatureFlags.renderSchedulingOpt()
                && albedo.equals(currentFillAlbedo) && AtomicGBufferCompat.isFillProgramBound()) {
            return;
        }
        currentFillAlbedo = albedo;
        rebindFillIfActive();
        Minecraft.getMinecraft().getTextureManager().bindTexture(albedo);
        TextureSamplingRegistry.restoreAlbedoSampling(albedo);
    }

    /**
     * Restore fill MRT and re-bind {@code currentFillAlbedo}.
     * Call only when that albedo matches the mesh about to draw.
     */
    public static void rebindFillAndGunPbr() {
        if (!isGBufferFillActive() && !isShadowDepthActive()) {
            return;
        }
        if (isGBufferFillActive()) {
            markFillCaptureDirty();
        }
        rebindAtomicCaptureIfActive();
        if (isGBufferFillActive()) {
            ensureFillLightmapState();
        }
        TextureSamplingRegistry.restoreDefaultTexUnit();
        if (currentFillAlbedo != null && isGBufferFillActive() && !isShadowDepthActive()) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(currentFillAlbedo);
            TextureSamplingRegistry.restoreAlbedoSampling(currentFillAlbedo);
        }
    }

    public static void clearCurrentFillAlbedo() {
        currentFillAlbedo = null;
    }

    /** Prefer registered player skin; else default Steve/Alex. */
    public static ResourceLocation resolveReadyPlayerSkin(AbstractClientPlayer player) {
        if (player == null) {
            return DefaultPlayerSkin.getDefaultSkinLegacy();
        }
        ResourceLocation loc = player.getLocationSkin();
        if (loc == null) {
            return DefaultPlayerSkin.getDefaultSkin(player.getUniqueID());
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getTextureManager() == null) {
            return DefaultPlayerSkin.getDefaultSkin(player.getUniqueID());
        }
        ITextureObject tex = mc.getTextureManager().getTexture(loc);
        if (tex == null || isMissingTexture(tex)) {
            return DefaultPlayerSkin.getDefaultSkin(player.getUniqueID());
        }
        return loc;
    }

    private static boolean isMissingTexture(ITextureObject tex) {
        if (tex == TextureUtil.MISSING_TEXTURE) {
            return true;
        }
        try {
            return tex.getGlTextureId() == TextureUtil.MISSING_TEXTURE.getGlTextureId();
        } catch (Throwable t) {
            return true;
        }
    }

    /** Bind ready player skin; PBR fill hooks when pipeline is active. */
    public static ResourceLocation bindReadyPlayerSkin() {
        AbstractClientPlayer player = Minecraft.getMinecraft().player;
        ResourceLocation skin = resolveReadyPlayerSkin(player);
        Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
        if (isPipelineEnabled() && isGBufferFillActive() && !isShadowDepthActive()) {
            ensurePbrMapsForBoundAlbedo(skin);
        }
        return skin;
    }

    /**
     * Call at the start of each FP held-item draw to drop stale gun/skin albedo pointers.
     * No-op when Atomic is absent or the deferred master switch is off.
     */
    public static void onFirstPersonItemBegin() {
        if (!isPipelineEnabled()) {
            return;
        }
        clearCurrentFillAlbedo();
        clearEmissive();
    }

    /**
     * Bind an albedo for the current fill mesh and adopt it as {@code currentFillAlbedo}.
     * Clears leftover emissive so glow from a previous item cannot tint the next mesh.
     */
    public static void bindFillAlbedo(ResourceLocation albedo) {
        if (albedo == null) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(albedo);
        if (isGBufferFillActive() && !isShadowDepthActive()) {
            clearEmissive();
            ensurePbrMapsForBoundAlbedo(albedo);
        } else {
            TextureSamplingRegistry.restoreAlbedoSampling(albedo);
        }
    }

    /**
     * Start opaque fill capture: disable blend, restore fill + albedo PBR.
     * Bind mesh albedo before calling.
     */
    public static void beginOpaqueFillCapture() {
        if (!isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        rebindFillAndGunPbr();
    }

    /**
     * After an opaque mesh (or morph) during fill: restore MRT + gun/item PBR bindings.
     */
    public static void afterOpaqueMesh() {
        if (!isGBufferFillActive() && !isShadowDepthActive()) {
            return;
        }
        rebindFillAndGunPbr();
    }

    /** Soft FX on fill: replace + alpha test (no SrcA blend into MRT). */
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
        if (!isPipelineEnabled()) {
            return;
        }
        endFlatEmissiveHighlight();
        TextureSamplingRegistry.restoreDefaultTexUnit();
        if (currentFillAlbedo != null) {
            TextureSamplingRegistry.restoreAlbedoSampling(currentFillAlbedo);
        }
    }

    /** Bind glow map on TEX4 during fill; {@code false} → use legacy glow path. */
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
     * Returns {@code 0} if absent.
     */
    public static int resolveGlowGlTextureId(String type, String fileName) {
        if (type == null || fileName == null) {
            return 0;
        }
        String cacheKey = type + "/" + fileName;
        Integer cached = GLOW_GL_ID_CACHE.get(cacheKey);
        if (cached != null) {
            return cached.intValue();
        }
        ResourceLocation loc = new ResourceLocation(com.modularwarfare.ModularWarfare.MOD_ID,
                String.format("skins/%s/%s_glow.png", type, fileName));
        Minecraft mc = Minecraft.getMinecraft();
        ITextureObject tex = mc.getTextureManager().getTexture(loc);
        if (tex == null || tex == TextureUtil.MISSING_TEXTURE) {
            try {
                mc.getResourceManager().getResource(loc);
                mc.getTextureManager().bindTexture(loc);
                tex = mc.getTextureManager().getTexture(loc);
            } catch (Throwable t) {
                GLOW_GL_ID_CACHE.put(cacheKey, Integer.valueOf(0));
                return 0;
            }
        }
        if (tex == null || tex == TextureUtil.MISSING_TEXTURE) {
            GLOW_GL_ID_CACHE.put(cacheKey, Integer.valueOf(0));
            return 0;
        }
        try {
            if (tex.getGlTextureId() == TextureUtil.MISSING_TEXTURE.getGlTextureId()) {
                GLOW_GL_ID_CACHE.put(cacheKey, Integer.valueOf(0));
                return 0;
            }
        } catch (Throwable t) {
            GLOW_GL_ID_CACHE.put(cacheKey, Integer.valueOf(0));
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
            int id = tex.getGlTextureId();
            GLOW_GL_ID_CACHE.put(cacheKey, Integer.valueOf(id));
            return id;
        } catch (Throwable t) {
            GLOW_GL_ID_CACHE.put(cacheKey, Integer.valueOf(0));
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
