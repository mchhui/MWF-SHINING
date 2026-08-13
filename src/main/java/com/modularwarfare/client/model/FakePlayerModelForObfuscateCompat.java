package com.modularwarfare.client.model;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import com.mrcrayfish.obfuscate.client.event.ModelPlayerEvent;

import mchhui.modularmovements.tactical.client.ClientListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FakePlayerModelForObfuscateCompat extends ModelPlayer {
    private boolean smallArms;

    public FakePlayerModelForObfuscateCompat(float modelSize, boolean smallArmsIn) {
        super(modelSize, smallArmsIn);
        this.smallArms=smallArmsIn;
    }

    public void setRotationAnglesMWF(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, float scaleFactor, Entity entityIn) {
        // TODO Auto-generated method stub
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        if(ModularWarfare.isLoadedModularMovements) {
            ClientListener.setRotationAngles(this, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
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
                        if (((GunType)type).animationType.equals(WeaponAnimationType.ENHANCED)) {
                            GunEnhancedRenderConfig config = (GunEnhancedRenderConfig)type.enhancedModel.config;
                            if(config.renderOffhandPart) {
                                this.bipedLeftArm.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY;
                                this.bipedLeftArm.rotateAngleX = -((float)Math.PI / 2F) + this.bipedHead.rotateAngleX;
                                this.bipedLeftArmwear.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY;
                                this.bipedLeftArmwear.rotateAngleX = -((float)Math.PI / 2F) + this.bipedHead.rotateAngleX;
                            }
                        }
                    }
                }
            }
        }
    }
    

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn)
    {
        this.resetRotationAngles();
        if(!MinecraftForge.EVENT_BUS.post(new ModelPlayerEvent.SetupAngles.Pre((EntityPlayer) entityIn, this, Minecraft.getMinecraft().getRenderPartialTicks())))
        {
            this.setRotationAnglesMWF(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
            MinecraftForge.EVENT_BUS.post(new ModelPlayerEvent.SetupAngles.Post((EntityPlayer) entityIn, this, Minecraft.getMinecraft().getRenderPartialTicks()));
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
                            if (((GunType)type).animationType.equals(WeaponAnimationType.ENHANCED)) {
                                GunEnhancedRenderConfig config = (GunEnhancedRenderConfig)type.enhancedModel.config;
                                if(config.renderOffhandPart) {
                                    this.bipedLeftArm.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY;
                                    this.bipedLeftArm.rotateAngleX = -((float)Math.PI / 2F) + this.bipedHead.rotateAngleX;
                                    this.bipedLeftArmwear.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY;
                                    this.bipedLeftArmwear.rotateAngleX = -((float)Math.PI / 2F) + this.bipedHead.rotateAngleX;
                                }
                            }
                        }
                    }
                }
            }
        }
        this.setupRotationAngles();
        // After Vehicle handlebars + BOW restore: ride aim upper-body twist (authoritative).
        ClientRenderHooks.applyRideUpperBodyTwist(this, entityIn);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        this.resetVisibilities();
        if(!MinecraftForge.EVENT_BUS.post(new ModelPlayerEvent.Render.Pre((EntityPlayer) entityIn, this, Minecraft.getMinecraft().getRenderPartialTicks())))
        {
            super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            MinecraftForge.EVENT_BUS.post(new ModelPlayerEvent.Render.Post((EntityPlayer) entityIn, this, Minecraft.getMinecraft().getRenderPartialTicks()));
        }
    }

    private void setupRotationAngles()
    {
        copyModelAngles(bipedRightArm, bipedRightArmwear);
        copyModelAngles(bipedLeftArm, bipedLeftArmwear);
        copyModelAngles(bipedRightLeg, bipedRightLegwear);
        copyModelAngles(bipedLeftLeg, bipedLeftLegwear);
        copyModelAngles(bipedBody, bipedBodyWear);
        copyModelAngles(bipedHead, bipedHeadwear);
    }

    private void resetRotationAngles()
    {
        this.resetAll(bipedHead);
        this.resetAll(bipedHeadwear);
        this.resetAll(bipedBody);
        this.resetAll(bipedBodyWear);

        this.resetAll(bipedRightArm);
        bipedRightArm.rotationPointX = -5.0F;
        bipedRightArm.rotationPointY = smallArms ? 2.5F : 2.0F;
        bipedRightArm.rotationPointZ = 0.0F;

        this.resetAll(bipedRightArmwear);
        bipedRightArmwear.rotationPointX = -5.0F;
        bipedRightArmwear.rotationPointY = smallArms ? 2.5F : 2.0F;
        bipedRightArmwear.rotationPointZ = 10.0F;

        this.resetAll(bipedLeftArm);
        bipedLeftArm.rotationPointX = 5.0F;
        bipedLeftArm.rotationPointY = smallArms ? 2.5F : 2.0F;
        bipedLeftArm.rotationPointZ = 0.0F;

        this.resetAll(bipedLeftArmwear);
        bipedLeftArmwear.rotationPointX = 5.0F;
        bipedLeftArmwear.rotationPointY = smallArms ? 2.5F : 2.0F;
        bipedLeftArmwear.rotationPointZ = 0.0F;

        this.resetAll(bipedLeftLeg);
        bipedLeftLeg.rotationPointX = 1.9F;
        bipedLeftLeg.rotationPointY = 12.0F;
        bipedLeftLeg.rotationPointZ = 0.0F;

        this.resetAll(bipedLeftLegwear);
        copyModelAngles(bipedLeftLeg, bipedLeftLegwear);

        this.resetAll(bipedRightLeg);
        bipedRightLeg.rotationPointX = -1.9F;
        bipedRightLeg.rotationPointY = 12.0F;
        bipedRightLeg.rotationPointZ = 0.0F;

        this.resetAll(bipedRightLegwear);
        copyModelAngles(bipedRightLeg, bipedRightLegwear);
    }

    private void resetAll(ModelRenderer renderer)
    {
        renderer.offsetX = 0.0F;
        renderer.offsetY = 0.0F;
        renderer.offsetZ = 0.0F;
        renderer.rotateAngleX = 0.0F;
        renderer.rotateAngleY = 0.0F;
        renderer.rotateAngleZ = 0.0F;
        renderer.rotationPointX = 0.0F;
        renderer.rotationPointY = 0.0F;
        renderer.rotationPointZ = 0.0F;
    }

    private void resetVisibilities()
    {
        this.bipedHead.isHidden = false;
        this.bipedBody.isHidden = false;
        this.bipedRightArm.isHidden = false;
        this.bipedLeftArm.isHidden = false;
        this.bipedRightLeg.isHidden = false;
        this.bipedLeftLeg.isHidden = false;
    }
}
