package com.modularwarfare.utility;

import net.minecraft.client.Minecraft;
import net.optifine.shaders.MWFOptifineShadesHelper;
import net.optifine.shaders.Program;
import net.optifine.shaders.Shaders;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Author: MrCrayfish
 */
public class OptifineHelper {
    private static Boolean loaded = null;
    private static Field shaderName;
    private static Field gbuffersFormat;

    public static boolean isLoaded() {
        if (loaded == null) {
            try {
                Class.forName("optifine.Installer");
                loaded = true;
            } catch (ClassNotFoundException e) {
                loaded = false;
            }
        }
        return loaded;
    }

    public static boolean isRenderingDfb() {
        if (isShadersEnabled()) {
            return Shaders.isRenderingDfb;
        }
        return false;
    }

    public static void checkBufferFlip(Program program) {
        if (isLoaded()) {
            if (isShadersEnabled()) {
                Class<?> clazz;
                try {
                    clazz = Class.forName("net.optifine.shaders.Shaders");
                    Method m = clazz.getDeclaredMethod("checkBufferFlip", Program.class);
                    m.setAccessible(true);
                    m.invoke(null, program);
                } catch (ClassNotFoundException | SecurityException | IllegalArgumentException | NoSuchMethodException |
                         IllegalAccessException | InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static void bindGbuffersTextures() {
        if (isLoaded()) {
            if (isShadersEnabled()) {
                Class<?> clazz;
                try {
                    clazz = Class.forName("net.optifine.shaders.Shaders");
                    Method m = clazz.getDeclaredMethod("bindGbuffersTextures");
                    m.setAccessible(true);
                    m.invoke(null);
                } catch (ClassNotFoundException | SecurityException | IllegalArgumentException | NoSuchMethodException |
                         IllegalAccessException | InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static int getDrawFrameBuffer() {
        if (!isLoaded()) {
            return Minecraft.getMinecraft().getFramebuffer().framebufferObject;
        }

        if (!isShadersEnabled()) {
            return Minecraft.getMinecraft().getFramebuffer().framebufferObject;
        }

        if (Shaders.isRenderingDfb) {
            return MWFOptifineShadesHelper.getDFB();
        }
        return Minecraft.getMinecraft().getFramebuffer().framebufferObject;
    }

    public static int getPixelFormat(int internalFormat) {
        switch (internalFormat) {
            case 33333:
            case 33334:
            case 33339:
            case 33340:
            case 36208:
            case 36209:
            case 36226:
            case 36227:
                return 36251;
        }
        return 32993;
    }

    public static int[] getGbuffersFormat() {
        if (isLoaded()) {
            try {
                Class<?> clazz = Class.forName("net.optifine.shaders.Shaders");
                if (clazz != null && gbuffersFormat == null) {
                    gbuffersFormat = clazz.getDeclaredField("gbuffersFormat");
                    gbuffersFormat.setAccessible(true);
                }
                if (gbuffersFormat != null) {
                    return (int[]) gbuffersFormat.get(null);
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return null;
    }

    public static boolean isShadersEnabled() {
        if (isLoaded()) {
            try {
                Class<?> clazz = Class.forName("net.optifine.shaders.Shaders");
                if (clazz != null && shaderName == null) {
                    shaderName = clazz.getDeclaredField("shaderPackLoaded");
                }
                if (shaderName != null) {
                    return (boolean) (Boolean) shaderName.get(null);
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static int getProgram() {
        return Shaders.activeProgramID;
    }
}