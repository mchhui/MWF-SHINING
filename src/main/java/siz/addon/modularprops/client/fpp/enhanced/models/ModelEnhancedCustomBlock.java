package siz.addon.modularprops.client.fpp.enhanced.models;

import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomBlockEnhancedRenderConfig;

public class ModelEnhancedCustomBlock extends EnhancedModel {

    public ModelEnhancedCustomBlock(CustomBlockEnhancedRenderConfig config, com.modularwarfare.common.type.BaseType baseType) {
        super(config, baseType);
    }

    public ModelEnhancedCustomBlock() {
        super(null, null);
    }
}

