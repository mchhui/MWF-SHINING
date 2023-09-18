package com.modularwarfare.api;

import com.google.gson.annotations.SerializedName;
import net.minecraft.inventory.EntityEquipmentSlot;

public enum MWArmorType {

    @SerializedName("head") HEAD,
    @SerializedName("chest") CHEST,
    @SerializedName("legs") LEGS,
    @SerializedName("feet") FEET,
    @SerializedName("vest") VEST(1);

    int[] validSlots;

    private MWArmorType(int... validSlots) {
        this.validSlots = validSlots;
    }

    public static MWArmorType fromVanillaSlot(EntityEquipmentSlot entityEquipmentSlot) {
        if (entityEquipmentSlot == EntityEquipmentSlot.HEAD) {
            return HEAD;
        }
        if (entityEquipmentSlot == EntityEquipmentSlot.CHEST) {
            return CHEST;
        }
        if (entityEquipmentSlot == EntityEquipmentSlot.LEGS) {
            return LEGS;
        }
        if (entityEquipmentSlot == EntityEquipmentSlot.FEET) {
            return FEET;
        }
        return null;
    }

    public static boolean isVanilla(MWArmorType type) {
        return type == HEAD || type == CHEST || type == LEGS || type == FEET;
    }

    public boolean hasSlot(int slot) {
        for (int s : validSlots) {
            if (s == slot) return true;
        }
        return false;
    }

    public int[] getValidSlots() {
        return validSlots;
    }

}