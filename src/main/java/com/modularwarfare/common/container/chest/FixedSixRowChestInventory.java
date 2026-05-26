package com.modularwarfare.common.container.chest;

import com.modularwarfare.utility.ChestGuiLayout;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

public class FixedSixRowChestInventory implements IInventory {

    private final IInventory delegate;

    public FixedSixRowChestInventory(final IInventory delegate) {
        this.delegate = delegate;
    }

    public IInventory getDelegate() {
        return this.delegate;
    }

    public boolean isRealSlot(final int index) {
        return index >= 0 && index < this.delegate.getSizeInventory();
    }

    @Override
    public int getSizeInventory() {
        return ChestGuiLayout.CHEST_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(final int index) {
        return this.isRealSlot(index) ? this.delegate.getStackInSlot(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(final int index, final int amount) {
        return this.isRealSlot(index) ? this.delegate.decrStackSize(index, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(final int index) {
        return this.isRealSlot(index) ? this.delegate.removeStackFromSlot(index) : ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(final int index, final ItemStack stack) {
        if (this.isRealSlot(index)) {
            this.delegate.setInventorySlotContents(index, stack);
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return this.delegate.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        this.delegate.markDirty();
    }

    @Override
    public boolean isUsableByPlayer(final EntityPlayer player) {
        return this.delegate.isUsableByPlayer(player);
    }

    @Override
    public void openInventory(final EntityPlayer player) {
        this.delegate.openInventory(player);
    }

    @Override
    public void closeInventory(final EntityPlayer player) {
        this.delegate.closeInventory(player);
    }

    @Override
    public boolean isItemValidForSlot(final int index, final ItemStack stack) {
        return this.isRealSlot(index) && this.delegate.isItemValidForSlot(index, stack);
    }

    @Override
    public int getField(final int id) {
        return this.delegate.getField(id);
    }

    @Override
    public void setField(final int id, final int value) {
        this.delegate.setField(id, value);
    }

    @Override
    public int getFieldCount() {
        return this.delegate.getFieldCount();
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public String getName() {
        return this.delegate.getName();
    }

    @Override
    public boolean hasCustomName() {
        return this.delegate.hasCustomName();
    }

    @Override
    public ITextComponent getDisplayName() {
        return this.delegate.getDisplayName();
    }
}
