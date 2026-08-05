package com.modularwarfare.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers MWF skin/effect sampling (NEAREST vs LINEAR) and re-applies it after Atomic
 * PBR hot-swap / multi-tex-unit binds, which can leave the wrong filter on the albedo
 * or leave a non-TEX0 unit active (leaking into fire / particles).
 * <p>
 * {@code glTexParameteri} is sticky on the GL texture object — never call
 * {@link #applyBoundFilter} unless that albedo is currently bound on TEX0. After MWF draws,
 * call {@link #restoreVanillaBlocksAtlasSampling()} so the blocks atlas is not left LINEAR.
 */
@SideOnly(Side.CLIENT)
public final class TextureSamplingRegistry {

    private static final ConcurrentHashMap<ResourceLocation, Boolean> LINEAR =
            new ConcurrentHashMap<>();

    private TextureSamplingRegistry() {
    }

    public static void register(ResourceLocation loc, boolean linear) {
        if (loc != null) {
            LINEAR.put(loc, linear);
        }
    }

    public static boolean has(ResourceLocation loc) {
        return loc != null && LINEAR.containsKey(loc);
    }

    public static void registerIfAbsent(ResourceLocation loc, boolean linear) {
        if (loc != null) {
            LINEAR.putIfAbsent(loc, linear);
        }
    }

    public static void applyBoundFilter(boolean linear) {
        // Sticky on the currently bound TEX0 object — caller must bind the intended albedo first.
        int filter = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
    }

    /**
     * Bind albedo on TEX0 (without TextureManager mixin re-entry) and restore registered filter.
     */
    public static void restoreAlbedoSampling(ResourceLocation albedo) {
        if (albedo == null) {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            return;
        }
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        ITextureObject tex = Minecraft.getMinecraft().getTextureManager().getTexture(albedo);
        if (tex != null) {
            GlStateManager.bindTexture(tex.getGlTextureId());
            Boolean linear = LINEAR.get(albedo);
            if (linear != null) {
                applyBoundFilter(linear);
            }
        }
    }

    /** Force TEX0 active so later vanilla/Atomic binds do not hit TEX2–TEX5. */
    public static void restoreDefaultTexUnit() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Reset vanilla blocks-atlas filter/mipmap after MWF LINEAR skins or multi-unit binds may
     * have sticky-{@code glTexParameteri}'d the atlas (blurry/black fire, seamed water).
     * Uses sprite mode ({@code setBlurMipmap(false,false)}) — safe for fire overlays; Atomic
     * restores world mip settings again before water.
     */
    public static void restoreVanillaBlocksAtlasSampling() {
        restoreDefaultTexUnit();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getTextureManager() == null) {
            return;
        }
        try {
            GlStateManager.enableTexture2D();
            ITextureObject tex =
                    mc.getTextureManager()
                            .getTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
            if (tex != null) {
                tex.setBlurMipmap(false, false);
            }
        } catch (Throwable ignored) {
        }
    }
}
