package com.modularwarfare.common.guns;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public enum WeaponAnimationType {
    /**
     * Basic animation type, used for default guns.
     */
    @SerializedName("basic") BASIC,

    /**
     * Enhanced animation type, used for guns with a more complex reload animation.
     */
    @SerializedName("enhanced") ENHANCED;

    public static WeaponAnimationType fromString(String modeName) {
        return Arrays.stream(values())
                .filter(animationType -> animationType.name().equalsIgnoreCase(modeName))
                .findFirst()
                .orElse(null);
    }

}
