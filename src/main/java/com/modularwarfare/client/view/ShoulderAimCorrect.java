package com.modularwarfare.client.view;

import javax.annotation.Nullable;

import com.modularwarfare.ModConfig;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.utility.RayUtil;
import com.teamderpy.shouldersurfing.client.ShoulderInstance;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ShoulderAimCorrect {

    private static final double RAY_RANGE = 128.0D;
    private static final float DEFAULT_BODY_CORRECT_DEG = 20f;
    private static final float DEFAULT_RIDE_UPPER_MAX_DEG = 120f;

    private ShoulderAimCorrect() {}

    public static final class AimLook {
        public final float lookYaw;
        public final float lookPitch;
        public final float bodyYaw;
        public final boolean bodyFollowsLook;
        public final float upperBodyTwistDeg;

        public AimLook(float lookYaw, float lookPitch, float bodyYaw, boolean bodyFollowsLook) {
            this(lookYaw, lookPitch, bodyYaw, bodyFollowsLook, 0f);
        }

        public AimLook(float lookYaw, float lookPitch, float bodyYaw, boolean bodyFollowsLook,
                float upperBodyTwistDeg) {
            this.lookYaw = lookYaw;
            this.lookPitch = lookPitch;
            this.bodyYaw = bodyYaw;
            this.bodyFollowsLook = bodyFollowsLook;
            this.upperBodyTwistDeg = upperBodyTwistDeg;
        }
    }

    public static AimLook resolve(EntityPlayer player, float partialTicks) {
        float camYaw;
        float camPitch;
        if (player == Minecraft.getMinecraft().player) {
            camYaw = lerp(player.prevRotationYaw, player.rotationYaw, partialTicks);
            camPitch = lerp(player.prevRotationPitch, player.rotationPitch, partialTicks);
        } else {
            AimPoseClientStore.Entry synced = AimPoseClientStore.get(player.getUniqueID());
            if (synced != null) {
                float lookYaw = lerp(synced.prevLookYaw, synced.lookYaw, partialTicks);
                float lookPitch = lerp(synced.prevLookPitch, synced.lookPitch, partialTicks);
                float bodyYaw = lerp(synced.prevBodyYaw, synced.bodyYaw, partialTicks);
                return applyRideClamp(player, partialTicks, lookYaw, lookPitch, bodyYaw, true);
            }
            camYaw = lerp(player.prevRotationYawHead, player.rotationYawHead, partialTicks);
            camPitch = lerp(player.prevRotationPitch, player.rotationPitch, partialTicks);
            return applyRideClamp(player, partialTicks, camYaw, camPitch, camYaw, true);
        }

        if (!isShoulderSurfingActive()) {
            return applyRideClamp(player, partialTicks, camYaw, camPitch, camYaw, true);
        }

        Vec3d hit = crosshairHit(partialTicks);
        if (hit == null) {
            return applyRideClamp(player, partialTicks, camYaw, camPitch, camYaw, true);
        }
        Vec3d eye = player.getPositionEyes(partialTicks);
        double dx = hit.x - eye.x;
        double dy = hit.y - eye.y;
        double dz = hit.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz < 1.0E-6D && Math.abs(dy) < 1.0E-6D) {
            return applyRideClamp(player, partialTicks, camYaw, camPitch, camYaw, true);
        }

        float lookPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(horiz, 1.0E-6D)));
        float lookYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        float yawDelta = Math.abs(MathHelper.wrapDegrees(lookYaw - camYaw));
        float pitchDelta = Math.abs(lookPitch - camPitch);
        float correctDeg = bodyCorrectDegrees();
        boolean bodyFollows = yawDelta >= correctDeg || pitchDelta >= correctDeg;
        float bodyYaw = bodyFollows ? lookYaw : camYaw;
        return applyRideClamp(player, partialTicks, lookYaw, lookPitch, bodyYaw, bodyFollows);
    }

    private static AimLook applyRideClamp(EntityPlayer player, float partialTicks,
            float lookYaw, float lookPitch, float bodyYaw, boolean bodyFollows) {
        Entity mount = player.getRidingEntity();
        if (mount == null) {
            return new AimLook(lookYaw, lookPitch, bodyYaw, bodyFollows, 0f);
        }
        float mountYaw = lerp(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
        float maxTwist = rideUpperBodyMaxDegrees();
        float twist = MathHelper.clamp(MathHelper.wrapDegrees(lookYaw - mountYaw), -maxTwist, maxTwist);
        float clampedLookYaw = mountYaw + twist;
        return new AimLook(clampedLookYaw, lookPitch, mountYaw, false, twist);
    }

    public static boolean isShoulderSurfingActive() {
        if (!ClientProxy.shoulderSurfingLoaded) {
            return false;
        }
        try {
            return ShoulderInstance.getInstance().doShoulderSurfing();
        } catch (Throwable t) {
            return false;
        }
    }

    private static float bodyCorrectDegrees() {
        if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.client != null
            && ModConfig.INSTANCE.client.aimShoulderBodyCorrectDegrees != null) {
            return ModConfig.INSTANCE.client.aimShoulderBodyCorrectDegrees.floatValue();
        }
        return DEFAULT_BODY_CORRECT_DEG;
    }

    private static float rideUpperBodyMaxDegrees() {
        if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.client != null
            && ModConfig.INSTANCE.client.aimRideUpperBodyMaxDegrees != null) {
            return ModConfig.INSTANCE.client.aimRideUpperBodyMaxDegrees.floatValue();
        }
        return DEFAULT_RIDE_UPPER_MAX_DEG;
    }

    private static float lerp(float prev, float next, float pt) {
        return prev + (next - prev) * pt;
    }

    @Nullable
    private static Vec3d crosshairHit(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity entity = mc.getRenderViewEntity();
        if (entity == null || mc.world == null) {
            return null;
        }
        RayTraceResult pick = RayUtil.shoulderCrosshairPick(entity, partialTicks, RAY_RANGE);
        if (pick != null && pick.hitVec != null) {
            return pick.hitVec;
        }
        Vec3d look = entity.getLook(partialTicks);
        Vec3d eye = entity.getPositionEyes(partialTicks);
        return eye.add(look.x * RAY_RANGE, look.y * RAY_RANGE, look.z * RAY_RANGE);
    }
}
