package com.modularwarfare.client.model;

import com.modularwarfare.client.compat.ArmorTranslucentOverlay;
import com.modularwarfare.client.compat.AtomicShaderCompat;

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
    }

    @Override
    protected void initArmor() {
        this.modelLeggings = new FakeModelBiped(0.5F);
        this.modelArmor = new FakeModelBiped(1.0F);
    }

    /**
     * Overlay-only translucent armor pass (EntityForwardOverlay). Assumes the living model is posed.
     */
    public void renderTranslucentOnly(
            EntityLivingBase entitylivingbaseIn,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            float scale) {
        boolean flag = ModelCustomArmor.translucentBatch;
        boolean flag1 = ModelCustomArmor.needTranslucentBatchBuf;
        ModelCustomArmor.translucentBatch = true;
        ModelCustomArmor.needTranslucentBatchBuf = false;
        super.doRenderLayer(
                entitylivingbaseIn,
                limbSwing,
                limbSwingAmount,
                partialTicks,
                ageInTicks,
                netHeadYaw,
                headPitch,
                scale);
        ModelCustomArmor.translucentBatch = flag;
        ModelCustomArmor.needTranslucentBatchBuf = flag1;
    }

    @Override
    public void doRenderLayer(
            EntityLivingBase entitylivingbaseIn,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            float scale) {
        if (ArmorTranslucentOverlay.isDrawing()) {
            renderTranslucentOnly(
                    entitylivingbaseIn,
                    limbSwing,
                    limbSwingAmount,
                    partialTicks,
                    ageInTicks,
                    netHeadYaw,
                    headPitch,
                    scale);
            return;
        }

        boolean flag = ModelCustomArmor.translucentBatch;
        boolean flag1 = ModelCustomArmor.needTranslucentBatchBuf;
        ModelCustomArmor.translucentBatch = false;
        ModelCustomArmor.needTranslucentBatchBuf = false;
        // Do not beginOpaqueFillCapture here — Layer binds armor albedo first; ModelCustomArmor
        // adopts that albedo before mesh (avoids rebinding a stale held-item currentFillAlbedo).
        if (AtomicShaderCompat.isGBufferFillActive()) {
            AtomicShaderCompat.clearEmissive();
        }
        super.doRenderLayer(
                entitylivingbaseIn,
                limbSwing,
                limbSwingAmount,
                partialTicks,
                ageInTicks,
                netHeadYaw,
                headPitch,
                scale);
        if (AtomicShaderCompat.isGBufferFillActive()) {
            AtomicShaderCompat.clearEmissive();
            AtomicShaderCompat.afterOpaqueMesh();
        }
        if (ModelCustomArmor.needTranslucentBatchBuf) {
            if (AtomicShaderCompat.isGBufferFillActive()) {
                ArmorTranslucentOverlay.queue(
                        entitylivingbaseIn,
                        renderer,
                        limbSwing,
                        limbSwingAmount,
                        partialTicks,
                        ageInTicks,
                        netHeadYaw,
                        headPitch,
                        scale);
            } else {
                ModelCustomArmor.translucentBatch = true;
                super.doRenderLayer(
                        entitylivingbaseIn,
                        limbSwing,
                        limbSwingAmount,
                        partialTicks,
                        ageInTicks,
                        netHeadYaw,
                        headPitch,
                        scale);
            }
        }
        ModelCustomArmor.translucentBatch = flag;
        ModelCustomArmor.needTranslucentBatchBuf = flag1;
    }
}
