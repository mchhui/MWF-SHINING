package com.modularwarfare.common.container.chest;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;


public class SlotChestPadding extends net.minecraft.inventory.Slot implements IPaddingSlot {

    private final boolean padding;

    public SlotChestPadding(final IInventory inventory, final int index, final int x, final int y) {
        super(inventory, index, x, y);
        this.padding = inventory instanceof FixedSixRowChestInventory
                && !((FixedSixRowChestInventory) inventory).isRealSlot(index);
    }

    @Override
    public boolean isPaddingSlot() {
        return this.padding;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isItemValid(final ItemStack stack) {
        return !this.padding && super.isItemValid(stack);
    }

    @Override
    public boolean canTakeStack(final EntityPlayer playerIn) {
        return !this.padding && super.canTakeStack(playerIn);
    }

    @Override
    public void putStack(final ItemStack stack) {
        if (this.padding) {
            return;
        }
        super.putStack(stack);
    }

    @Override
    public ItemStack decrStackSize(final int amount) {
        if (this.padding) {
            return ItemStack.EMPTY;
        }
        return super.decrStackSize(amount);
    }

    @Override
    public ItemStack onTake(final EntityPlayer playerIn, final ItemStack stack) {
        if (this.padding) {
            return ItemStack.EMPTY;
        }
        return super.onTake(playerIn, stack);
    }
}
