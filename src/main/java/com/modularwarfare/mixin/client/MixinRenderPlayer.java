package com.modularwarfare.mixin.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.modularwarfare.core.MWFCoreHooks;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;

@SideOnly(Side.CLIENT)
@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {
    /**
     * @author KomiMoe
     * @reason I don't know, but must a doc
     */
    @Overwrite
    protected void renderLivingAt(AbstractClientPlayer entityLivingBaseIn, double x, double y, double z) {
        MWFCoreHooks.renderLivingAtForRenderPlayer(entityLivingBaseIn, x, y, z);
    }
}
