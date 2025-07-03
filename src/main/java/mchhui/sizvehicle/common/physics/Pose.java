package mchhui.sizvehicle.common.physics;

import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

/**
 * @author Hueihuea
 * @apiNote 姿态管理类
 * */
public class Pose {
    private static final Vector3f FORWARD = new Vector3f(0, 0, 1);
    private static final Vector3f UP = new Vector3f(0, 1, 0);
    private static final Vector3f LEFT = new Vector3f(1, 0, 0);

    private Quaternionf pose = new Quaternionf();

    public Quaternionf getQuaternion() {
        return pose;
    }

    public void getForward(Vector3f vec) {
        vec.set(FORWARD).rotate(pose);
    }

    public Vector3f getForward() {
        return new Vector3f(FORWARD).rotate(pose);
    }

    public void getUp(Vector3f vec) {
        vec.set(UP).rotate(pose);
    }

    public Vector3f getUp() {
        return new Vector3f(UP).rotate(pose);
    }

    public void getLeft(Vector3f vec) {
        vec.set(LEFT).rotate(pose);
    }

    public Vector3f getLeft() {
        return new Vector3f(LEFT).rotate(pose);
    }

    public void rotateX(float angle) {
        pose.rotateLocalX(Math.toRadians(angle));
    }

    public void rotateY(float angle) {
        pose.rotateLocalY(Math.toRadians(angle));
    }

    public void rotateZ(float angle) {
        pose.rotateLocalZ(Math.toRadians(angle));
    }

    public void rotateXRad(float angle) {
        pose.rotateLocalX(angle);
    }

    public void rotateYRad(float angle) {
        pose.rotateLocalY(angle);
    }

    public void rotateZRad(float angle) {
        pose.rotateLocalZ(angle);
    }

    @SideOnly(Side.CLIENT)
    public void renderDebugAxis() {
        // 保存当前 OpenGL 状态
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.glLineWidth(3.0F);

        // 获取变换后的轴向向量
        Vector3f forward = getForward();
        Vector3f up = getUp();
        Vector3f left = getLeft();

        //        System.out.println(forward);

        // 轴的长度
        float axisLength = 1.0f;

        Tessellator tessellator = Tessellator.getInstance();

        // 绘制 FORWARD 轴 (蓝色 - Z轴)
        tessellator.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        tessellator.getBuffer().pos(0, 0, 0).color(0, 0, 255, 255).endVertex();
        tessellator.getBuffer().pos(forward.x * axisLength, forward.y * axisLength, forward.z * axisLength).color(0, 0, 255, 255).endVertex();
        tessellator.draw();

        // 绘制 UP 轴 (绿色 - Y轴)
        tessellator.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        tessellator.getBuffer().pos(0, 0, 0).color(0, 255, 0, 255).endVertex();
        tessellator.getBuffer().pos(up.x * axisLength, up.y * axisLength, up.z * axisLength).color(0, 255, 0, 255).endVertex();
        tessellator.draw();

        // 绘制 LEFT 轴 (红色 - X轴)
        tessellator.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        tessellator.getBuffer().pos(0, 0, 0).color(255, 0, 0, 255).endVertex();
        tessellator.getBuffer().pos(left.x * axisLength, left.y * axisLength, left.z * axisLength).color(255, 0, 0, 255).endVertex();
        tessellator.draw();

        // 恢复 OpenGL 状态
        GlStateManager.glLineWidth(1.0F);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
