package com.modularwarfare.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.modularwarfare.client.ClientRenderHooks;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Apply ride upper-body yaw after vanilla {@code setRotationAngles}.
 */
@SideOnly(Side.CLIENT)
@Mixin(ModelBiped.class)
public class MixinModelBiped {

    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    private void mwf$applyRideUpperBodyTwist(float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
        if ((Object) this instanceof net.minecraft.client.model.ModelPlayer) {
            return;
        }
        ClientRenderHooks.applyRideUpperBodyTwist((ModelBiped) (Object) this, entityIn);
    }
}
