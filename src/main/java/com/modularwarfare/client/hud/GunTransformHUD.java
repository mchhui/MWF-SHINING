package com.modularwarfare.client.hud;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.GunTransformManager;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.common.guns.ItemAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.HashMap;

public class GunTransformHUD {
    
    private static final ResourceLocation DEFAULT_WHEEL_TEXTURE = new ResourceLocation(ModularWarfare.MOD_ID, "textures/gui/transform_wheel.png");
    private static final int WHEEL_SIZE = 128;
    private static final int MOVE_RESET_DELAY = 200;
    private static final long TRANSFORM_COOLDOWN = 500;
    
    private static boolean isVisible = false;
    private static int selectedIndex = -1;
    private static long lastMoveTime = 0;
    private static float totalDeltaX = 0;
    private static float totalDeltaY = 0;
    private static int currentMouseScreenX = 0;
    private static int currentMouseScreenY = 0;
    private static long showStartTime = 0;
    private static long lastTransformTime = 0;
    private static final long QUICK_CLICK_THRESHOLD = 200;
    
    public static void setVisible(boolean visible) {
        if(visible) {
            if(System.currentTimeMillis() - lastTransformTime < TRANSFORM_COOLDOWN) {
                return;
            }
            
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer player = mc.player;
            ItemStack heldItem = player.getHeldItemMainhand();
            
            if(heldItem.getItem() instanceof ItemGun) {
                if(GunTransformManager.isTransforming(player)) {
                    return;
                }
            }
        }
        
        isVisible = visible;
        if(visible) {
            selectedIndex = -1;
            lastMoveTime = System.currentTimeMillis();
            showStartTime = System.currentTimeMillis();
            totalDeltaX = 0;
            totalDeltaY = 0;
            
            Minecraft mc = Minecraft.getMinecraft();
            int centerX = mc.displayWidth / 2;
            int centerY = mc.displayHeight / 2;
            Mouse.setCursorPosition(centerX, centerY);
            currentMouseScreenX = Mouse.getX();
            currentMouseScreenY = Mouse.getY();
        }
    }
    
    public static boolean isVisible() {
        return isVisible;
    }
    
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if(!isVisible || event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        ItemStack heldItem = player.getHeldItemMainhand();
        
        if(!(heldItem.getItem() instanceof ItemGun)) return;
        
        GunType gunType = ((ItemGun)heldItem.getItem()).type;
        if(gunType.transformations == null || gunType.transformations.isEmpty()) return;
        
        java.util.List<String> availableGuns = new java.util.ArrayList<>();
        for(String targetGun : gunType.transformations.values()) {
            if(!targetGun.equals(gunType.internalName)) {
                availableGuns.add(targetGun);
            }
        }
    
        if(availableGuns.isEmpty()) return;
        
        ScaledResolution scaled = new ScaledResolution(mc);
        int centerX = scaled.getScaledWidth() / 2;
        int centerY = scaled.getScaledHeight() / 2;
        
        int numOptions = availableGuns.size();
        
        int currentMouseX = Mouse.getX();
        int currentMouseY = Mouse.getY();
        int mouseDeltaX = currentMouseX - currentMouseScreenX;
        int mouseDeltaY = -(currentMouseY - currentMouseScreenY);
        currentMouseScreenX = currentMouseX;
        currentMouseScreenY = currentMouseY;
        
        float sensitivity = 0.5f;
        totalDeltaX += mouseDeltaX * sensitivity;
        totalDeltaY += mouseDeltaY * sensitivity;
        
        float moveLength = (float) Math.sqrt(totalDeltaX * totalDeltaX + totalDeltaY * totalDeltaY);
        if(moveLength > 30f) {

            float moveAngle = (float) Math.atan2(totalDeltaY, totalDeltaX);
            if(moveAngle < 0) {
                moveAngle += 2 * Math.PI;
            }
            

            float moveDegrees = (float) Math.toDegrees(moveAngle);
            

            float sectionSize = 360f / numOptions;
            

            selectedIndex = (int)((moveDegrees + sectionSize/2) % 360 / sectionSize);
        }
        

        long currentTime = System.currentTimeMillis();
        if(currentTime - lastMoveTime > MOVE_RESET_DELAY) {

            if(moveLength < 5f) {
                totalDeltaX = 0;
                totalDeltaY = 0;
            }
        }
        lastMoveTime = currentTime;
        
        // 渲染HUD
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.8F);
        
        // 绘制轮盘背景
        ResourceLocation wheelTexture = DEFAULT_WHEEL_TEXTURE;
        if(gunType.wheelType != null) {
            wheelTexture = gunType.wheelType.resourceLocations.get(0);
        }
        mc.getTextureManager().bindTexture(wheelTexture);
        drawModalRectWithCustomSizedTexture(centerX - WHEEL_SIZE/2, centerY - WHEEL_SIZE/2, 
                0, 0, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE);
        
        // 绘制选择指示线
        if(selectedIndex >= 0) {
            float angle = selectedIndex * (360.0f / numOptions);
            float radians = (float) Math.toRadians(angle);
            int radius = WHEEL_SIZE/3;
            int lineEndX = centerX + (int)(Math.cos(radians) * radius);
            int lineEndY = centerY + (int)(Math.sin(radians) * radius);
            
            drawLine(centerX, centerY, lineEndX, lineEndY, 0xFFFFFF00);
        }
        
        // 绘制鼠标当前位置
        int mouseX = Mouse.getX() * scaled.getScaledWidth() / mc.displayWidth;
        int mouseY = scaled.getScaledHeight() - Mouse.getY() * scaled.getScaledHeight() / mc.displayHeight - 1;
        drawRect(mouseX - 2, mouseY - 2, mouseX + 2, mouseY + 2, 0xFFFF0000);
        
        // 绘制移动方向指示器
        if(Math.abs(totalDeltaX) > 0 || Math.abs(totalDeltaY) > 0) {
            if(moveLength > 0) {
                float normalizedX = totalDeltaX / moveLength;
                float normalizedY = totalDeltaY / moveLength;
                int indicatorLength = 30;
                int dirX = centerX + (int)(normalizedX * indicatorLength);
                int dirY = centerY + (int)(normalizedY * indicatorLength);
                drawLine(centerX, centerY, dirX, dirY, 0xFF00FF00);
            }
        }
        
        // 绘制选项
        for(int i = 0; i < numOptions; i++) {
            String targetGun = availableGuns.get(i);
            boolean isSelected = i == selectedIndex;
            drawTransformOption(centerX, centerY, i, numOptions, targetGun, isSelected);
        }
        
        GlStateManager.popMatrix();
    }
    
    private void drawTransformOption(int centerX, int centerY, int index, int total, String targetGun, boolean selected) {
        float angle = index * (360.0f / total);
        int radius = total == 1 ? 0 : WHEEL_SIZE/3;
        
        int x = centerX + (int)(Math.cos(Math.toRadians(angle)) * radius);
        int y = centerY + (int)(Math.sin(Math.toRadians(angle)) * radius);
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        
        float scale = selected || total == 1 ? 0.7f : 0.5f;
        GlStateManager.scale(scale, scale, scale);
        
        String displayName = targetGun.substring(targetGun.lastIndexOf(":") + 1);
        int color = selected || total == 1 ? 0xFFFF00 : 0xFFFFFF;
        
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(displayName, 
                -Minecraft.getMinecraft().fontRenderer.getStringWidth(displayName)/2, -4, color);
        
        GlStateManager.popMatrix();
    }
    
    public static void onKeyReleased() {
        if(!isVisible) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        ItemStack heldItem = player.getHeldItemMainhand();
        
        if(heldItem.getItem() instanceof ItemGun) {
            GunType gunType = ((ItemGun)heldItem.getItem()).type;
            if(gunType.transformations != null && !gunType.transformations.isEmpty()) {

                java.util.List<String> availableGuns = new java.util.ArrayList<>();
                for(String targetGun : gunType.transformations.values()) {
                    if(!targetGun.equals(gunType.internalName)) {
                        availableGuns.add(targetGun);
                    }
                }
                
                if(!availableGuns.isEmpty()) {
                    long holdTime = System.currentTimeMillis() - showStartTime;
                    String targetGun;
                    Integer targetTransformId = null;
                    
                    if(availableGuns.size() == 1) {
                        targetGun = availableGuns.get(0);
                        // 找到对应的变换ID
                        for(Integer id : gunType.transformations.keySet()) {
                            if(gunType.transformations.get(id).equals(targetGun)) {
                                targetTransformId = id;
                                break;
                            }
                        }
                    } else if(holdTime < QUICK_CLICK_THRESHOLD) {
                        if(heldItem.hasTagCompound()) {
                            int lastState = ItemGun.getLastTransformState(heldItem);
                            targetGun = gunType.transformations.get(lastState);
                            targetTransformId = lastState;
                            if(targetGun == null || targetGun.equals(gunType.internalName)) {
                                setVisible(false);
                                return;
                            }
                        } else {
                            setVisible(false);
                            return;
                        }
                    } else if(selectedIndex >= 0 && selectedIndex < availableGuns.size()) {
                        targetGun = availableGuns.get(selectedIndex);
                        // 找到对应的变换ID
                        for(Integer id : gunType.transformations.keySet()) {
                            if(gunType.transformations.get(id).equals(targetGun)) {
                                targetTransformId = id;
                                break;
                            }
                        }
                    } else {
                        setVisible(false);
                        return;
                    }
                    
                    // 检查变换所需的配件条件
                    if(targetTransformId != null && gunType.transformationRequirements != null) {
                        HashMap<AttachmentPresetEnum, String> requirements = gunType.transformationRequirements.get(targetTransformId);
                        if(requirements != null && !requirements.isEmpty()) {
                            boolean allRequirementsMet = true;
                            String missingAttachment = null;
                            AttachmentPresetEnum missingType = null;
                            
                            for(AttachmentPresetEnum attachmentType : requirements.keySet()) {
                                String requiredAttachment = requirements.get(attachmentType);
                                ItemStack attachmentStack = GunType.getAttachment(heldItem, attachmentType);
                                
                                if(attachmentStack == null || !(attachmentStack.getItem() instanceof ItemAttachment)) {
                                    allRequirementsMet = false;
                                    missingAttachment = requiredAttachment;
                                    missingType = attachmentType;
                                    break;
                                }
                                
                                ItemAttachment attachmentItem = (ItemAttachment)attachmentStack.getItem();
                                if(!attachmentItem.type.internalName.equals(requiredAttachment)) {
                                    allRequirementsMet = false;
                                    missingAttachment = requiredAttachment;
                                    missingType = attachmentType;
                                    break;
                                }
                            }
                            
                            if(!allRequirementsMet) {
                                ItemAttachment attachmentItem = null;
                                String attachmentDisplayName = missingAttachment;
                                
                                for(ItemAttachment item : ModularWarfare.attachmentTypes.values()) {
                                    if(item.type.internalName.equals(missingAttachment)) {
                                        attachmentDisplayName = item.type.displayName != null ? 
                                                item.type.displayName : missingAttachment;
                                        attachmentItem = item;
                                        break;
                                    }
                                }
                                
                                String attachmentTypeKey = "mwf.dictionary." + missingType.typeName;
                                String attachmentTypeName = net.minecraft.client.resources.I18n.format(attachmentTypeKey, missingType.typeName);
                                
                                String messageKey = "mwf.transform.requirement_not_met";
                                String message = net.minecraft.client.resources.I18n.format(messageKey, 
                                        attachmentTypeName, attachmentDisplayName);
                                
                                player.sendMessage(new net.minecraft.util.text.TextComponentString("§c" + message));
                                setVisible(false);
                                return;
                            }
                        }
                    }
                    
                    GunTransformManager.transformGun(player, targetGun);
                    lastTransformTime = System.currentTimeMillis();
                }
            }
        }
        
        setVisible(false);
    }
    
    private void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, 0.0D).tex(u * f, (v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).tex((u + width) * f, (v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y, 0.0D).tex((u + width) * f, v * f1).endVertex();
        bufferbuilder.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
        tessellator.draw();
    }
    
    private void drawLine(int x1, int y1, int x2, int y2, int color) {
        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(1, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(x1, y1, 0).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(x2, y2, 0).color(f, f1, f2, f3).endVertex();
        tessellator.draw();
        
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
    
    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            int j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom, 0.0D).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(right, top, 0.0D).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(left, top, 0.0D).color(f, f1, f2, f3).endVertex();
        tessellator.draw();
        
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
} 