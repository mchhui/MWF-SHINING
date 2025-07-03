package mchhui.sizvehicle.client.handler;

import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DebugHUDHandler {
    public static DebugHUDHandler INSTANCE;

    public int entityID;
    public Vector3f speed;
    public Vector3f driveForce;
    public Vector3f resistanceForce;

    // 用于存储上一次的数据，避免空指针
    public Vector3f lastSpeed = new Vector3f(0, 0, 0);
    public Vector3f lastDriveForce = new Vector3f(0, 0, 0);
    public Vector3f lastResistanceForce = new Vector3f(0, 0, 0);

    public DebugHUDHandler() {
        DebugHUDHandler.INSTANCE = this;
    }

    @SubscribeEvent
    public void onRenderHUD(RenderGameOverlayEvent.Post event) {
        if (event.getType() != ElementType.ALL) {
            return;
        }

        // 检查玩家是否在驾驶车辆
        if (Minecraft.getMinecraft().player == null || Minecraft.getMinecraft().player.getRidingEntity() == null) {
            return;
        }

        // 更新数据
        if (speed != null)
            lastSpeed = speed;
        if (driveForce != null)
            lastDriveForce = driveForce;
        if (resistanceForce != null)
            lastResistanceForce = resistanceForce;

        // 渲染HUD
        renderDebugHUD();
    }

    private void renderDebugHUD() {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fontRenderer = mc.fontRenderer;
        ScaledResolution scaledResolution = new ScaledResolution(mc);

        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        // 设置HUD位置（右上角）
        int x = screenWidth - 200;
        int y = 20;
        int lineHeight = 12;

        // 背景
        drawRect(x - 5, y - 5, x + 195, y + 320, 0x80000000);

        // 标题
        fontRenderer.drawString("车辆调试信息", x, y, 0xFFFFFF);
        y += lineHeight + 5;

        // 速度信息
        fontRenderer.drawString("速度 (m/s):", x, y, 0x00FF00);
        y += lineHeight;
        fontRenderer.drawString(String.format("  X: %.2f", lastSpeed.x), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  Y: %.2f", lastSpeed.y), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  Z: %.2f", lastSpeed.z), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  总速度: %.2f", lastSpeed.length()), x, y, 0xFFFF00);
        y += lineHeight + 5;

        // 驱动力信息
        fontRenderer.drawString("驱动力 (N):", x, y, 0x00FF00);
        y += lineHeight;
        fontRenderer.drawString(String.format("  X: %.1f", lastDriveForce.x), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  Y: %.1f", lastDriveForce.y), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  Z: %.1f", lastDriveForce.z), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  总驱动力: %f", lastDriveForce.length()), x, y, 0xFFFF00);
        y += lineHeight + 5;

        // 阻力信息
        fontRenderer.drawString("阻力 (N):", x, y, 0x00FF00);
        y += lineHeight;
        fontRenderer.drawString(String.format("  X: %.1f", lastResistanceForce.x), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  Y: %.1f", lastResistanceForce.y), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  Z: %.1f", lastResistanceForce.z), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  总阻力: %.1f", lastResistanceForce.length()), x, y, 0xFFFF00);
        y += lineHeight + 5;
        
        // 合力信息
        Vector3f netForce = new Vector3f(lastDriveForce).add(lastResistanceForce);
        fontRenderer.drawString("合力 (N):", x, y, 0x00FF00);
        y += lineHeight;
        fontRenderer.drawString(String.format("  X: %.1f", netForce.x), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  Y: %.1f", netForce.y), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  Z: %.1f", netForce.z), x, y, 0xFFFFFF);
        y += lineHeight;
        fontRenderer.drawString(String.format("  总合力: %.1f", netForce.length()), x, y, 0xFFFF00);
        y += lineHeight + 5;
        
        // 向量颜色说明
        fontRenderer.drawString("Debug轴线说明:", x, y, 0x00FF00);
        y += lineHeight;
        fontRenderer.drawString("  青色 - 速度向量", x, y, 0x00FFFF);
        y += lineHeight;
        fontRenderer.drawString("  绿色 - 驱动力向量", x, y, 0x00FF00);
        y += lineHeight;
        fontRenderer.drawString("  红色 - 阻力向量", x, y, 0xFF0000);
        y += lineHeight;
        fontRenderer.drawString("  黄色 - 合力向量", x, y, 0xFFFF00);
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        net.minecraft.client.gui.Gui.drawRect(left, top, right, bottom, color);
    }
}
