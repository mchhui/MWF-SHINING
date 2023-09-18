package com.modularwarfare.common.guns;


import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public enum WeaponFireMode {

    /**
     * SemiAutomatic fire mode
     */
    @SerializedName("semi") SEMI,

    /**
     * Fully automatic fire mode
     */
    @SerializedName("full") FULL,

    /**
     * Burst of shots fire mode
     */
    @SerializedName("burst") BURST;

    public static WeaponFireMode fromString(String modeName) {
        return Arrays.stream(values())
                .filter(fireMode -> fireMode.name().equalsIgnoreCase(modeName))
                .findFirst()
                .orElse(null);
    }

}
