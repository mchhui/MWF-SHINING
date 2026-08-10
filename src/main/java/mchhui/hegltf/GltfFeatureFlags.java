package mchhui.hegltf;

import com.modularwarfare.ModConfig;

public final class GltfFeatureFlags {

    private GltfFeatureFlags() {}

    public static boolean skinAnimOpt() {
        return ModConfig.INSTANCE != null && ModConfig.INSTANCE.gltf != null
            && ModConfig.INSTANCE.gltf.skinAnimOpt;
    }

    public static boolean renderSchedulingOpt() {
        return ModConfig.INSTANCE != null && ModConfig.INSTANCE.gltf != null
            && ModConfig.INSTANCE.gltf.renderSchedulingOpt;
    }
}
