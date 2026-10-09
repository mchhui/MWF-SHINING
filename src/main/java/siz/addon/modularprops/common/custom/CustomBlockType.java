package siz.addon.modularprops.common.custom;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.type.BaseType;
import net.minecraft.block.material.Material;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomBlockEnhancedRenderConfig;
import siz.addon.modularprops.client.fpp.enhanced.models.ModelEnhancedCustomBlock;

public class CustomBlockType extends BaseType {

    /**
     * TileEntity类（如果需要自定义）
     */
    public Class<? extends TileEntityCustomBlock> tileEntityClass = null;

    /**
     * 动画类型（默认ENHANCED）
     */
    public WeaponAnimationType animationType = WeaponAnimationType.ENHANCED;

    /**
     * 方块属性
     */
    public BlockProperties blockProperties = new BlockProperties();

    public static class BlockProperties {
        /**
         * 硬度
         */
        public float hardness = 1.0F;

        /**
         * 抗性
         */
        public float resistance = 5.0F;

        /**
         * 材料（字符串形式，如 "ROCK", "IRON", "WOOD" 等）
         */
        public String materialName = "ROCK";
        
        /**
         * 材料（从 materialName 转换）
         */
        public transient Material material = Material.ROCK;

        /**
         * 光照等级
         */
        public int lightLevel = 0;
        
        /**
         * 是否可通过（生物可以通过）
         */
        public boolean isPassable = false;
        
        /**
         * 初始化 Material 从 materialName
         */
        public void initMaterial() {
            try {
                material = (Material) Material.class.getField(materialName).get(null);
            } catch (Exception e) {
                material = Material.ROCK; // 默认值
            }
        }
    }

    @Override
    public void loadExtraValues() {
        // 初始化 Material
        if (blockProperties != null) {
            blockProperties.initMaterial();
        }
        
        loadBaseValues();
        loadWeaponSoundMap();
    }

    @Override
    public void reloadModel() {
        if (animationType == WeaponAnimationType.ENHANCED) {
            CustomBlockEnhancedRenderConfig config =
                ModularWarfare.getRenderConfig(this, CustomBlockEnhancedRenderConfig.class);
            if (config != null) {
                enhancedModel = new ModelEnhancedCustomBlock(config, this);
            }
        }
    }

    @Override
    public String getAssetDir() {
        return "customblocks";
    }
}

