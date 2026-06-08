package com.modularwarfare.client.gui;

import com.modularwarfare.common.container.ContainerChestModified;
import com.modularwarfare.common.container.chest.IPaddingSlot;
import com.modularwarfare.utility.ChestGuiLayout;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.Slot;


public class GuiChestModified extends GuiContainer {

    private static final int COLOR_PANEL = 0xFF101010;
    private static final int COLOR_PADDING_BORDER = 0xFF1E1E1E;
    private static final int COLOR_PADDING_FILL = 0xC0483838;

    public GuiChestModified(final ContainerChestModified container) {
        super(container);
        this.xSize = ChestGuiLayout.GUI_WIDTH;
        this.ySize = ChestGuiLayout.GUI_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.applyChestSlotLayout();
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
        this.applyChestSlotLayout();
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawPlayerModelOnTop(mouseX, mouseY);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    private void drawPlayerModelOnTop(final int mouseX, final int mouseY) {
        this.prepareTextureDraw();
        GlStateManager.enableDepth();
        final int modelX = this.guiLeft + ChestGuiLayout.MODEL_OUTSIDE_OFFSET_X;
        final int modelY = this.guiTop + ChestGuiLayout.MODEL_OUTSIDE_OFFSET_Y;
        GuiInventory.drawEntityOnScreen(
                modelX,
                modelY,
                30,
                (float) modelX - mouseX,
                (float) modelY - 50.0F - mouseY,
                (EntityLivingBase) this.mc.player);
        this.prepareTextureDraw();
    }

    private void applyChestSlotLayout() {
        if (this.inventorySlots instanceof ContainerChestModified) {
            ((ContainerChestModified) this.inventorySlots).applySlotLayout();
        }
    }

    @Override
    protected void renderHoveredToolTip(final int mouseX, final int mouseY) {
        if (this.isPaddingSlotHovered(mouseX, mouseY)) {
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    private boolean isPaddingSlotHovered(final int mouseX, final int mouseY) {
        final Slot slot = this.getHoveredSlot(mouseX, mouseY);
        return slot instanceof IPaddingSlot && ((IPaddingSlot) slot).isPaddingSlot();
    }

    private Slot getHoveredSlot(final int mouseX, final int mouseY) {
        for (final Slot slot : this.inventorySlots.inventorySlots) {
            if (slot != null && this.isPointInRegion(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
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
        if (!(this.inventorySlots instanceof ContainerChestModified)) {
            return;
        }
        final ContainerChestModified container = (ContainerChestModified) this.inventorySlots;
        final int left = this.guiLeft;
        final int top = this.guiTop;

        this.prepareTextureDraw();
        this.drawRect(left, top, left + this.xSize, top + this.ySize, COLOR_PANEL);

        this.drawChestAreaSlots(left, top, container);
        this.drawBackpackAreaSlots(left, top, container);
        this.drawPlayerAreaSlots(left, top);

        this.prepareTextureDraw();
    }

    private void drawChestAreaSlots(final int guiLeft, final int guiTop, final ContainerChestModified container) {
        for (int index = 0; index < ChestGuiLayout.CHEST_SLOTS; index++) {
            final int col = index % ChestGuiLayout.CHEST_COLS;
            final int row = index / ChestGuiLayout.CHEST_COLS;
            final int x = guiLeft + ChestGuiLayout.chestSlotBgX(col);
            final int y = guiTop + ChestGuiLayout.chestSlotBgY(row);
            if (index >= container.chestSlotCount) {
                this.drawDisabledSlotFrame(x, y);
            } else {
                this.drawSlotTexture(x, y);
            }
        }
    }

    private void drawBackpackAreaSlots(final int guiLeft, final int guiTop, final ContainerChestModified container) {
        final int usedSlots = Math.min(container.getBackpackContentSlotCount(), ChestGuiLayout.BACKPACK_DISPLAY_SLOTS);
        for (int index = 0; index < ChestGuiLayout.BACKPACK_DISPLAY_SLOTS; index++) {
            final int col = index % ChestGuiLayout.BACKPACK_COLS;
            final int row = index / ChestGuiLayout.BACKPACK_COLS;
            final int x = guiLeft + ChestGuiLayout.backpackSlotBgX(col);
            final int y = guiTop + ChestGuiLayout.backpackSlotBgY(row);
            if (index >= usedSlots) {
                this.drawDisabledSlotFrame(x, y);
            } else {
                this.drawSlotTexture(x, y);
            }
        }
    }

    private void drawPlayerAreaSlots(final int guiLeft, final int guiTop) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.drawSlotTexture(
                        guiLeft + ChestGuiLayout.playerSlotBgX(col),
                        guiTop + ChestGuiLayout.playerSlotBgY(row));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.drawSlotTexture(
                    guiLeft + ChestGuiLayout.playerSlotBgX(col),
                    guiTop + ChestGuiLayout.hotbarSlotBgY());
        }
        for (int i = 0; i < 7; i++) {
            this.drawSlotTexture(
                    guiLeft + ChestGuiLayout.equipSlotBgX(),
                    guiTop + ChestGuiLayout.equipSlotBgY(i));
        }
    }

    private void drawSlotTexture(final int x, final int y) {
        this.prepareTextureDraw();
        this.mc.getTextureManager().bindTexture(GuiInventoryModified.ICONS);
        this.drawTexturedModalRect(x, y, 0, 0, ChestGuiLayout.SLOT, ChestGuiLayout.SLOT);
    }

    private void drawDisabledSlotFrame(final int x, final int y) {
        this.drawRect(x, y, x + ChestGuiLayout.SLOT, y + ChestGuiLayout.SLOT, COLOR_PADDING_BORDER);
        this.drawRect(x + 1, y + 1, x + ChestGuiLayout.SLOT - 1, y + ChestGuiLayout.SLOT - 1, COLOR_PADDING_FILL);
    }

    private void prepareTextureDraw() {
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
    }
}
