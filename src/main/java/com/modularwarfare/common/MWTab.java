package com.modularwarfare.common;

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
import java.util.Map;
import java.util.HashMap;

public class MWTab extends CreativeTabs {

    public Comparator<ItemStack> tabSorter;
    
    private Map<Item, Integer> itemOrderMap;

    public String contentPack;
    
    private Item firstItem;

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
        if (firstItem != null) {
            return new ItemStack(firstItem);
        }
        return new ItemStack(Items.IRON_AXE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(@Nonnull NonNullList<ItemStack> items) {
        super.displayAllRelevantItems(items);
        if (tabSorter != null) {
            items.sort(tabSorter);
        }
    }

    public void preInitialize(List<Item> order) {
        itemOrderMap = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            itemOrderMap.put(order.get(i), i);
        }
        
        tabSorter = (stack1, stack2) -> {
            Item item1 = stack1.getItem();
            Item item2 = stack2.getItem();
            
            Integer order1 = itemOrderMap.get(item1);
            Integer order2 = itemOrderMap.get(item2);
            
            if (order1 != null && order2 != null) {
                return Integer.compare(order1, order2);
            }
            
            if (order1 != null) {
                return -1;
            }
            if (order2 != null) {
                return 1;
            }
            
            String name1 = item1.getRegistryName() != null ? item1.getRegistryName().toString() : "";
            String name2 = item2.getRegistryName() != null ? item2.getRegistryName().toString() : "";
            return name1.compareTo(name2);
        };
        
        if (!order.isEmpty()) {
            firstItem = order.get(0);
        }
    }
}
