package com.modularwarfare.client.view;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Mouse;

import com.modularwarfare.ModConfig;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;

import com.teamderpy.shouldersurfing.client.ShoulderInstance;

public class AutoSwitchToFirstView {

    private int initialThirdPersonView = -1;

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (Minecraft.getMinecraft().player != null && ModConfig.INSTANCE.hud.autoSwitchToFirstView) {
            boolean isMouseDown = Mouse.isButtonDown(1);
    
            if (initialThirdPersonView == -1 && Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
                initialThirdPersonView = 1;
            }
    
            if (initialThirdPersonView == 1) {
                if (isMouseDown && (Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() instanceof ItemGun)) {
                    Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
                } else if (!isMouseDown && !ClientRenderHooks.isAimingScope) {
                    if (ClientProxy.shoulderSurfingLoaded) {
                        Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
                        ShoulderInstance.getInstance().setShoulderSurfing(true);
                    } else {
                        Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
                    }
                    initialThirdPersonView = -1;
                }
            } else {
                if (isMouseDown) {
                }
            }
        }
    }

}