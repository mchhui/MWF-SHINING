package com.modularwarfare.client.input;

import java.lang.reflect.Field;

import com.modularwarfare.utility.OptifineHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class OptifineInputUtil {

    private static final int ZOOM_KEYCODE_UNSET = Integer.MIN_VALUE;
    private static int savedZoomKeyCode = ZOOM_KEYCODE_UNSET;

    private static KeyBinding findZoomKeyBinding(GameSettings settings) {
        if (settings == null) {
            return null;
        }
        Class<?> clazz = settings.getClass();
        try {
            Field f = clazz.getField("ofKeyBindZoom");
            return (KeyBinding) f.get(settings);
        } catch (NoSuchFieldException ignored) {
        } catch (Exception ignored) {
        }
        try {
            Field f = clazz.getDeclaredField("ofKeyBindZoom");
            f.setAccessible(true);
            return (KeyBinding) f.get(settings);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void clearOptifineZoomState(Minecraft mc) {
        try {
            Class<?> cfg = Class.forName("net.optifine.Config");
            Field zoomMode = cfg.getField("zoomMode");
            if (zoomMode.getBoolean(null)) {
                zoomMode.setBoolean(null, false);
                try {
                    Field zoomSmooth = cfg.getField("zoomSmoothCamera");
                    zoomSmooth.setBoolean(null, false);
                } catch (Exception ignored) {
                }
                if (mc.gameSettings != null) {
                    mc.gameSettings.smoothCamera = false;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void disableZoom(boolean flag) {
        if (!OptifineHelper.isLoaded()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return;
        }
        KeyBinding binding = findZoomKeyBinding(mc.gameSettings);
        if (binding == null) {
            return;
        }
        if (flag) {
            if (savedZoomKeyCode == ZOOM_KEYCODE_UNSET) {
                savedZoomKeyCode = binding.getKeyCode();
            }
            if (binding.getKeyCode() != 0) {
                binding.setKeyCode(0);
            }
            clearOptifineZoomState(mc);
        } else {
            if (savedZoomKeyCode != ZOOM_KEYCODE_UNSET) {
                binding.setKeyCode(savedZoomKeyCode);
                savedZoomKeyCode = ZOOM_KEYCODE_UNSET;
            }
        }
    }

    private OptifineInputUtil() { }
}
