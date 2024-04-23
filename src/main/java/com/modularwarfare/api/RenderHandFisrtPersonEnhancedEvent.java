package com.modularwarfare.api;

import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.basic.renderers.RenderGunStatic;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;

import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class RenderHandFisrtPersonEnhancedEvent extends Event {
    public CustomItemRenderer renderer;
    public EnumHandSide side;

    @Cancelable
    public static class PreFirstLayer extends RenderHandFisrtPersonEnhancedEvent {
        public PreFirstLayer(CustomItemRenderer renderer, EnumHandSide handSide) {
            this.renderer = renderer;
            this.side = handSide;
        }
    }

    @Cancelable
    public static class PreSecondLayer extends RenderHandFisrtPersonEnhancedEvent {
        public PreSecondLayer(CustomItemRenderer renderer, EnumHandSide handSide) {
            this.renderer = renderer;
            this.side = handSide;
        }
    }

}
