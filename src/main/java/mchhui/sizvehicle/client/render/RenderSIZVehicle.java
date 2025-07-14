package mchhui.sizvehicle.client.render;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.util.vector.Quaternion;

import mchhui.hegltf.DataNode;
import mchhui.hegltf.GltfDataModel;
import mchhui.hegltf.GltfRenderModel;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import mchhui.sizvehicle.common.entity.EntityCar;
import mchhui.sizvehicle.common.model.Model;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

public class RenderSIZVehicle extends RenderLiving<EntityCar> {
    private static final ResourceLocation texture = new ResourceLocation("modularwarfare:gltf/赛博基尼.png");
    private static final Model model = new Model(new GltfRenderModel(GltfDataModel.load(new ResourceLocation("modularwarfare:gltf/赛博基尼.glb"))));

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
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        body:
        {
            GlStateManager.pushMatrix();
            Quaternionf q = new Quaternionf(entity.getLastPose().getQuaternion()).slerp(entity.getPose().getQuaternion(), partialTicks);
            GlStateManager.rotate(new Quaternion(q.x, q.y, q.z, q.w));
            model.setAnimationCalBlender(new NodeAnimationBlender("sizvehicle") {
                @Override
                public void handle(DataNode node, Matrix4f mat) {
                    if (node.name.equals("l_f_suspension") || node.name.equals("r_f_suspension")) {
                        mat.rotateY((float)Math.toRadians(entity.getMaxWhellAngle() * entity.getInputAngleFactor()));
                        mat.rotateX(-(entity.lastWhellProcess+(entity.whellProcess-entity.lastWhellProcess)*partialTicks));
                    }
                    if (node.name.equals("l_b_suspension") || node.name.equals("r_b_suspension")) {
                        mat.rotateX(-(entity.lastWhellProcess+(entity.whellProcess-entity.lastWhellProcess)*partialTicks));
                    }
                }
            });
            model.updateAnimation(0, true);
            model.renderPartExcept(null);
            GlStateManager.popMatrix();
            break body;
        }

        GlStateManager.translate(0, 1.8f, 0);
        entity.renderDebugAxis();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCar entity) {
        // TODO Auto-generated method stub
        return null;
    }
}