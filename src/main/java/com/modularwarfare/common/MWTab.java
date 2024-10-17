package com.modularwarfare.common;

import com.google.common.collect.Ordering;
import com.modularwarfare.ModularWarfare;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;

public class MWTab extends CreativeTabs {

    public Comparator<ItemStack> tabSorter;

    public String contentPack;

    public MWTab(String contentPack) {
        super("MW:" + contentPack);
        this.contentPack = contentPack;
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public String getTranslationKey() {
        String name = contentPack;
        if (name.endsWith(".zip")) {
            name = name.replace(".zip", "");
        } else if (name.endsWith(".jar")) {
            name = name.replace(".jar", "");
        }
        return TextFormatting.RED + "[MW] " + TextFormatting.WHITE + name;
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon() {
        final ItemStack[] itemStack = {new ItemStack(Items.IRON_AXE)};

        ModularWarfare.gunTypes.forEach((s, gun) -> {
            if (gun.type.contentPack.equals(contentPack)) {
                itemStack[0] = new ItemStack(gun);
            }
        });

        return itemStack[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(@Nonnull NonNullList<ItemStack> items) {
        super.displayAllRelevantItems(items);
        items.sort(tabSorter);
    }

    public void preInitialize(List<Item> order) {
        tabSorter = Ordering.explicit(order).onResultOf(ItemStack::getItem);
    }
}
