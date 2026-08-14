package com.modularwarfare.client.view;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.modularwarfare.ModConfig;
import com.modularwarfare.client.ClientProxy;
import com.teamderpy.shouldersurfing.client.ShoulderHelper;
import com.teamderpy.shouldersurfing.client.ShoulderInstance;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * ShoulderSurfing visual aim pose (model facing along yellow ray; not fire/trail).
 */
@SideOnly(Side.CLIENT)
public final class ShoulderAimCorrect {

    private static final double RAY_RANGE = 128.0D;
    private static final float DEFAULT_BODY_CORRECT_DEG = 20f;
    /** Default ride upper-body yaw clamp (degrees). */
    private static final float DEFAULT_RIDE_UPPER_MAX_DEG = 120f;

    private ShoulderAimCorrect() {}

    public static final class AimLook {
        /** Head / aim-bone yaw & pitch. */
        public final float lookYaw;
        public final float lookPitch;
        /** {@code renderYawOffset}; ride = seat facing. */
        public final float bodyYaw;
        public final boolean bodyFollowsLook;
        /** Upper-body yaw relative to {@link #bodyYaw} (degrees); 0 when not riding. */
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

    /** Local SS aim look, or camera follow; remotes use head yaw only. */
    public static AimLook resolve(EntityPlayer player, float partialTicks) {
        float camYaw;
        float camPitch;
        if (player == Minecraft.getMinecraft().player) {
            camYaw = lerp(player.prevRotationYaw, player.rotationYaw, partialTicks);
            camPitch = lerp(player.prevRotationPitch, player.rotationPitch, partialTicks);
        } else {
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

    /** Ride: bodyYaw = seat; look/twist clamped to {@link #rideUpperBodyMaxDegrees()}. */
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

    /** Crosshair world hit under ShoulderSurfing. */
    @Nullable
    private static Vec3d crosshairHit(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity entity = mc.getRenderViewEntity();
        if (entity == null || mc.world == null) {
            return null;
        }
        RayTraceResult objectMouseOver = entity.rayTrace(RAY_RANGE, partialTicks);
        Vec3d cameraPos;
        try {
            cameraPos = ShoulderHelper.shoulderSurfingLook(entity, partialTicks, RAY_RANGE).cameraPos();
        } catch (Throwable t) {
            return objectMouseOver != null ? objectMouseOver.hitVec : null;
        }
        double d1 = RAY_RANGE;
        if (objectMouseOver != null) {
            d1 = objectMouseOver.hitVec.distanceTo(cameraPos);
        }
        Vec3d look = entity.getLook(partialTicks);
        Vec3d end = cameraPos.add(look.x * d1, look.y * d1, look.z * d1);
        Entity pointedEntity = null;
        Vec3d entityHit = null;
        List<Entity> list = mc.world.getEntitiesInAABBexcluding(entity,
            entity.getEntityBoundingBox().expand(look.x * d1, look.y * d1, look.z * d1).grow(1.0D, 1.0D, 1.0D),
            Predicates.and(EntitySelectors.NOT_SPECTATING, new Predicate<Entity>() {
                @Override
                public boolean apply(@Nullable Entity e) {
                    return e != null && e.canBeCollidedWith();
                }
            }));
        double d2 = d1;
        for (int j = 0; j < list.size(); j++) {
            Entity entity1 = list.get(j);
            AxisAlignedBB aabb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
            RayTraceResult intercept = aabb.calculateIntercept(cameraPos, end);
            if (aabb.contains(cameraPos)) {
                if (d2 >= 0.0D) {
                    pointedEntity = entity1;
                    entityHit = intercept == null ? cameraPos : intercept.hitVec;
                    d2 = 0.0D;
                }
            } else if (intercept != null) {
                double d3 = cameraPos.distanceTo(intercept.hitVec);
                if (d3 < d2 || d2 == 0.0D) {
                    if (entity1.getLowestRidingEntity() == entity.getLowestRidingEntity()
                        && !entity1.canRiderInteract()) {
                        if (d2 == 0.0D) {
                            pointedEntity = entity1;
                            entityHit = intercept.hitVec;
                        }
                    } else {
                        pointedEntity = entity1;
                        entityHit = intercept.hitVec;
                        d2 = d3;
                    }
                }
            }
        }
        if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
            return entityHit;
        }
        if (objectMouseOver != null) {
            return objectMouseOver.hitVec;
        }
        return end;
    }
}
