package com.modularwarfare.client.gui;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.container.ContainerInventoryModified;
import com.modularwarfare.utility.ModUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class GuiInventoryModified extends InventoryEffectRenderer {
    public static final ResourceLocation ICONS;
    public static final ResourceLocation INVENTORY_BG;

    static {
        ICONS = new ResourceLocation(ModularWarfare.MOD_ID, "textures/gui/icons.png");
        INVENTORY_BG = new ResourceLocation(ModularWarfare.MOD_ID, "textures/gui/inventory.png");
    }

    public GuiInventoryModified(final EntityPlayer player) {
        super(new ContainerInventoryModified(player.inventory, !player.getEntityWorld().isRemote, player));
        this.allowUserInput = true;
        this.xSize = 176;
        /** The Y size of the inventory window in pixels. */
        this.ySize = 185;
    }

    private void resetGuiLeft() {
        this.guiLeft = (this.width - this.xSize) / 2;
    }

    public void updateScreen() {
        this.updateActivePotionEffects();
        this.resetGuiLeft();
    }

    public void initGui() {
        this.buttonList.clear();
        super.initGui();
        this.resetGuiLeft();
    }

    protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY) {
        //this.fontRenderer.drawString(I18n.format("PlayerStats:", new Object[0]), 103, 10, 20210712);
        //this.fontRenderer.drawString(I18n.format("- Deaths ", new Object[0]), 103, 19, 20210712);
    }

    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        if (!(Minecraft.getMinecraft().player.openContainer instanceof ContainerInventoryModified)) {
            return;
        }
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawPlayerModel(mouseX, mouseY);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    private void drawPlayerModel(final int mouseX, final int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GuiInventory.drawEntityOnScreen(
                this.guiLeft + ModUtil.PLAYER_MODEL_PREVIEW_X,
                this.guiTop + ModUtil.PLAYER_MODEL_PREVIEW_Y,
                30,
                (float) (this.guiLeft + ModUtil.PLAYER_MODEL_PREVIEW_X) - mouseX,
                (float) (this.guiTop + ModUtil.PLAYER_MODEL_PREVIEW_Y) - 50.0F - mouseY,
                this.mc.player);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.9f);
        this.mc.getTextureManager().bindTexture(GuiInventoryModified.INVENTORY_BG);
        final int k = this.guiLeft;
        final int l = this.guiTop;
        this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
        final ContainerInventoryModified containter = (ContainerInventoryModified) Minecraft.getMinecraft().player.openContainer;
        final IItemHandler backpack = (IItemHandler) containter.extra;
        if (backpack.getStackInSlot(0).hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, (EnumFacing) null)) {
            final ItemStack stack = backpack.getStackInSlot(0);
            final IItemHandler backpackInv = (IItemHandler) stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, (EnumFacing) null);
            final int x = k + ModUtil.BACKPACK_CONTENT_OFFSET_X;
            final int y = l + ModUtil.BACKPACK_CONTENT_OFFSET_Y;
            final int cols = ModUtil.BACKPACK_CONTENT_COLS;
            final int slotCount = backpackInv.getSlots();
            final int rows = (slotCount + cols - 1) / cols;
            final int panelWidth = ModUtil.BACKPACK_CONTENT_PANEL_WIDTH;

            this.mc.getTextureManager().bindTexture(GuiInventoryModified.ICONS);
            this.drawTexturedModalRect(x - 5, y - 18, 18, 0, panelWidth, 18);
            for (int row = 0; row < rows; row++) {
                this.drawTexturedModalRect(x - 5, y + row * ModUtil.INVENTORY_SLOT_SIZE_PIXELS, 18, 5, panelWidth, 18);
                for (int col = 0; col < cols; col++) {
                    final int index = row * cols + col;
                    if (index >= slotCount) {
                        break;
                    }
                    this.drawSlotBackground(
                            x + col * ModUtil.INVENTORY_SLOT_SIZE_PIXELS,
                            -1 + y + row * ModUtil.INVENTORY_SLOT_SIZE_PIXELS);
                }
            }
            this.drawTexturedModalRect(x - 5, -1 + y + rows * ModUtil.INVENTORY_SLOT_SIZE_PIXELS, 18, 33, panelWidth, 5);

            if (stack != null) {
                ItemBackpack backpackItem = (ItemBackpack) stack.getItem();
                this.fontRenderer.drawString(backpackItem.type.displayName, x, y - 12, 16777215);
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.color(1.0f, 1.0f, 1.0f);
        }
    }

    protected void actionPerformed(final GuiButton button) {
    }

    public void drawSlotBackground(final int x, final int y) {
        this.mc.getTextureManager().bindTexture(GuiInventoryModified.ICONS);
        this.drawTexturedModalRect(x, y, 0, 0, 18, 18);
    }
}
