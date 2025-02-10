package com.modularwarfare.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public final class HudHoldUtil {
    private static boolean active = true;
    private static boolean hideGuiBackup = false;

    static {
        hideGuiBackup = Minecraft.getMinecraft().gameSettings.hideGUI;
    }
    //杀死那个F1
    @SubscribeEvent
    public static void onKeyInput(KeyInputEvent event) {
        if (Keyboard.getEventKey() == Keyboard.KEY_F1 && !active) {
            Minecraft.getMinecraft().gameSettings.hideGUI = hideGuiBackup;
        }
    }

    public static void disableHideGui(boolean flag) {
        active = !flag;
        if (flag) {
            hideGuiBackup = Minecraft.getMinecraft().gameSettings.hideGUI;
            Minecraft.getMinecraft().gameSettings.hideGUI = false;
        }
    }

    private HudHoldUtil() { }
} 