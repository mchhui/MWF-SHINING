package com.modularwarfare.client.flashlight;

import com.modularwarfare.common.guns.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.UUID;

@SideOnly(Side.CLIENT)
public class FlashlightRenderManager {
    private static final FlashlightRenderManager INSTANCE = new FlashlightRenderManager();
    private final HashMap<UUID, Boolean> flashlightStates = new HashMap<>();

    public static FlashlightRenderManager getInstance() {
        return INSTANCE;
    }

    public void setFlashlightState(UUID playerId, boolean enabled) {
        flashlightStates.put(playerId, enabled);
    }

    public boolean getFlashlightState(UUID playerId) {
        return flashlightStates.getOrDefault(playerId, false);
    }

    public void toggleFlashlightState(UUID playerId) {
        setFlashlightState(playerId, !getFlashlightState(playerId));
    }

    public void updateFlashlightState(UUID playerId, ItemStack gunStack) {
        if (gunStack.getItem() instanceof ItemGun) {
            boolean enabled = ((ItemGun) gunStack.getItem()).getFlashlightEnabled(gunStack);
            setFlashlightState(playerId, enabled);
        }
    }

    /** 本地玩家以物品 NBT 为准；他人以同步 map 为准。 */
    public boolean isFlashlightOnFor(UUID playerId) {
        EntityPlayerSP local = Minecraft.getMinecraft().player;
        if (local != null && local.getUniqueID().equals(playerId)) {
            ItemStack held = local.getHeldItemMainhand();
            if (held.getItem() instanceof ItemGun) {
                return ((ItemGun) held.getItem()).getFlashlightEnabled(held);
            }
            return false;
        }
        return getFlashlightState(playerId);
    }
}
