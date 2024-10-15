package com.modularwarfare.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class KeyBindingUtil {
    private static boolean active = true;

    static {
        final KeyBinding binding = Minecraft.getMinecraft().gameSettings.keyBindSprint;
        final IKeyConflictContext ctx = binding.getKeyConflictContext();
        binding.setKeyConflictContext(new IKeyConflictContext() {
            @Override
            public boolean isActive() {
                return active && ctx.isActive();
            }

            @Override
            public boolean conflicts(IKeyConflictContext other) {
                return ctx.conflicts(other);
            }
        });
    }

    public static void disableSprinting(boolean flag) {
        active = !flag;
    }

    private KeyBindingUtil() { }
}
