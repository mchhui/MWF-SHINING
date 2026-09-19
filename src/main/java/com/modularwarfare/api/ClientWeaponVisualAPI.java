package com.modularwarfare.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client main-thread API for local prediction; does not cast a skill or authorize firing. */
@SideOnly(Side.CLIENT)
public final class ClientWeaponVisualAPI {
    private static volatile boolean skillAimBlocked;
    private static boolean waitForAimRelease;
    private static final Map<UUID, Parts> parts = new HashMap<>();
    static { MinecraftForge.EVENT_BUS.register(ClientWeaponVisualAPI.class); }
    private ClientWeaponVisualAPI() {}

    /** Client-side gate used by skill mods while a skill owns the aim button. */
    public static void setSkillAimBlocked(boolean blocked) {
        if (skillAimBlocked && !blocked && org.lwjgl.input.Mouse.isButtonDown(1)) waitForAimRelease = true;
        skillAimBlocked = blocked;
        if (blocked) {
            com.modularwarfare.client.fpp.basic.renderers.RenderParameters.adsSwitch = 0;
            com.modularwarfare.client.ClientRenderHooks.isAiming = false;
            com.modularwarfare.client.ClientRenderHooks.isAimingScope = false;
        }
    }
    public static boolean isSkillAimBlocked() {
        if (waitForAimRelease && !org.lwjgl.input.Mouse.isButtonDown(1)) waitForAimRelease = false;
        return skillAimBlocked || waitForAimRelease;
    }

    private static final class Visibility {
        final boolean visible;
        final long expires;
        Visibility(boolean visible, long expires) { this.visible = visible; this.expires = expires; }
    }
    private static final class Parts {
        final EntityLivingBase holder;
        final ItemStack stack;
        final Map<String, Visibility> values = new HashMap<>();
        Parts(EntityLivingBase holder) { this.holder = holder; this.stack = holder.getHeldItemMainhand(); }
    }
    private static ItemGun gun(EntityLivingBase holder) {
        if (holder == null || !holder.world.isRemote || !Minecraft.getMinecraft().isCallingFromMinecraftThread()) return null;
        ItemStack stack = holder.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGun)) return null;
        ItemGun gun = (ItemGun) stack.getItem();
        return gun.type.animationType == WeaponAnimationType.ENHANCED && gun.type.enhancedModel != null ? gun : null;
    }
    public static boolean playAnimation(EntityLivingBase holder, String name, double start, double end,
                                         float speed, boolean allowReload, boolean allowFire) {
        ItemGun gun = gun(holder);
        if (gun == null || name == null || name.length() > 128 || !Float.isFinite(speed) || speed <= 0) return false;
        if (name.isEmpty()) {
            if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start) return false;
        } else {
            try {
                AnimationType type = AnimationType.AnimationTypeJsonAdapter.fromString(name);
                if (!gun.type.enhancedModel.config.animations.containsKey(type)) return false;
            } catch (RuntimeException invalid) { return false; }
        }
        AnimationController controller = AnimationController.getController(holder, gun.type.enhancedModel.config);
        controller.updateCurrentItem();
        controller.CUSTOM = 0; controller.customAnimation = name;
        controller.customAnimationHold = false; controller.customLoopStart = controller.customLoopEnd = -1;
        controller.startTime = start; controller.endTime = end;
        // Frame API speed=1 means real-time playback. The legacy controller advances in 60 Hz steps.
        controller.customAnimationSpeed = name.isEmpty() ? speed / 60.0 : speed;
        controller.customAnimationReload = allowReload; controller.customAnimationFire = allowFire;
        controller.updateActionAndTime();
        return true;
    }
    public static boolean stopAnimation(EntityLivingBase holder) {
        ItemGun gun = gun(holder);
        if (gun == null) return false;
        AnimationController controller = AnimationController.getController(holder, gun.type.enhancedModel.config);
        controller.CUSTOM = 1; controller.customAnimation = "";
        controller.customAnimationHold = false;
        controller.customAnimationReload = false; controller.customAnimationFire = false;
        controller.updateActionAndTime();
        return true;
    }
    /** Explicit model FPS keeps the server timeline and client playback on the same clock. */
    public static boolean playFrames(EntityLivingBase holder, double start, double end, float fps, float speed,
            boolean hold, double loopStart, double loopEnd, boolean reload, boolean fire) {
        if (!Float.isFinite(fps) || fps <= 0 || fps > 240 || !Double.isFinite(loopStart)
                || !Double.isFinite(loopEnd) || (loopStart >= 0 && (loopEnd <= loopStart || !hold))) return false;
        if (!playAnimation(holder, "", start, end, speed, reload, fire)) return false;
        ItemGun gun = gun(holder);
        AnimationController c = AnimationController.getController(holder, gun.type.enhancedModel.config);
        c.customAnimationSpeed *= fps / gun.type.enhancedModel.config.FPS;
        c.customAnimationHold = hold; c.customLoopStart = loopStart; c.customLoopEnd = loopEnd;
        return true;
    }
    public static boolean setPartVisible(EntityLivingBase holder, String part, boolean visible, int durationTicks) {
        if (gun(holder) == null || part == null || part.isEmpty() || part.length() > 128 || durationTicks < 1 || durationTicks > 72000) return false;
        Parts state = parts.get(holder.getUniqueID());
        if (state == null || state.holder != holder || state.stack != holder.getHeldItemMainhand()) {
            state = new Parts(holder); parts.put(holder.getUniqueID(), state);
        }
        state.values.put(part, new Visibility(visible, holder.world.getTotalWorldTime() + durationTicks));
        return true;
    }
    public static void resetParts(EntityLivingBase holder) {
        if (holder != null) parts.remove(holder.getUniqueID());
    }
    /** Always copies: per-holder overrides must never contaminate the shared third-person render cache. */
    public static HashSet<String> applyPartVisibility(EntityLivingBase holder, ItemStack stack, HashSet<String> defaults) {
        HashSet<String> result = new HashSet<>(defaults);
        if (holder == null) return result;
        Parts state = parts.get(holder.getUniqueID());
        if (state == null || state.holder != holder || state.stack != stack) return result;
        long now = holder.world.getTotalWorldTime();
        state.values.forEach((part, visibility) -> {
            if (visibility.expires > now) {
                if (visibility.visible) result.remove(part); else result.add(part);
            }
        });
        return result;
    }
    @SubscribeEvent public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        parts.values().removeIf(state -> {
            if (mc.world == null || state.holder.world != mc.world || state.holder.isDead
                    || state.holder.getHeldItemMainhand() != state.stack) return true;
            state.values.values().removeIf(value -> value.expires <= mc.world.getTotalWorldTime());
            return state.values.isEmpty();
        });
    }
}
