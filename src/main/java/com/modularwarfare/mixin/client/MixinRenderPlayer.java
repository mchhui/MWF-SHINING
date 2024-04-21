package com.modularwarfare.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.SoftOverride;

import com.modularwarfare.ModConfig;
import com.modularwarfare.core.MWFCoreHooks;
import com.modularwarfare.utility.OptifineHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.MWFOptifineShadesHelper;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {
    @Overwrite
    protected void renderLivingAt(EntityLivingBase entityLivingBaseIn, double x, double y, double z) {
        MWFCoreHooks.renderLivingAtForRenderPlayer(entityLivingBaseIn, x, y, z);
    }
}
