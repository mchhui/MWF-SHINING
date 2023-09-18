package com.modularwarfare.client.gui;

import com.modularwarfare.client.gui.api.GuiUtils;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.utility.RenderHelperMW;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class TextureButton extends GuiButton {
    public int isOver = 2;
    public int colorText = 0xFFFF5555;
    public int colorTheme = 0x55000000;
    public boolean hidden = false;
    public int state = -1;
    public double x;
    public double y;
    private ResourceLocation iconTexture = null;
    private int toolTipY;
    private String toolTip;
    private boolean showToolTip = false;
    private int xMovement;
    private AttachmentPresetEnum attachment;
    private ItemStack itemStack = ItemStack.EMPTY;
    private TypeEnum type;

    public TextureButton(int buttonId, double x, double y, String givenText) {
        super(buttonId, (int) x, (int) y, givenText);
    }

    public TextureButton(int buttonId, double x, double y, int givenWidth, int givenHeight, String givenText) {
        this(buttonId, x, y, givenText);
        this.width = givenWidth;
        this.height = givenHeight;
    }
    public TextureButton(int id, double x, double y, int width, int height, ResourceLocation iconTexture) {
        this(id, x, y, width, height, "");
        this.x = x;
        this.y = y;
        this.iconTexture = iconTexture;
    }

    public TypeEnum getType() {
        return type;
    }

    public TextureButton setType(TypeEnum type) {
        this.type = type;
        return this;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public TextureButton setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        return this;
    }

    public AttachmentPresetEnum getAttachmentType() {
        return attachment;
    }

    public TextureButton setAttachment(AttachmentPresetEnum attachment) {
        this.attachment = attachment;
        return this;
    }

    public TextureButton addToolTip(String givenToolTip) {
        this.showToolTip = true;
        this.toolTip = givenToolTip;
        return this;
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return this.enabled && this.visible && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        if (hidden) {
            if (hovered || this.state == 0) {
                visible = true;
            } else {
                visible = false;
            }
        }
        if (visible) {

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            isOver = getHoverState(hovered);

//            if (drawBackground) {
//                if (drawShadow) {
//                    GuiUtils.renderRectWithOutline(x, y, width, height, colorTheme, colorTheme, 1);
//                } else {
//                    GuiUtils.renderRect(x, y, width, height, buttonColor);
//                }
//            }
            mouseDragged(mc, mouseX, mouseY);
            String displayText = displayString;
            if (isOver == 2) {

//                if (!soundPlayed) {
//                    //Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(ModSounds.CLICK, 1.0F));
//                    soundPlayed = true;
//                }
//
//                if (!animationStarted) {
//                    animationStarted = true;
//                }

//                if (fade <= 0) {
//                    fade = 0;
//                } else {
//                    fade -= .2;
//                }

//                if (xMovement < 3) {
//                    xMovement++;
//                }

                GlStateManager.pushMatrix();
                //GuiUtils.renderRectWithFade((int)x, (int)y, width, height, 0x22000000, 0.1f);
                GlStateManager.popMatrix();

            } else {
//                toolTipY = 0;
//                fade = 1;
//                soundPlayed = false;
//                animationStarted = false;
//                if (xMovement > 0) {
//                    xMovement--;
//                }
            }
            if (iconTexture != null) {
                if (isOver == 2) {
                    GuiUtils.renderColor(0x999999);
                } else {
                    GuiUtils.renderColor(0xFFFFFF);
                }
                //GuiUtils.renderImage(x + width / 2 - 8, y + (height - 8) - 10, iconTexture, 16.0D, 16.0D);
                Minecraft.getMinecraft().renderEngine.bindTexture(iconTexture);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                RenderHelperMW.drawTexturedRect(x, y, width + 0.05d, height);
                GL11.glColor3f(1.0F, 1.0F, 1.0F);
                if (isOver == 2) {
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.4f);
                    //GlStateManager.color(1, 1, 1);
                    GlStateManager.disableTexture2D();
                    GlStateManager.enableBlend();
                    RenderHelperMW.drawTexturedRect(x, y, width + 0.05d, height);//,0XFFFFFF
                    GlStateManager.enableTexture2D();
                    //GlStateManager.disableBlend();
                }
            }
            //GlStateManager.translate(0, 0, 5000);
            //Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRenderer, itemStack, (int)x, (int)y, null);
            if (!this.itemStack.isEmpty()) {
                GlStateManager.pushMatrix();
                double scale = 5.9d / scaledresolution.getScaleFactor() * 0.9D;
                GlStateManager.translate(0, 0, -135);
                GlStateManager.scale(scale, scale, scale);

                RenderHelperMW.renderItemStack(itemStack, (int) ((x + 3) / scale), (int) ((y + 2.5d) / scale), partialTicks, false);
                GlStateManager.popMatrix();

                GlStateManager.disableDepth();
                GlStateManager.pushMatrix();
                scale = 6.0d / scaledresolution.getScaleFactor() * 0.4D;
                GlStateManager.translate(0, 0, 1);
                GlStateManager.scale(scale, scale, scale);
                String str = itemStack.getDisplayName();//itemStack.getDisplayName()
                int strW = mc.fontRenderer.getStringWidth(str);
                while (strW > 36) {
                    str = str.substring(0, str.length() - 1);
                    strW = mc.fontRenderer.getStringWidth(str);
                }
                if (strW > 4) {
                    //str=itemStack.getDisplayName().subSequence(0, 4).toString();
                }
                RenderHelperMW.renderText(str, (int) ((x + (4 / scaledresolution.getScaleFactor())) / scale), (int) ((y + (4 / scaledresolution.getScaleFactor())) / scale), 0xFFFFFF);
                GlStateManager.popMatrix();
                GlStateManager.enableDepth();
            } else if (this.attachment != null) {
                GlStateManager.pushMatrix();
                double scale = 6.0d / scaledresolution.getScaleFactor() * 0.5D;
                GlStateManager.scale(scale, scale, scale);
                RenderHelperMW.renderText(I18n.format("mwf:gui.modify.none"), (int) ((x + 2) / scale), (int) ((y + 2) / scale), 0xFFFFFF);
                GlStateManager.popMatrix();
            }

            if (!enabled) {
                displayText = ChatFormatting.GRAY + displayText;
            }
//            if (!centeredText) {
//                GuiUtils.renderTextWithShadow(displayText, x + 2, y + (height - 8) / 2, isOver == 2 ? 0xFFFFFFFF : 0xFFFFFFFF);
//                return;
//            }
            if (this.type == TypeEnum.SideButton) {
                Minecraft.getMinecraft().renderEngine.bindTexture(this.state == 0 ? GuiGunModify.arrow_down : GuiGunModify.arrow_up);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                RenderHelperMW.drawTexturedRect(x, y, width + 0.05d, height);
            }
            if (this.type == TypeEnum.SideButtonVert) {
                Minecraft.getMinecraft().renderEngine.bindTexture(this.state == 0 ? GuiGunModify.arrow_right : GuiGunModify.arrow_left);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                RenderHelperMW.drawTexturedRect(x - (width / 1.7), y, height / 2, height);
            }
            if (this.type == TypeEnum.Slot && !this.itemStack.isEmpty() && isOver == 2) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 500);
                double scale = 1.5d;
                GlStateManager.scale(scale, scale, scale);
                this.toolTip = this.itemStack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL).toString();
                int toolTipWidth = mc.fontRenderer.getStringWidth(this.toolTip);
                GuiUtils.renderRectWithOutline((int) (mouseX / scale), (int) ((mouseY - 10) / scale), toolTipY, 10, colorTheme, colorTheme, 1);
                if (toolTipY < (toolTipWidth + 2)) {

                    int toolTipGap = (toolTipWidth + 2) - toolTipY;

                    if (toolTipGap >= 10) {
                        toolTipY = toolTipY + 10;
                    } else {
                        toolTipY = toolTipY + 1;
                    }

                } else if (toolTipY > (toolTipWidth + 2)) {
                    toolTipY--;
                }

                if (toolTipY >= (toolTipWidth + 2)) {
                    GuiUtils.renderText(this.toolTip, (int) ((mouseX + 1) / scale), (int) ((mouseY - 9) / scale), 0xFFFFFF);
                }
                GlStateManager.popMatrix();
            }

            GuiUtils.renderCenteredTextWithShadow(displayText, ((int) x + width / 2) + xMovement, (int) y + (height - 8) / 2, isOver == 2 ? 0xFFFFFFFF : colorText, 0x000000);
        }

        GlStateManager.popMatrix();

    }

    public enum TypeEnum {
        Button,
        Slot,
        SubSlot,
        SideButton,
        SideButtonVert;
    }
}
