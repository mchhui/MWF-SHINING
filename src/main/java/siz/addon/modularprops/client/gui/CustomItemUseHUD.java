package siz.addon.modularprops.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import siz.addon.modularprops.client.fpp.enhanced.animation.CustomItemAnimationController;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig;
import siz.addon.modularprops.common.custom.CustomItemType;
import siz.addon.modularprops.common.custom.ItemCustom;
import siz.addon.modularprops.common.handler.CustomItemEventHandler;

/**
 * 自定义道具使用进度条HUD
 * 显示在屏幕中下位置
 */
@SideOnly(Side.CLIENT)
public class CustomItemUseHUD extends Gui {
    
    private static final int BAR_WIDTH = 182; // 与饥饿条/经验条宽度一致
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_BORDER = 1;
    
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        
        if (player == null) {
            return;
        }
        
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!(mainHand.getItem() instanceof ItemCustom)) {
            return;
        }
        
        ItemCustom item = (ItemCustom) mainHand.getItem();
        CustomItemType itemType = item.type;
        
        if (itemType == null || itemType.animationType != com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
            return;
        }
        
        // 检查是否需要显示进度条（usable 或有 consumableConfig）
        boolean shouldShowProgress = itemType.usable || 
                                    (itemType.consumableConfig != null);
        
        if (!shouldShowProgress) {
            return;
        }
        
        // 获取动画控制器
        CustomItemAnimationController controller = CustomItemEventHandler.getItemAnimationController(
            player.getUniqueID(), itemType
        );
        
        if (controller == null || controller.isUseComplete()) {
            return;
        }
        
        // 获取USE动画进度
        double useProgress = controller.USE;
        if (useProgress >= 1.0 || useProgress <= 0.0) {
            return;
        }
        
        // 获取执行百分比
        float executePercent = 0.8f; // 默认值
        if (itemType.enhancedModel != null && itemType.enhancedModel.config instanceof CustomItemEnhancedRenderConfig) {
            CustomItemEnhancedRenderConfig config = (CustomItemEnhancedRenderConfig) itemType.enhancedModel.config;
            executePercent = config.useExecutePercent;
        }
        
        // 计算进度条显示的进度
        // 进度条显示到executePercent时就是100%
        float displayProgress = (float)(useProgress / executePercent);
        if (displayProgress > 1.0f) {
            displayProgress = 1.0f;
        }
        
        // 渲染进度条（不再传递executePercent，因为进度条本身就是基于executePercent）
        renderUseProgressBar(event.getResolution(), displayProgress);
    }
    
    private void renderUseProgressBar(ScaledResolution resolution, float progress) {
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        
        // 位置：屏幕中下位置（在经验条上方一点）
        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = screenHeight - 48; // 在快捷栏上方
        
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        
        // 绘制背景（黑色半透明）
        drawRect(x - BAR_BORDER, y - BAR_BORDER, 
                 x + BAR_WIDTH + BAR_BORDER, y + BAR_HEIGHT + BAR_BORDER, 
                 0x88000000);
        
        // 绘制进度条内部背景（深灰色）
        drawRect(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF333333);
        
        // 绘制进度条（渐变色：黄色->绿色）
        int progressWidth = (int)(BAR_WIDTH * progress);
        if (progressWidth > 0) {
            // 根据进度改变颜色
            int color;
            if (progress < 1.0f) {
                // 未到执行点：黄色
                color = 0xFFFFAA00;
            } else {
                // 已到执行点：绿色
                color = 0xFF00FF00;
            }
            drawRect(x, y, x + progressWidth, y + BAR_HEIGHT, color);
        }
        
        GlStateManager.disableBlend();
    }
}

