package com.modularwarfare.common.guns;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

/**
 * WTF???
 * R.I.P
 */
@Deprecated
public enum WeaponScopeType {

    @SerializedName("default") DEFAULT,

    @SerializedName("reddot") REDDOT,

    @SerializedName("2x") TWO,

    @SerializedName("4x") FOUR,

    @SerializedName("8x") EIGHT,

    @SerializedName("15x") FIFTEEN;

    public static WeaponScopeType fromString(String modeName) {
        return Arrays.stream(values())
                .filter(scopeType -> scopeType.name().equalsIgnoreCase(modeName))
                .findFirst()
                .orElse(null);
    }

}
