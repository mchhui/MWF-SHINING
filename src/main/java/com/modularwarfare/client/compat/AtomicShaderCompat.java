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

import cloud.siz.atomic.api.render.AtomicGBufferCompat;

/**
 * Soft-depend helpers for SIZ Atomic Shader G-buffer / shadow / emissive.
 * Mirrors HE {@code AtomicShaderCompat}; MWF already compileOnly-depends on the Atomic API jar.
 */
@SideOnly(Side.CLIENT)
public final class AtomicShaderCompat {

    public static final String MODID = "siz_atomicshader";

    private static ResourceLocation currentFillAlbedo;

    private AtomicShaderCompat() {
    }

    public static boolean isAtomicLoaded() {
        return Loader.isModLoaded(MODID);
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

    /**
     * When Atomic owns deferred mesh: skip color draws outside fill/shadow
     * (avoids fullbright forward dual-paint). When master is off, never skip.
     * <p>
     * Inventory / container entity previews ({@code GuiInventory.drawEntityOnScreen}
     * etc.) run after the world fill session with no fill active — must not skip
     * or held MWF guns (and ELM-bound previews' weapons) stay invisible.
     */
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

    /**
     * Bind vanilla lightmap on TEX1 and restamp {@code gl_MultiTexCoord1}.
     * <p>
     * Must run <b>before</b> the final TEX0 albedo bind. {@code enableLightmap} can steal TEX0
     * when GlStateManager active-texture is desynced — callers must bind gun/skin albedo after.
     * Do not call this after {@link #rebindFillAndGunPbr} returns.
     */
    public static void ensureFillLightmapState() {
        if (!isGBufferFillActive() || isShadowDepthActive()) {
            return;
        }
        // Sync GM ↔ GL before enableLightmap so the lightmap atlas cannot land on TEX0.
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
        // Keep TEX0 UV matrix identity (lightmap scale belongs on TEX1 only).
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
        // Capture rebind only — lightmap+albedo ordering is owned by rebindFillAndGunPbr /
        // HandDeferredPass (enableLightmap must not run after TEX0 albedo is set).
        AtomicGBufferCompat.rebindFillIfActive();
    }

    /**
     * Skin / morph / raw GL20 program steals: Atomic FillCaptureGuard may not see HE-LWJGL3
     * {@code GL20.glUseProgram}; force the next fill rebind to run.
     */
    public static void markFillCaptureDirty() {
        if (!isPipelineEnabled()) {
            return;
        }
        AtomicGBufferCompat.markFillCaptureDirty();
    }

    /**
     * After TextureManager preload of gun/item albedo (+ {@code _n}/{@code _s}): warm Atomic's
     * decoded PBR cache so first FP draw does not ImageIO/decode on the render thread.
     */
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
        // EntityPbrTextureCache.uploadRgba may leave TEX0 unbound (bindTexture(0));
        // re-bind albedo with fill program active so _n/_s hot-swap runs, then restore filter.
        Minecraft.getMinecraft().getTextureManager().bindTexture(albedo);
        TextureSamplingRegistry.restoreAlbedoSampling(albedo);
    }

    /**
     * After morph / FBO steal: restore fill MRT and re-bind {@code currentFillAlbedo} (gun / item /
     * armor / skin — whatever the peer last adopted). Not a gun-only helper despite the name.
     * Call only when that albedo is still the mesh about to draw; never use this to "fix" arms
     * after a stale held-item albedo (bind skin via {@link #bindFillAlbedo} instead).
     * <p>
     * Always restores capture when called — HE/MWF may change program via {@code GL20.glUseProgram}
     * without going through OpenGlHelper; do not skip based on a Java fill-bound flag alone.
     */
    public static void rebindFillAndGunPbr() {
        if (!isGBufferFillActive() && !isShadowDepthActive()) {
            return;
        }
        // Always dirty: peers may steal program via raw GL20 without FillCaptureGuard noticing.
        // Skinned guns already mark dirty in skinFromPose; non-skinned must match that path.
        if (isGBufferFillActive()) {
            markFillCaptureDirty();
        }
        rebindAtomicCaptureIfActive();
        // After PBR unit hops: bind lightmap on TEX1 + stamp MultiTexCoord1, then TEX0 albedo.
        // Never enableLightmap after the albedo bind below.
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

    /**
     * Player skin location that is safe to sample. When the network skin is not registered yet
     * or resolves to {@link TextureUtil#MISSING_TEXTURE} (purple), fall back to
     * {@link DefaultPlayerSkin} — enter-world / switch-save FP arms otherwise go purple.
     */
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

    /**
     * Bind a ready player skin (or Steve/Alex default). Atomic PBR fill hooks only when
     * pipeline master is on; otherwise this is a plain TextureManager bind (MWF purple-skin fix).
     */
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
     * Start opaque mesh capture while Atomic Hand/Entity fill is active: disable soft blend
     * (SrcA into MRT → black fringes) and restore fill + last albedo PBR maps.
     * Soft translucent layers must use {@code finishHandDeferredIfActive} (FP) or
     * {@code AtomicExternalDrawEvent.EntityForwardOverlay} after composite — not this path.
     * <p>
     * Call {@link #bindFillAlbedo} / {@link #ensurePbrMapsForBoundAlbedo} for the mesh about to
     * draw <b>before</b> this, or {@link #rebindFillAndGunPbr} will restore a stale item albedo.
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
        if (!isPipelineEnabled()) {
            return;
        }
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
     * Loads the texture on first use when the resource exists (preload may have skipped GLOW).
     */
    public static int resolveGlowGlTextureId(String type, String fileName) {
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
                return 0;
            }
        }
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
