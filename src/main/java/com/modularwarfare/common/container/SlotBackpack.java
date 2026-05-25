package com.modularwarfare.common.container;

import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.utility.ModUtil;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Backpack slot in the players inventory
 */
public class SlotBackpack extends SlotItemHandler {
    public SlotBackpack(final IItemHandler inv, final int index, final int xPosition, final int yPosition) {
        super(inv, index, xPosition, yPosition);
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    @Override
    public boolean isItemValid(@Nonnull final ItemStack stack) {
        return stack.getItem() instanceof ItemBackpack;
    }

    @Override
    @Nullable
    @SideOnly(Side.CLIENT)
    public String getSlotTexture() {
        return ModUtil.SLOT_TEXTURE_BACKPACK;
    }
}
