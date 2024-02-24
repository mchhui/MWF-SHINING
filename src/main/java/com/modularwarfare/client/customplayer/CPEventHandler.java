package com.modularwarfare.client.customplayer;

import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CPEventHandler {
    public static HashMap<String, CustomPlayerConfig> cpConfig = new HashMap<String, CustomPlayerConfig>();

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if(1==1) {
            return;
        }
        if (cpConfig.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        CustomPlayer customPlayer = CustomPlayer.getCustomPlayer(event.getEntityPlayer().getName());
        customPlayer.bind(cpConfig.get("siz.character_1"));
        GlStateManager.pushMatrix();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.rotate(event.getEntityPlayer().prevRenderYawOffset
            + (event.getEntityPlayer().renderYawOffset - event.getEntityPlayer().prevRenderYawOffset)
                * event.getPartialRenderTick(),
            0, -1, 0);
        GlStateManager.translate(event.getX(), event.getY(), event.getZ());
        customPlayer.render(event.getEntityPlayer(), event.getPartialRenderTick());
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }
}
