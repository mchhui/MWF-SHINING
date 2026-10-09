package siz.addon.modularprops.client.fpp.enhanced.models;

import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig;

public class ModelEnhancedCustomItem extends EnhancedModel {

    public ModelEnhancedCustomItem(CustomItemEnhancedRenderConfig config, com.modularwarfare.common.type.BaseType baseType) {
        super(config, baseType);
    }

    public ModelEnhancedCustomItem() {
        super(null, null);
    }
}

