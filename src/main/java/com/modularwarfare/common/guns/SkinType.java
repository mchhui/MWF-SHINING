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
        // Disable by default the texture preloading
        // textures[0] = Texture.BASIC;
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
        int result = internalName != null ? internalName.hashCode() : 0;
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (skinAsset != null ? skinAsset.hashCode() : 0);
        result = 31 * result + (sampling != null ? sampling.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(textures);
        return result;
    }

    @Override
    public String toString() {
        return skinAsset;
    }

    public enum Sampling {
        /**
         * Flat sampling is used for skins that are not affected by the shape of the model
         */
        @SerializedName("flat") FLAT,
        /**
         * Linear sampling is used for skins that are affected by the shape of the model
         */
        @SerializedName("linear") LINEAR;
    }

    public enum Texture {
        /**
         * Basic texture is the main texture of the model
         */
        @SerializedName("basic") BASIC("skins/%s/%s.png"),
        /**
         * Glow texture is used to add a glowing effect to the model
         */
        @SerializedName("glow") GLOW("skins/%s/%s_glow.png"),
        /**
         * Specular texture is used to add a specular effect to the model
         */
        @SerializedName("specular") SPECULAR("skins/%s/%s_s.png"),
        /**
         * Normal texture is used to add a normal effect to the model
         */
        @SerializedName("normal") NORMAL("skins/%s/%s_n.png");

        public final String format;

        Texture(String format) {
            this.format = format;
        }
    }

}
