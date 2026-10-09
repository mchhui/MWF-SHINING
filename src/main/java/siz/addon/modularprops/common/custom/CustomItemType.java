package siz.addon.modularprops.common.custom;

import com.google.gson.annotations.SerializedName;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.type.BaseType;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig;
import siz.addon.modularprops.client.fpp.enhanced.models.ModelEnhancedCustomItem;

import java.util.ArrayList;

public class CustomItemType extends BaseType {

    /**
     * 道具类型枚举
     */
    public enum ItemTypeEnum {
        @SerializedName("generic") GENERIC,
        @SerializedName("tool") TOOL,
        @SerializedName("weapon") WEAPON,
        @SerializedName("consumable") CONSUMABLE,
        @SerializedName("misc") MISC
    }

    /**
     * 道具类型（通过JSON配置）
     */
    public ItemTypeEnum itemType = ItemTypeEnum.GENERIC;

    /**
     * 动画类型（默认ENHANCED）
     */
    public WeaponAnimationType animationType = WeaponAnimationType.ENHANCED;

    /**
     * 移动速度修正
     */
    public float moveSpeedModifier = 1.0F;

    /**
     * 是否可以使用
     */
    public boolean usable = false;

    /**
     * 使用冷却时间（tick）
     */
    public int useCooldown = 0;
    
    /**
     * Consumable 类型专用配置
     */
    public ConsumableConfig consumableConfig = null;
    
    public static class ConsumableConfig {
        /**
         * 使用后执行的指令列表（支持 %player%, @p, @s 等占位符）
         * 可以配置多个指令，按顺序执行
         * 
         * 指令执行者标签（可选，写在指令最前面）：
         * - [console] - 以控制台身份执行（默认，不受权限限制）
         * - [op] - 以管理员权限执行（临时授予OP）
         * - [player] - 以玩家身份执行（受玩家当前权限限制）
         * 
         * JSON 配置示例：
         * "consumableConfig": {
         *   "useCommand": [
         *     "[player]playsound minecraft:entity.player.levelup player %player%",
         *     "[console]effect %player% minecraft:regeneration 10 1",
         *     "[op]gamemode creative %player%",
         *     "tellraw %player% {\"text\":\"使用了道具！\",\"color\":\"green\"}",
         *     "give %player% minecraft:diamond 1"
         *   ],
         *   "consumeOnUse": true
         * }
         * 
         * 注意：不带标签的指令默认以控制台身份执行
         */
        public ArrayList<String> useCommand = new ArrayList<>();
        
        /**
         * 使用后是否消耗道具
         */
        public boolean consumeOnUse = true;
    }

    public CustomItemType() {
        // maxStackSize 继承自 BaseType，默认值为 null，在 loadExtraValues 中设置
    }

    @Override
    public void loadExtraValues() {
        // 设置默认堆叠数量（如果未在JSON中指定）
        if (maxStackSize == null) {
            maxStackSize = 1;
        }

        loadBaseValues();
        loadWeaponSoundMap();
    }

    @Override
    public void reloadModel() {
        if (animationType == WeaponAnimationType.ENHANCED) {
            CustomItemEnhancedRenderConfig config =
                ModularWarfare.getRenderConfig(this, CustomItemEnhancedRenderConfig.class);
            if (config != null) {
                enhancedModel = new ModelEnhancedCustomItem(config, this);
            }
        }
    }

    @Override
    public String getAssetDir() {
        return "customitems";
    }
}

