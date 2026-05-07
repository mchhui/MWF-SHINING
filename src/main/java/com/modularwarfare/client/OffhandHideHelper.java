package com.modularwarfare.client;

import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 根据 {@link EnhancedRenderConfig#hideOffhandItem} 判断是否应在主手持有该道具时隐藏副手渲染。
 */
@SideOnly(Side.CLIENT)
public final class OffhandHideHelper {

    private OffhandHideHelper() {}

    public static boolean shouldHideOffhandForMainhand(ItemStack mainHand) {
        if (mainHand == null || mainHand.isEmpty()) {
            return false;
        }
        if (!(mainHand.getItem() instanceof BaseItem)) {
            return false;
        }
        BaseType type = ((BaseItem) mainHand.getItem()).baseType;
        if (type == null || type.enhancedModel == null || type.enhancedModel.config == null) {
            return false;
        }
        return ((EnhancedRenderConfig) type.enhancedModel.config).hideOffhandItem;
    }
}
