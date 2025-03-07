package com.modularwarfare.common.guns;

import com.google.gson.annotations.SerializedName;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

public enum PotionEffectEnum {

    @SerializedName("speed") SPEED,
    @SerializedName("slowness") SLOWNESS,
    @SerializedName("haste") HASTE,
    @SerializedName("mining_fatigue") MINING_FATIGUE,
    @SerializedName("strength") STRENGTH,
    @SerializedName("instant_health") INSTANT_HEALTH,
    @SerializedName("instant_damage") INSTANT_DAMAGE,
    @SerializedName("jump_boost") JUMP_BOOST,
    @SerializedName("nausea") NAUSEA,
    @SerializedName("regeneration") REGENERATION,
    @SerializedName("resistance") RESISTANCE,
    @SerializedName("fire_resistance") FIRE_RESISTANCE,
    @SerializedName("water_breathing") WATER_BREATHING,
    @SerializedName("invisibility") INVISIBILITY,
    @SerializedName("blindness") BLINDNESS,
    @SerializedName("night_vision") NIGHT_VISION,
    @SerializedName("hunger") HUNGER,
    @SerializedName("weakness") WEAKNESS,
    @SerializedName("poison") POISON,
    @SerializedName("wither") WITHER,
    @SerializedName("health_boost") HEALTH_BOOST,
    @SerializedName("absorption") ABSORPTION,
    @SerializedName("saturation") SATURATION,
    @SerializedName("glowing") GLOWING,
    @SerializedName("levitation") LEVITATION,
    @SerializedName("luck") LUCK,
    @SerializedName("unluck") UNLUCK,
    @SerializedName("stun") STUN;

    public Potion getPotion() {
        String name = name().toLowerCase();
        Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(name));
        if (potion == null) {
            // 特殊处理自定义药水效果
            if (name.equals("stun")) {
                potion = Potion.REGISTRY.getObject(new ResourceLocation("modularwarfare", "stun"));
            }
            
            if (potion == null) {
                throw new IllegalStateException("Invalid MobEffect requested: " + name);
            }
        }
        return potion;
    }

}
