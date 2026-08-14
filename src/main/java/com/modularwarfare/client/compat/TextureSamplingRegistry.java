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
 * Remembers NEAREST vs LINEAR per albedo and re-applies after Atomic PBR binds.
 * {@code glTexParameteri} is sticky — call {@link #applyBoundFilter} only when albedo is on TEX0.
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

    /** Bind albedo on TEX0 and restore registered filter. Unregistered locations use NEAREST. */
    public static void restoreAlbedoSampling(ResourceLocation albedo) {
        if (albedo == null) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            return;
        }
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        ITextureObject tex = Minecraft.getMinecraft().getTextureManager().getTexture(albedo);
        if (tex != null) {
            GlStateManager.bindTexture(tex.getGlTextureId());
            applyBoundFilter(Boolean.TRUE.equals(LINEAR.get(albedo)));
        }
    }

    /** Force TEX0 active so later vanilla/Atomic binds do not hit TEX2–TEX5. */
    public static void restoreDefaultTexUnit() {
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Vanilla entity/player skins: pixel art, never linear. Call after any path that may have
     * left {@code GL_LINEAR} on the skin texture object.
     */
    public static void forceNearestOnBoundTex0() {
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        applyBoundFilter(false);
    }

    /** Reset blocks atlas filter/mipmap after MWF skin binds. */
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
