package com.modularwarfare.common.container.chest;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotBackpackDisplayPadding extends net.minecraft.inventory.Slot implements IPaddingSlot {

    public SlotBackpackDisplayPadding(final IInventory inventory, final int index, final int x, final int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean isPaddingSlot() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isItemValid(final ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeStack(final EntityPlayer playerIn) {
        return false;
    }

    @Override
    public void putStack(final ItemStack stack) {
    }

    @Override
    public ItemStack decrStackSize(final int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack onTake(final EntityPlayer playerIn, final ItemStack stack) {
        return ItemStack.EMPTY;
    }
}
