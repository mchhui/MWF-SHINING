package com.modularwarfare.common.guns;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public enum WeaponType {

    /**
     * enum weaponType
     * CUSTOM, PISTOL, MP, SMG, CARBINE, RIFLE, AR, DMR, SNIPER, SHOTGUN, etc
     */
    @SerializedName("custom") Custom("Custom"),
    @SerializedName("pistol") Pistol("Pistol"),
    @SerializedName("revolver") Revolver("Revolver"),
    @SerializedName("mp") MP("MP"),
    @SerializedName("smg") SMG("SMG"),
    @SerializedName("carbine") Carbine("Carbine"),
    @SerializedName("rifle") RIFLE("Rifle"),
    @SerializedName("ar") AR("Assault Rifle"),
    @SerializedName("dmr") DMR("DMR"),
    @SerializedName("semisniper") SemiSniper("Semi-sniper"),
    @SerializedName("boltsniper") BoltSniper("Bolt-sniper"),
    @SerializedName("shotgun") Shotgun("Shotgun"),
    @SerializedName("launcher") Launcher("Launcher");


    public String typeName;

    WeaponType(String typeName) {
        this.typeName = typeName;
    }

    public static WeaponType fromEventName(String typeName) {
        if (typeName != null) {
            return Arrays.stream(values())
                    .filter(weaponType -> weaponType.typeName.equalsIgnoreCase(typeName))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

}