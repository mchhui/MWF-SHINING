package com.modularwarfare.common.guns;

import com.google.gson.annotations.SerializedName;

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
        for (WeaponAnimationType animationType : values()) {
            if (animationType.name().equalsIgnoreCase(modeName)) {
                return animationType;
            }
        }
        return null;
    }

}
