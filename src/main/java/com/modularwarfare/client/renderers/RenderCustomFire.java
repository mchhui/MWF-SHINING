package com.modularwarfare.client.renderers;

import com.modularwarfare.common.entity.EntityCustomFire;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderCustomFire extends Render<EntityCustomFire> {
    
    public static final Factory FACTORY = new Factory();
    
    public RenderCustomFire(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityCustomFire entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 不渲染任何内容
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCustomFire entity) {
        return null;
    }
    
    public static class Factory implements IRenderFactory<EntityCustomFire> {
        @Override
        public Render<EntityCustomFire> createRenderFor(RenderManager manager) {
            return new RenderCustomFire(manager);
        }
    }
} 