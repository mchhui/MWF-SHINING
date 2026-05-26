package com.modularwarfare.common.container.chest;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;

public final class ChestPlaceholderItems {

    private static final String NBT_PADDING = "MwfChestPadding";

    private static ItemStack cachedBarrier;

    private ChestPlaceholderItems() {
    }

    public static ItemStack getBarrierStack() {
        if (cachedBarrier == null) {
            cachedBarrier = new ItemStack(Blocks.BARRIER);
            cachedBarrier.setTagInfo(NBT_PADDING, new NBTTagByte((byte) 1));
        }
        return cachedBarrier.copy();
    }

    public static boolean isPlaceholder(final ItemStack stack) {
        return !stack.isEmpty()
                && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean(NBT_PADDING);
    }
}
