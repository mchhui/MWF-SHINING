package com.modularwarfare.client.trail;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.GunNodeWorld;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.model.InstantBulletRenderer;
import com.modularwarfare.client.model.InstantBulletTeslaRender;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.utility.vector.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 客户端尾迹起点纠正。
 * <p>
 * 流程：服务端 {@code FireManager.drawTrail} 广播 {@code PacketGunTrail}（含 shooterEntityId）→
 * 入队 → {@code RenderWorldLast} flush 第三人称/他人，{@code RenderGunEnhanced} 第一人称 render 结束 flush 本地 1P →
 * spawn 计算起点/终点 → {@code ClientEventHandler} 绘制。
 * <p>
 * 本地开火另走 {@code FireManager.queueClientPredictedTrails} 预测入队，忽略服务端回包重复。
 */
@SideOnly(Side.CLIENT)
public final class TrailOriginResolver {

    private static final float THIRD_PERSON_ORIGIN_FORWARD = 2.0F;

    private static int frameCounter = 0;

    private static final CopyOnWriteArrayList<PendingTesla> PENDING_TESLA = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<PendingGunTrail> PENDING_GUN = new CopyOnWriteArrayList<>();

    private TrailOriginResolver() {}

    public static void queueTeslaTrail(int shooterEntityId, double posX, double posY, double posZ, double targetX,
            double targetY, double targetZ, float bulletSpeed, String gunTypeName) {
        PENDING_TESLA.add(new PendingTesla(shooterEntityId, posX, posY, posZ, targetX, targetY, targetZ, bulletSpeed,
                gunTypeName));
    }

    public static void queueGunTrail(int shooterEntityId, String gunType, String model, String tex, boolean glow,
            double posX, double posY, double posZ, double motionX, double motionZ, double dirX, double dirY,
            double dirZ, double range, float bulletSpeed) {
        PENDING_GUN.add(new PendingGunTrail(shooterEntityId, gunType, model, tex, glow, posX, posY, posZ, motionX,
                motionZ, dirX, dirY, dirZ, range, bulletSpeed));
    }

    public static void flushPendingTrailsAfterWorld(float partialTicks) {
        frameCounter++;
        flushPending(false, true);
    }

    public static void flushPendingTrailsAfterFirstPersonGun(float partialTicks) {
        flushPending(true, false);
    }

    public static void renderTrailsOnly(float partialTicks) {
        InstantBulletRenderer.RenderAllTrails(partialTicks);
        InstantBulletTeslaRender.RenderAllTeslaTrails(partialTicks);
    }

    public static boolean shouldIgnoreServerTrailForLocalShooter(int shooterEntityId) {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.player != null && shooterEntityId == mc.player.getEntityId();
    }

    static boolean isLocalFirstPersonShot(int shooterEntityId, GunType gunType) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || gunType == null || mc.gameSettings.thirdPersonView != 0) {
            return false;
        }
        return shooterEntityId == mc.player.getEntityId();
    }

    static Vector3f resolveOrigin(int shooterEntityId, GunType gunType, Vector3f packetOrigin) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || gunType == null
                || gunType.animationType != WeaponAnimationType.ENHANCED) {
            return packetOrigin;
        }
        boolean localFirstPerson = shooterEntityId == mc.player.getEntityId()
                && mc.gameSettings.thirdPersonView == 0;
        if (!localFirstPerson) {
            return packetOrigin;
        }

        GunEnhancedRenderConfig renderConfig = ModularWarfare.getRenderConfig(gunType, GunEnhancedRenderConfig.class);
        if (renderConfig == null || !renderConfig.fpTrailOriginCorrection) {
            return packetOrigin;
        }

        GunNodeWorld.trackTrailOriginNode(gunType);
        net.minecraft.util.math.Vec3d muzzle = GunNodeWorld.firstPersonMuzzle(mc.player, gunType);
        if (muzzle == null) {
            return packetOrigin;
        }
        net.minecraft.util.math.Vec3d world = GunNodeWorld.applyTrailWorldOffset(muzzle, gunType);
        return new Vector3f((float) world.x, (float) world.y, (float) world.z);
    }

    private static Vector3f applyThirdPersonOriginForward(int shooterEntityId, GunType gunType, Vector3f origin,
            double dirX, double dirY, double dirZ) {
        if (isLocalFirstPersonShot(shooterEntityId, gunType)) {
            return origin;
        }
        double lenSq = dirX * dirX + dirY * dirY + dirZ * dirZ;
        if (lenSq < 1.0E-12D) {
            return origin;
        }
        double invLen = THIRD_PERSON_ORIGIN_FORWARD / Math.sqrt(lenSq);
        origin.x += (float) (dirX * invLen);
        origin.y += (float) (dirY * invLen);
        origin.z += (float) (dirZ * invLen);
        return origin;
    }

    private static GunType resolveGunType(String gunTypeName) {
        if (gunTypeName == null || gunTypeName.isEmpty() || !ModularWarfare.gunTypes.containsKey(gunTypeName)) {
            return null;
        }
        return ModularWarfare.gunTypes.get(gunTypeName).type;
    }

    private static void flushPending(boolean localFirstPersonOnly, boolean excludeLocalFirstPerson) {
        List<PendingTesla> teslaReady = new ArrayList<>();
        for (PendingTesla p : PENDING_TESLA) {
            if (!p.shouldDefer(localFirstPersonOnly, excludeLocalFirstPerson)) {
                teslaReady.add(p);
            }
        }
        List<PendingGunTrail> gunReady = new ArrayList<>();
        for (PendingGunTrail p : PENDING_GUN) {
            if (!p.shouldDefer(localFirstPersonOnly, excludeLocalFirstPerson)) {
                gunReady.add(p);
            }
        }
        for (PendingTesla p : teslaReady) {
            PENDING_TESLA.remove(p);
            p.spawn();
        }
        for (PendingGunTrail p : gunReady) {
            PENDING_GUN.remove(p);
            p.spawn();
        }
    }

    private abstract static class PendingTrailBase {
        final int shooterEntityId;
        final double posX, posY, posZ;
        final String gunTypeName;

        PendingTrailBase(int shooterEntityId, double posX, double posY, double posZ, String gunTypeName) {
            this.shooterEntityId = shooterEntityId;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.gunTypeName = gunTypeName;
        }

        boolean shouldDefer(boolean localFirstPersonOnly, boolean excludeLocalFirstPerson) {
            GunType gunType = resolveGunType(gunTypeName);
            boolean local1p = isLocalFirstPersonShot(shooterEntityId, gunType);
            if (localFirstPersonOnly) {
                return !local1p;
            }
            if (excludeLocalFirstPerson) {
                return local1p;
            }
            return false;
        }
    }

    private static final class PendingTesla extends PendingTrailBase {
        final double targetX, targetY, targetZ;
        final float bulletSpeed;

        PendingTesla(int shooterEntityId, double posX, double posY, double posZ, double targetX, double targetY,
                double targetZ, float bulletSpeed, String gunTypeName) {
            super(shooterEntityId, posX, posY, posZ, gunTypeName);
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.bulletSpeed = bulletSpeed;
        }

        void spawn() {
            Vector3f origin = new Vector3f((float) posX, (float) posY, (float) posZ);
            GunType gunType = resolveGunType(gunTypeName);
            origin = resolveOrigin(shooterEntityId, gunType, origin);
            origin = applyThirdPersonOriginForward(shooterEntityId, gunType, origin, targetX - posX, targetY - posY,
                    targetZ - posZ);
            Vector3f target = new Vector3f((float) targetX, (float) targetY, (float) targetZ);
            InstantBulletTeslaRender.AddTeslaTrail(
                    new InstantBulletTeslaRender.TeslaTrail(origin, target, bulletSpeed, gunType));
        }
    }

    private static final class PendingGunTrail extends PendingTrailBase {
        final String gunType, model, tex;
        final boolean glow;
        final double motionX, motionZ, dirX, dirY, dirZ, range;
        final float bulletSpeed;

        PendingGunTrail(int shooterEntityId, String gunType, String model, String tex, boolean glow, double posX,
                double posY, double posZ, double motionX, double motionZ, double dirX, double dirY, double dirZ,
                double range, float bulletSpeed) {
            super(shooterEntityId, posX, posY, posZ, gunType);
            this.gunType = gunType;
            this.model = model;
            this.tex = tex;
            this.glow = glow;
            this.motionX = motionX;
            this.motionZ = motionZ;
            this.dirX = dirX;
            this.dirY = dirY;
            this.dirZ = dirZ;
            this.range = range;
            this.bulletSpeed = bulletSpeed;
        }

        void spawn() {
            if (!ModularWarfare.gunTypes.containsKey(gunType)) {
                return;
            }
            GunType type = ModularWarfare.gunTypes.get(gunType).type;
            Vector3f hit = new Vector3f((float) (posX + dirX * range), (float) (posY + dirY * range),
                    (float) (posZ + dirZ * range));
            Vector3f origin = new Vector3f((float) posX, (float) posY, (float) posZ);
            origin = resolveOrigin(shooterEntityId, type, origin);
            origin = applyThirdPersonOriginForward(shooterEntityId, type, origin, dirX, dirY, dirZ);
            InstantBulletRenderer.AddTrail(
                    new InstantBulletRenderer.InstantShotTrail(type, model, tex, glow, origin, hit, bulletSpeed));
        }
    }
}
