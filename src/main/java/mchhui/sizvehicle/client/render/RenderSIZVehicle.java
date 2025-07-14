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
import mchhui.sizvehicle.network.NetworkManager;
import mchhui.sizvehicle.network.client.PacketWheelPositions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderSIZVehicle extends RenderLiving<EntityCar> {
    private static final ResourceLocation texture = new ResourceLocation("modularwarfare:gltf/赛博基尼.png");
    private static final Model model = new Model(new GltfRenderModel(GltfDataModel.load(new ResourceLocation("modularwarfare:gltf/赛博基尼.glb"))));

    public RenderSIZVehicle(RenderManager rendermanagerIn) {
        super(rendermanagerIn, new ModelZombie(), 0.5f);
    }

    @Override
    public void doRender(EntityCar entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 如果服务器端的轮子位置尚未初始化，发送轮子位置信息
        if (!entity.areWheelOffsetsInitialized()) {
            sendWheelPositionsToServer(entity);
        }
        
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

    /**
     * 发送轮子位置信息到服务器
     */
    private void sendWheelPositionsToServer(EntityCar entity) {
        // 更新模型动画以获取准确的轮子位置
        model.updateAnimation(0, false);
        
        // 获取轮子位置变换矩阵
        Matrix4f leftFrontTransform = model.getGlobalTransform("l_f_tp");
        Matrix4f rightFrontTransform = model.getGlobalTransform("r_f_tp");
        Matrix4f leftBackTransform = model.getGlobalTransform("l_b_tp");
        Matrix4f rightBackTransform = model.getGlobalTransform("r_b_tp");
        
        // 创建并发送数据包
        PacketWheelPositions packet = new PacketWheelPositions(
            entity.getEntityId(),
            leftFrontTransform,
            rightFrontTransform,
            leftBackTransform,
            rightBackTransform
        );
        
        NetworkManager.sendToServer(packet);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCar entity) {
        // TODO Auto-generated method stub
        return null;
    }
}