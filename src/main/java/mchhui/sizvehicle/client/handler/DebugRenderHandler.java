package mchhui.sizvehicle.client.handler;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DebugRenderHandler {
    public static DebugRenderHandler INSTANCE;
    public int entityID;
    public Vector3f speed;
    public Vector3f driveForce;
    public Vector3f resistanceForce;
    public Vector3f debugPoint1;
    public Vector3f debugPoint2;
    public Vector3f debugPoint3;
    public Vector3f debugPoint4;

    public DebugRenderHandler() {
        DebugRenderHandler.INSTANCE = this;
    }

    @SubscribeEvent
    public void onRenderHUD(RenderGameOverlayEvent.Post event) {
        if (event.getType() != ElementType.ALL) {
            return;
        }
        renderDebugHUD();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        renderTestPoint();
    }

    public void renderDebugHUD() {

    }

    public void renderTestPoint() {
        if (debugPoint1 == null) {
            return;
        }
        
        // 保存OpenGL状态
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        // 获取相机实体和位置
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if (entity == null) {
            GlStateManager.popMatrix();
            return;
        }

        // 调整相机位置偏移（将世界坐标转换为相对于相机的坐标）
        double interpPosX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * Minecraft.getMinecraft().getRenderPartialTicks();
        double interpPosY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * Minecraft.getMinecraft().getRenderPartialTicks();
        double interpPosZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * Minecraft.getMinecraft().getRenderPartialTicks();

        GlStateManager.translate(-interpPosX, -interpPosY, -interpPosZ);

        // 渲染所有四个调试点
        renderSingleDebugPoint(debugPoint1, 1.0F, 0.0F, 0.0F, 0.8F); // 红色
        if (debugPoint2 != null) {
            renderSingleDebugPoint(debugPoint2, 0.0F, 1.0F, 0.0F, 0.8F); // 绿色
        }
        if (debugPoint3 != null) {
            renderSingleDebugPoint(debugPoint3, 0.0F, 0.0F, 1.0F, 0.8F); // 蓝色
        }
        if (debugPoint4 != null) {
            renderSingleDebugPoint(debugPoint4, 1.0F, 1.0F, 0.0F, 0.8F); // 黄色
        }

        // 恢复OpenGL状态
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void renderSingleDebugPoint(Vector3f point, float r, float g, float b, float a) {
        GlStateManager.pushMatrix();
        
        // 移动到调试点位置
        GlStateManager.translate(point.x, point.y, point.z);

        // 设置颜色
        GlStateManager.color(r, g, b, a);

        // 绘制一个小立方体来标记点位置
        float size = 0.1F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 绘制立方体的6个面
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        // 前面
        buffer.pos(-size, -size, size).endVertex();
        buffer.pos(size, -size, size).endVertex();
        buffer.pos(size, size, size).endVertex();
        buffer.pos(-size, size, size).endVertex();

        // 后面
        buffer.pos(-size, -size, -size).endVertex();
        buffer.pos(-size, size, -size).endVertex();
        buffer.pos(size, size, -size).endVertex();
        buffer.pos(size, -size, -size).endVertex();

        // 上面
        buffer.pos(-size, size, -size).endVertex();
        buffer.pos(-size, size, size).endVertex();
        buffer.pos(size, size, size).endVertex();
        buffer.pos(size, size, -size).endVertex();

        // 下面
        buffer.pos(-size, -size, -size).endVertex();
        buffer.pos(size, -size, -size).endVertex();
        buffer.pos(size, -size, size).endVertex();
        buffer.pos(-size, -size, size).endVertex();

        // 右面
        buffer.pos(size, -size, -size).endVertex();
        buffer.pos(size, size, -size).endVertex();
        buffer.pos(size, size, size).endVertex();
        buffer.pos(size, -size, size).endVertex();

        // 左面
        buffer.pos(-size, -size, -size).endVertex();
        buffer.pos(-size, -size, size).endVertex();
        buffer.pos(-size, size, size).endVertex();
        buffer.pos(-size, size, -size).endVertex();

        tessellator.draw();
        
        GlStateManager.popMatrix();
    }
}
