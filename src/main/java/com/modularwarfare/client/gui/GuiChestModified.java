package com.modularwarfare.client.gui;

import com.modularwarfare.common.container.ContainerChestModified;
import com.modularwarfare.utility.ChestGuiLayout;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;


public class GuiChestModified extends GuiContainer {

    private static final ItemStack BARRIER_PLACEHOLDER = new ItemStack(Blocks.BARRIER);

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
        if (!(this.mc.player.openContainer instanceof ContainerChestModified)) {
            return;
        }
        this.oldMouseX = mouseX;
        this.oldMouseY = mouseY;
        this.drawDefaultBackground();
        this.drawPlayerModelOutsidePanel();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
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
        this.drawBarrierPlaceholders();
    }

    private String getChestSectionTitle() {
        return ((ContainerChestModified) this.inventorySlots).chestInventory.getDisplayName().getFormattedText();
    }

    private String getBackpackSectionTitle() {
        if (this.inventorySlots instanceof ContainerChestModified) {
            final ContainerChestModified container = (ContainerChestModified) this.inventorySlots;
            if (container.extra != null) {
                final ItemStack backpack = container.extra.getStackInSlot(0);
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

    private void drawBarrierPlaceholders() {
        if (!(this.inventorySlots instanceof ContainerChestModified)) {
            return;
        }
        final ContainerChestModified container = (ContainerChestModified) this.inventorySlots;

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();

        for (int index = container.chestSlotCount; index < ChestGuiLayout.CHEST_SLOTS; index++) {
            final int col = index % ChestGuiLayout.CHEST_COLS;
            final int row = index / ChestGuiLayout.CHEST_COLS;
            this.drawBarrierAt(ChestGuiLayout.chestSlotX(col), ChestGuiLayout.chestSlotY(row));
        }

        final int backpackUsed = container.getBackpackContentSlotCount();
        for (int index = backpackUsed; index < ChestGuiLayout.BACKPACK_DISPLAY_SLOTS; index++) {
            final int col = index % ChestGuiLayout.BACKPACK_COLS;
            final int row = index / ChestGuiLayout.BACKPACK_COLS;
            this.drawBarrierAt(ChestGuiLayout.backpackSlotX(col), ChestGuiLayout.backpackSlotY(row));
        }

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    private void drawBarrierAt(final int relX, final int relY) {
        this.itemRender.renderItemAndEffectIntoGUI(BARRIER_PLACEHOLDER, relX, relY);
        this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, BARRIER_PLACEHOLDER, relX, relY, null);
    }
}
