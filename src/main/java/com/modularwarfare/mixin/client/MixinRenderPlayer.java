package com.modularwarfare.mixin.client;

import com.modularwarfare.ModConfig;
import com.modularwarfare.utility.OptifineHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.MWFOptifineShadesHelper;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {
    @Overwrite
    protected void renderLivingAt(EntityLivingBase entityLivingBaseIn, double x, double y, double z) {
        GlStateManager.translate((float) x, (float) y, (float) z);
        if (OptifineHelper.isShadersEnabled()) {
            if (Shaders.isShadowPass && MWFOptifineShadesHelper.getPreShadowPassThirdPersonView() == 0) {
                if (entityLivingBaseIn == Minecraft.getMinecraft().player) {
                    Vec3d vec = new Vec3d(0, 0, -ModConfig.INSTANCE.general.playerShadowOffset);
                    vec = vec.rotateYaw((float) Math.toRadians(-Minecraft.getMinecraft().player.rotationYaw));
                    GlStateManager.translate(vec.x, vec.y, vec.z);
                }
            }
        }
    }
}
