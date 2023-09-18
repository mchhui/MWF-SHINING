package com.modularwarfare.common.guns;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public enum WeaponDotColorType {

    @SerializedName("red") RED,

    @SerializedName("blue") BLUE,

    @SerializedName("green") GREEN;


    public static WeaponDotColorType fromString(String modeName) {
        return Arrays.stream(values())
                .filter(dotColorType -> dotColorType.name().equalsIgnoreCase(modeName))
                .findFirst()
                .orElse(null);
    }

}
