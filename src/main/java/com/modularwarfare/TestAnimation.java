package com.modularwarfare;

import java.io.IOException;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.modularwarfare.loader.ObjModelAnimation;
import com.modularwarfare.loader.ObjModelBuilder;
import com.modularwarfare.loader.api.ObjModelLoader;
import com.modularwarfare.loader.blenderani.BlenderAnimation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber
public class TestAnimation {
    public static ObjModelAnimation animation;
    public static float x;
    public static float y=3;
    public static float z;
    public static float s=0.2f;

    @SubscribeEvent
    public static void onRenderHand(RenderPlayerEvent.Post event) {
        if (animation == null) {
            try {
                InputStreamReader reader = new InputStreamReader(Minecraft.getMinecraft().getResourceManager()
                        .getResource(new ResourceLocation("modularwarfare:test_animation.json")).getInputStream());
                animation = new ObjModelAnimation(
                        ObjModelLoader.load(new ResourceLocation("modularwarfare:gun_animation.obj")), new Gson().fromJson(reader, BlenderAnimation.class), 24);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("modularwarfare:siz_bg_m416.png"));
            GlStateManager.pushMatrix();
            GlStateManager.color(1,1,1,1);
            GlStateManager.translate(x,y,z);
            if(animation.isFinish()) {
                animation.setStartTime(System.currentTimeMillis());
            }
            animation.updateFrame(System.currentTimeMillis());
            animation.renderAll(s);
            GlStateManager.popMatrix();
        }
    }
}
