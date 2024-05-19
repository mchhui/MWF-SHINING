package com.modularwarfare.client.view;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

    public ItemStack heldItemstStack=ItemStack.EMPTY;

    private static boolean aimlock = false;
    private static boolean aimFlag = false;
    private static long lastAimTime;

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        Item item = heldItemstStack.getItem();
        if(item instanceof ItemGun) {
            if (Minecraft.getMinecraft().player != null && ModConfig.INSTANCE.hud.autoSwitchToFirstView) {
                boolean isMouseDown = Mouse.isButtonDown(1);
    
                long time = System.currentTimeMillis();
                if (isMouseDown) {
                    if (!aimFlag) {
                        aimFlag = true;
                        lastAimTime = time;
                    }
                } else {
                    if (aimFlag) {
                        if (time - lastAimTime < 200) {
                            if (!aimlock && Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
                                aimlock = true;
                            } else if (aimlock) {
                                if (ClientProxy.shoulderSurfingLoaded) {
                                    Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
                                     ShoulderInstance.getInstance().setShoulderSurfing(true);
                                } else {
                                    Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
                                }
                                Minecraft.getMinecraft().renderGlobal.setDisplayListEntitiesDirty();
                                aimlock = false;
                            }
                        }
                    }
                    aimFlag = false;
                }
                if (aimlock) {
                    Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
                }
            }
        }
    }

    public static boolean getAutoAimLock() {
        return aimlock;
    }

}