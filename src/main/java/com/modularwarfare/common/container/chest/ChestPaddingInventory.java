package com.modularwarfare.common.container.chest;

import com.modularwarfare.utility.ChestGuiLayout;
import net.minecraft.inventory.InventoryBasic;

public final class ChestPaddingInventory extends InventoryBasic {

    public static final ChestPaddingInventory INSTANCE = new ChestPaddingInventory();

    private ChestPaddingInventory() {
        super("mwf_chest_padding", false, ChestGuiLayout.BACKPACK_DISPLAY_SLOTS);
    }
}
