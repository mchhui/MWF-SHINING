package com.modularwarfare.api;

import com.modularwarfare.client.model.FakeRenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 用于在 FakeRenderPlayer 中添加渲染层的事件
 * 允许附属mod添加自定义的渲染层
 */
public class AddRenderLayerEvent extends Event {
    
    public FakeRenderPlayer renderPlayer;
    
    public AddRenderLayerEvent(FakeRenderPlayer renderPlayer) {
        this.renderPlayer = renderPlayer;
    }
    
    /**
     * 添加一个渲染层
     * @param layer 要添加的渲染层
     */
    public void addLayer(LayerRenderer<?> layer) {
        if (renderPlayer != null && layer != null) {
            renderPlayer.addLayer(layer);
        }
    }
}

