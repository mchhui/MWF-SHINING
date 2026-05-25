package com.modularwarfare.mixin.client.accessor;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface IGuiContainerAccessor {

    @Accessor("inventorySlots")
    Container getInventorySlots();
}
