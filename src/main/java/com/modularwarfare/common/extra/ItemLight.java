package com.modularwarfare.common.extra;

import com.modularwarfare.ModularWarfare;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class ItemLight extends Item {

    public ItemLight(final String name) {
        ResourceLocation registryName = new ResourceLocation(ModularWarfare.MOD_ID, name);
        String translationKey = registryName.getPath();
        this.setRegistryName(registryName);
        this.setTranslationKey(translationKey);
    }

}
