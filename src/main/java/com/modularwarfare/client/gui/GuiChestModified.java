package com.modularwarfare.client.gui;

import com.modularwarfare.common.container.chest.ChestPlaceholderItems;
import com.modularwarfare.common.container.ContainerChestModified;
import com.modularwarfare.common.container.chest.IPaddingSlot;
import com.modularwarfare.utility.ChestGuiLayout;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.Slot;


public class GuiChestModified extends GuiContainer {

    private static final int COLOR_PANEL = 0xC0101010;
    private static final int COLOR_SLOT_BORDER = 0xFF373737;
    private static final int COLOR_SLOT_FILL = 0xFF8B8B8B;

    private float oldMouseX;
    private float oldMouseY;

    public GuiChestModified(final ContainerChestModified container) {
        super(container);
        this.xSize = ChestGuiLayout.GUI_WIDTH;
        this.ySize = ChestGuiLayout.GUI_HEIGHT;
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        if (!(this.inventorySlots instanceof ContainerChestModified)) {
            return;
        }
        final ContainerChestModified container = (ContainerChestModified) this.inventorySlots;
        if (this.mc.player.openContainer != container) {
            this.mc.player.openContainer = container;
        }
        this.oldMouseX = mouseX;
        this.oldMouseY = mouseY;
        container.applySlotLayout();
        this.drawDefaultBackground();
        this.drawPlayerModelOutsidePanel();
        super.drawScreen(mouseX, mouseY, partialTicks);
        container.applySlotLayout();
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void renderHoveredToolTip(final int mouseX, final int mouseY) {
        if (this.isPaddingSlotHovered(mouseX, mouseY)) {
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    private boolean isPaddingSlotHovered(final int mouseX, final int mouseY) {
        for (final Slot slot : this.inventorySlots.inventorySlots) {
            if (slot == null || !this.isPointInRegion(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY)) {
                continue;
            }
            if (slot instanceof IPaddingSlot && ((IPaddingSlot) slot).isPaddingSlot()) {
                return true;
            }
            if (ChestPlaceholderItems.isPlaceholder(slot.getStack())) {
                return true;
            }
        }
        return false;
    }

    /** 模型绘制在面板左缘外侧，避免遮挡箱子格 */
    private void drawPlayerModelOutsidePanel() {
        GuiPlayerInventory.drawEntityOnScreen(
                this.guiLeft + ChestGuiLayout.MODEL_OUTSIDE_OFFSET_X,
                this.guiTop + ChestGuiLayout.MODEL_OUTSIDE_OFFSET_Y,
                30,
                this.guiLeft + ChestGuiLayout.MODEL_OUTSIDE_OFFSET_X - this.oldMouseX,
                this.guiTop + ChestGuiLayout.MODEL_OUTSIDE_OFFSET_Y - 50 - this.oldMouseY,
                (EntityLivingBase) this.mc.player);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(this.getChestSectionTitle(), ChestGuiLayout.CHEST_X, 6, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), ChestGuiLayout.CHEST_X, ChestGuiLayout.PLAYER_Y - 12, 0x404040);
        this.fontRenderer.drawString(this.getBackpackSectionTitle(), ChestGuiLayout.BACKPACK_X, 6, 0x404040);
    }

    private String getChestSectionTitle() {
        return ((ContainerChestModified) this.inventorySlots).chestInventory.getDisplayName().getFormattedText();
    }

    private String getBackpackSectionTitle() {
        if (this.inventorySlots instanceof ContainerChestModified) {
            final ContainerChestModified container = (ContainerChestModified) this.inventorySlots;
            if (container.extra != null) {
                final net.minecraft.item.ItemStack backpack = container.extra.getStackInSlot(0);
                if (!backpack.isEmpty()) {
                    return backpack.getDisplayName();
                }
            }
        }
        return I18n.format("mwf:gui.chest.backpack");
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        final int left = this.guiLeft;
        final int top = this.guiTop;

        this.drawRect(left, top, left + this.xSize, top + this.ySize, COLOR_PANEL);

        this.drawSlotGrid(left, top, ChestGuiLayout.CHEST_X, ChestGuiLayout.CHEST_Y, ChestGuiLayout.CHEST_COLS, ChestGuiLayout.CHEST_ROWS);
        this.drawSlotGrid(left, top, ChestGuiLayout.BACKPACK_X, ChestGuiLayout.BACKPACK_Y, ChestGuiLayout.BACKPACK_COLS, ChestGuiLayout.BACKPACK_ROWS);
        this.drawSlotGrid(left, top, ChestGuiLayout.CHEST_X, ChestGuiLayout.PLAYER_Y, 9, 3);
        this.drawSlotGrid(left, top, ChestGuiLayout.CHEST_X, ChestGuiLayout.HOTBAR_Y, 9, 1);
        this.drawSlotGrid(left, top, ChestGuiLayout.EQUIP_X, ChestGuiLayout.EQUIP_Y, 1, 7);
    }

    private void drawSlotGrid(final int guiLeft, final int guiTop, final int originX, final int originY, final int cols, final int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.drawSlotFrame(guiLeft + originX + col * ChestGuiLayout.SLOT, guiTop + originY + row * ChestGuiLayout.SLOT);
            }
        }
    }

    private void drawSlotFrame(final int x, final int y) {
        this.drawRect(x, y, x + ChestGuiLayout.SLOT, y + ChestGuiLayout.SLOT, COLOR_SLOT_BORDER);
        this.drawRect(x + 1, y + 1, x + ChestGuiLayout.SLOT - 1, y + ChestGuiLayout.SLOT - 1, COLOR_SLOT_FILL);
    }
}
