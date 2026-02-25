package com.modularwarfare.client.model;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FakeLayerBipedArmor extends LayerBipedArmor {
    private final RenderLivingBase<?> renderer;

    public FakeLayerBipedArmor(RenderLivingBase<?> rendererIn) {
        super(rendererIn);
        renderer = rendererIn;
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void initArmor() {
        this.modelLeggings = new FakeModelBiped(0.5F);
        this.modelArmor = new FakeModelBiped(1.0F);
    }

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        boolean flag = ModelCustomArmor.translucentBatch;
        boolean flag1 = ModelCustomArmor.needTranslucentBatchBuf;
        ModelCustomArmor.translucentBatch = false;
        ModelCustomArmor.needTranslucentBatchBuf = false;
        super.doRenderLayer(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
        if (ModelCustomArmor.needTranslucentBatchBuf) {
            ModelCustomArmor.translucentBatch = true;
            super.doRenderLayer(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
        }
        ModelCustomArmor.translucentBatch = flag;
        ModelCustomArmor.needTranslucentBatchBuf = flag1;
    }
}
