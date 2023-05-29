package com.modularwarfare.common.guns;

import com.google.gson.annotations.SerializedName;
import com.modularwarfare.common.type.BaseType;

import java.util.Arrays;

public class SkinType {


    public String internalName;
    public String displayName;
    public String skinAsset;
    public Sampling sampling = Sampling.FLAT;
    public Texture[] textures = new Texture[0];

    public SkinType() {
        /**
         * Disable by default the texture preloading
         */
        //textures[0] = Texture.BASIC;
    }

    public static SkinType getDefaultSkin(BaseType baseType) {
        SkinType skinType = new SkinType();
        skinType.internalName = baseType.internalName;
        skinType.skinAsset = skinType.getSkin();
        skinType.displayName = baseType.displayName + " - Default";
        return skinType;
    }

    public String getSkin() {
        return skinAsset != null ? skinAsset : internalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkinType skinType = (SkinType) o;

        if (!internalName.equals(skinType.internalName)) return false;
        if (!displayName.equals(skinType.displayName)) return false;
        if (!skinAsset.equals(skinType.skinAsset)) return false;
        if (sampling != skinType.sampling) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(textures, skinType.textures);
    }

    @Override
    public int hashCode() {
        int result = internalName.hashCode();
        result = 31 * result + displayName.hashCode();
        result = 31 * result + skinAsset.hashCode();
        result = 31 * result + sampling.hashCode();
        result = 31 * result + Arrays.hashCode(textures);
        return result;
    }

    @Override
    public String toString() {
        return skinAsset;
    }

    public enum Sampling {
        @SerializedName("flat") FLAT,
        @SerializedName("linear") LINEAR;
    }

    public enum Texture {
        @SerializedName("basic") BASIC("skins/%s/%s.png"),
        @SerializedName("glow") GLOW("skins/%s/%s_glow.png"),
        @SerializedName("specular") SPECULAR("skins/%s/%s_s.png"),
        @SerializedName("normal") NORMAL("skins/%s/%s_n.png");

        public String format;

        Texture(String format) {
            this.format = format;
        }
    }

}
