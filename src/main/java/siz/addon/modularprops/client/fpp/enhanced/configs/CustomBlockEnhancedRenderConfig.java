package siz.addon.modularprops.client.fpp.enhanced.configs;

import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType;

import java.util.HashMap;

public class CustomBlockEnhancedRenderConfig extends EnhancedRenderConfig {

    /**
     * 自定义方块动画映射（使用专门的动画类型枚举）
     */
    public HashMap<AnimationCustomBlockType, EnhancedRenderConfig.Animation> customAnimations = new HashMap<>();

    /**
     * 方块朝向修正配置
     */
    public BlockFacingConfig blockFacing = new BlockFacingConfig();

    /**
     * 方块朝向修正配置
     */
    public static class BlockFacingConfig {
        /**
         * 是否根据方块朝向自动旋转
         */
        public boolean autoRotateByFacing = true;

        /**
         * 朝向旋转偏移量（在自动旋转基础上的额外旋转）
         */
        public float facingRotationOffset = 0;
    }
}

