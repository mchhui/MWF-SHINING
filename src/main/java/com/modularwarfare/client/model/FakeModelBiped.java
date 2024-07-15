package com.modularwarfare.client.model;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;

import mchhui.modularmovements.tactical.client.ClientLitener;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FakeModelBiped extends ModelBiped {
    public FakeModelBiped() {
        super(0.0F);
    }

    public FakeModelBiped(float modelSize) {
        super(modelSize, 0.0F, 64, 32);
    }

    public FakeModelBiped(float modelSize, float p_i1149_2_, int textureWidthIn, int textureHeightIn) {
        super(modelSize, p_i1149_2_, textureWidthIn, textureHeightIn);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, float scaleFactor, Entity entityIn) {
        // TODO Auto-generated method stub
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        if(ModularWarfare.isLoadedModularMovements) {
            ClientLitener.setRotationAngles(this, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                scaleFactor, entityIn);  
        }
        if (this.rightArmPose == ModelBiped.ArmPose.BOW_AND_ARROW)
        {
            this.bipedRightArm.rotateAngleY = -0.1F + this.bipedHead.rotateAngleY;
            this.bipedRightArm.rotateAngleX = -((float)Math.PI / 2F) + this.bipedHead.rotateAngleX;
            this.bipedLeftArm.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY + 0.4F;
            this.bipedLeftArm.rotateAngleX = -((float)Math.PI / 2F) + this.bipedHead.rotateAngleX;
            ItemStack itemstack = ((EntityLivingBase)entityIn).getHeldItemMainhand();
            if (itemstack != ItemStack.EMPTY && !itemstack.isEmpty()) {
                if (itemstack.getItem() instanceof ItemGun) {
                    BaseType type = ((BaseItem) itemstack.getItem()).baseType;
                    if (type.hasModel()) {
                        GunEnhancedRenderConfig config = (GunEnhancedRenderConfig)type.enhancedModel.config;
                        if(config.renderOffhandPart) {
                            this.bipedLeftArm.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY;
                            this.bipedLeftArm.rotateAngleX = -((float)Math.PI / 2F) + this.bipedHead.rotateAngleX;
                        }
                    }
                }
            }
        }
    }
}
