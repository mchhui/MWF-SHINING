package mchhui.sizvehicle.client.render;

import mchhui.sizvehicle.common.entity.EntityCar;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderSIZVehicle extends RenderLiving<EntityCar> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("sizvehicle:textures/entity/siz_vehicle.png");

    public RenderSIZVehicle(RenderManager rendermanagerIn) {
        super(rendermanagerIn, new ModelZombie(), 0.5f);

    }

    @Override
    public void doRender(EntityCar entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 调用父类的doRender方法来确保基础渲染正常工作
//        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        // 添加自定义渲染逻辑
        GlStateManager.pushMatrix();
        renderLivingAt(entity, x, y, z);
        GlStateManager.translate(0, 1.8f, 0);
        entity.renderDebugAxis();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCar entity) {
        return TEXTURE;
    }
}