package com.modularwarfare.client.handler;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGrenade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.lwjgl.opengl.GL11;

import java.util.HashMap;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID, value = Side.CLIENT)
public class GrenadeRenderHandler {

    public static final ResourceLocation GRENADE_INDICATOR = new ResourceLocation("modularwarfare", "textures/gui/grenade_indicator.png");
    
    // 定义需要排除的部分
    public static final HashSet<String> DEFAULT_EXCEPT = new HashSet<String>();
    public static final List<String> defaultHideList = Arrays.asList(
        "leftArmModel", "leftArmLayerModel",
        "rightArmModel", "rightArmLayerModel",
        "leftArmSlimModel", "leftArmLayerSlimModel",
        "rightArmSlimModel", "rightArmLayerSlimModel"
    );
    
    static {
        DEFAULT_EXCEPT.addAll(defaultHideList);
    }
    
    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemGrenade)) return;

        GrenadeType type = ((ItemGrenade) stack.getItem()).type;
        if (type.animationType == WeaponAnimationType.ENHANCED && type.showIndicator) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemGrenade)) return;

        GrenadeType type = ((ItemGrenade) stack.getItem()).type;
        if (type.animationType != WeaponAnimationType.ENHANCED) return;

        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();

        if (type.showIndicator) {
            renderGrenadeIndicator(mc, width, height, GrenadeEnhancedHandler.isThrowLow);
        }
        
        if (type.showTimerBar) {
            renderTimerBar(mc, width, height, stack, type);
        }
    }
    
    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;
        
        if (mc.getRenderViewEntity() != player) return;

        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemGrenade)) return;

        GrenadeType type = ((ItemGrenade) stack.getItem()).type;
        if (type.animationType != WeaponAnimationType.ENHANCED) return;

        if (!GrenadeEnhancedHandler.isHolding || !type.showTrajectory) return;

        GrenadeTrajectoryManager.getInstance().calculateTrajectory(player, type, event.getPartialTicks());
        GrenadeTrajectoryManager.getInstance().renderTrajectory(player, event.getPartialTicks());
    }

    private static void renderTimerBar(Minecraft mc, int width, int height, ItemStack stack, GrenadeType type) {
        int barWidth = 3;
        int barHeight = 40;
        int barX = width / 2 + 40;
        int barY = height / 2 - barHeight / 2;
        
        Gui.drawRect(barX, barY, barX + barWidth, barY + barHeight, 0x88000000);
        
        if(stack.hasTagCompound() && stack.getTagCompound().hasKey("timerStarted") && 
           stack.getTagCompound().getBoolean("timerStarted")) {
            
            long startTime = stack.getTagCompound().getLong("timerStartTime");
            float elapsedTime = (System.currentTimeMillis() - startTime) / 1000f;
            float progress = Math.min(elapsedTime / type.fuseTime + mc.getRenderPartialTicks() / 20f / type.fuseTime, 1.0f);
            
            int progressHeight = (int)(barHeight * progress);
            
            int color;
            if (progress < 0.5f) {
                float factor = progress * 2;
                int r = 255;
                int g = 255;
                int b = (int)(255 * (1 - factor));
                color = ((0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            } else {
                float factor = (progress - 0.5f) * 2;
                int r = 255;
                int g = (int)(255 * (1 - factor));
                int b = 0;
                color = ((0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            }
            
            Gui.drawRect(barX, barY + barHeight - progressHeight, 
                        barX + barWidth, barY + barHeight, 
                        color);
        }
        
        int borderColor = 0x66AAAAAA;
        
        Gui.drawRect(barX - 1, barY - 1, barX + barWidth + 1, barY, borderColor);
        Gui.drawRect(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, borderColor);
        Gui.drawRect(barX - 1, barY - 1, barX, barY + barHeight + 1, borderColor);
        Gui.drawRect(barX + barWidth, barY - 1, barX + barWidth + 1, barY + barHeight + 1, borderColor);
    }

    private static void renderGrenadeIndicator(Minecraft mc, int width, int height, boolean isLowThrow) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2, height / 2, 0);
        
        if(isLowThrow) {
            GlStateManager.rotate(180, 0, 0, 1);
        }
        
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        
        mc.renderEngine.bindTexture(GRENADE_INDICATOR);
        Gui.drawModalRectWithCustomSizedTexture(-8, -8, 0, 0, 16, 16, 16, 16);
        
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

} 