package com.modularwarfare.api;

import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;

import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.common.eventhandler.Event;

public class RenderHandSleeveEnhancedEvent extends Event {
    public CustomItemRenderer renderer;
    public EnumHandSide side;
    public EnhancedModel model;

    public static class Post extends RenderHandSleeveEnhancedEvent {
        public Post(CustomItemRenderer renderer, EnumHandSide side, EnhancedModel model) {
            this.renderer = renderer;
            this.side = side;
            this.model = model;
        }
    }
}
