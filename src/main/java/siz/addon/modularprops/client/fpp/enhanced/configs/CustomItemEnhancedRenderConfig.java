package siz.addon.modularprops.client.fpp.enhanced.configs;

import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import org.lwjgl.util.vector.Vector3f;
import siz.addon.modularprops.client.fpp.enhanced.AnimationCustomItemType;

import java.util.HashMap;

public class CustomItemEnhancedRenderConfig extends EnhancedRenderConfig {

    /**
     * 自定义道具动画映射（使用专门的动画类型枚举）
     */
    public HashMap<AnimationCustomItemType, EnhancedRenderConfig.Animation> customAnimations = new HashMap<>();

    /**
     * 对象控制
     */
    public HashMap<String, ObjectControl> objectControl = new HashMap<>();

    /**
     * 冲刺配置
     */
    public Sprint sprint = new Sprint();

    /**
     * USE动画执行百分比（0.0-1.0）
     * 当USE动画播放到此百分比时，发包到服务器执行指令
     */
    public float useExecutePercent = 0.8f;

    public static class Sprint {
        public Vector3f sprintRotate = new Vector3f(-20.0F, 30.0F, -0.0F);
        public Vector3f sprintTranslate = new Vector3f(0.5F, -0.10F, -0.65F);
    }

    public static class ObjectControl extends EnhancedRenderConfig.Transform {
        // 可以添加自定义对象控制参数
    }
}

