package siz.addon.modularprops.client;

import com.modularwarfare.api.AddRenderLayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import siz.addon.modularprops.client.model.layers.RenderLayerHeldCustomItem;

/**
 * 处理渲染层注册的事件处理器
 * 在 FakeRenderPlayer 初始化时注册 RenderLayerHeldCustomItem
 */
@SideOnly(Side.CLIENT)
public class ModularPropsLayerHandler {
    
    @SubscribeEvent
    public void onAddRenderLayer(AddRenderLayerEvent event) {
        // 注册自定义物品的渲染层
        event.addLayer(new RenderLayerHeldCustomItem(event.renderPlayer));
    }
}

