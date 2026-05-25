package com.modularwarfare.mixin.client.accessor;

import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.inventory.GuiChest;

@Mixin(GuiChest.class)
public interface IGuiChestAccessor {

    @Accessor("upperChestInventory")
    IInventory getUpperChestInventory();

    @Accessor("lowerChestInventory")
    IInventory getLowerChestInventory();

    @Accessor("inventoryRows")
    int getInventoryRows();
}
