package com.modularwarfare.api;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.network.PacketWeaponVisual;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.network.PacketAimingResponse;

/** Server API for transient enhanced-gun visuals; no client class is loaded here. */
public final class WeaponVisualAPI {
    private WeaponVisualAPI() {}

    /** Force/clear MWF's third-person aiming pose for a player-owned skill. */
    public static boolean setThirdAim(EntityLivingBase holder, boolean aiming) {
        if (!(holder instanceof EntityPlayerMP) || holder.world.isRemote || holder.getServer() == null
                || !holder.getServer().isCallingFromMinecraftThread()) return false;
        EntityPlayerMP player = (EntityPlayerMP) holder;
        if (aiming) ServerTickHandler.playerAimInstant.put(player.getUniqueID(), true);
        else ServerTickHandler.playerAimInstant.remove(player.getUniqueID());
        ModularWarfare.NETWORK.sendToAll(new PacketAimingResponse(player.getUniqueID(), aiming));
        return true;
    }

    /** Keeps the server-side third-person pose alive and refreshes the client pose each tick. */
    public static boolean maintainThirdAim(EntityLivingBase holder) {
        if (!(holder instanceof EntityPlayerMP) || holder.world.isRemote || holder.getServer() == null
                || !holder.getServer().isCallingFromMinecraftThread()) return false;
        ServerTickHandler.playerAimInstant.put(holder.getUniqueID(), true);
        // The normal MWF aiming synchronizer can publish false on the same tick. Refreshing
        // the skill-owned pose makes the raised-hand state survive the whole cast/recovery.
        ModularWarfare.NETWORK.sendToAll(new PacketAimingResponse(holder.getUniqueID(), true));
        return true;
    }

    public static boolean playAnimation(EntityLivingBase holder, String animation, float speed,
                                         boolean allowReload, boolean allowFire) {
        if (animation == null || animation.isEmpty()) return false;
        return send(holder, PacketWeaponVisual.PLAY, animation, 0, 0, speed, allowReload, allowFire, 0);
    }

    /** start/end are model frames, converted using the render config FPS. */
    public static boolean playAnimation(EntityLivingBase holder, double startFrame, double endFrame, float speed,
                                         boolean allowReload, boolean allowFire) {
        if (!Double.isFinite(startFrame) || !Double.isFinite(endFrame) || startFrame < 0 || endFrame <= startFrame) return false;
        return send(holder, PacketWeaponVisual.PLAY, "", startFrame, endFrame, speed, allowReload, allowFire, 0);
    }

    public static boolean stopAnimation(EntityLivingBase holder) {
        return send(holder, PacketWeaponVisual.STOP, "", 0, 0, 1, false, false, 0);
    }

    /** Play a frame range, optionally holding its end or looping another frame range until stopped. */
    public static boolean playFrames(EntityLivingBase holder, double start, double end, float fps, float speed,
            boolean hold, double loopStart, double loopEnd, boolean reload, boolean fire) {
        if (holder == null || holder.world.isRemote || holder.getServer() == null
                || !holder.getServer().isCallingFromMinecraftThread()
                || !Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start
                || !Float.isFinite(fps) || fps <= 0 || fps > 240 || !Float.isFinite(speed) || speed <= 0
                || !Double.isFinite(loopStart) || !Double.isFinite(loopEnd)
                || (loopStart >= 0 && (loopEnd <= loopStart || !hold))) return false;
        ItemStack stack = holder.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemGun)) return false;
        ItemGun gun = (ItemGun)stack.getItem();
        if (gun.type.animationType != WeaponAnimationType.ENHANCED) return false;
        PacketWeaponVisual packet = new PacketWeaponVisual(holder.getEntityId(), holder.getUniqueID(),
                gun.type.internalName, PacketWeaponVisual.FRAMES, "", start, end, speed, reload, fire, 0);
        packet.fps = fps; packet.hold = hold; packet.loopStart = loopStart; packet.loopEnd = loopEnd;
        ModularWarfare.NETWORK.sendToAllAround(packet, holder.posX, holder.posY, holder.posZ, 256, holder.dimension);
        return true;
    }

    /** Explicit visibility wins over content-pack hide/show lists for the main gun mesh.
     * TTL is mandatory (1..72000 ticks); restore sooner on skill cancellation with resetParts. */
    public static boolean setPartVisible(EntityLivingBase holder, String part, boolean visible, int durationTicks) {
        if (part == null || part.isEmpty() || durationTicks < 1 || durationTicks > 72000) return false;
        return send(holder, visible ? PacketWeaponVisual.SHOW : PacketWeaponVisual.HIDE, part,
                0, 0, 1, false, false, durationTicks);
    }

    public static boolean resetParts(EntityLivingBase holder) {
        return send(holder, PacketWeaponVisual.RESET_PARTS, "", 0, 0, 1, false, false, 0);
    }

    private static boolean send(EntityLivingBase holder, int operation, String name, double start, double end,
                                float speed, boolean reload, boolean fire, int duration) {
        if (holder == null || holder.world.isRemote || holder.getServer() == null
                || !holder.getServer().isCallingFromMinecraftThread() || !Float.isFinite(speed) || speed <= 0
                || name.length() > 128) return false;
        ItemStack stack = holder.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGun)) return false;
        ItemGun gun = (ItemGun) stack.getItem();
        if (gun.type.animationType != WeaponAnimationType.ENHANCED) return false;
        ModularWarfare.NETWORK.sendToAllAround(new PacketWeaponVisual(holder.getEntityId(), holder.getUniqueID(),
                gun.type.internalName, operation, name, start, end, speed, reload, fire, duration),
                holder.posX, holder.posY, holder.posZ, 256, holder.dimension);
        return true;
    }
}
