package com.modularwarfare.client.trail;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.GunNodeWorld;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.client.model.InstantBulletRenderer;
import com.modularwarfare.client.model.InstantBulletTeslaRender;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.utility.vector.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

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
 * <p>
 * {@code /mw-client debugnode on} 时绘制：黄线=trailOrigin 节点朝向，红线=包眼位→命中，绿线=纠正后起点→命中。
 */
@SideOnly(Side.CLIENT)
public final class TrailOriginResolver {

    private static final float THIRD_PERSON_ORIGIN_FORWARD = 2.0F;
    private static final float LIVE_RAY_LENGTH = 8.0F;
    private static final long DEBUG_RAY_TTL_MS = 5000L;
    private static final int MAX_DEBUG_RAYS = 24;

    private static int frameCounter = 0;

    private static final CopyOnWriteArrayList<PendingTesla> PENDING_TESLA = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<PendingGunTrail> PENDING_GUN = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<DebugRay> DEBUG_RAYS = new CopyOnWriteArrayList<>();

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

    /**
     * World-space trail debug lines when {@link RenderGunEnhanced#debugMarkerNodes} is on.
     * Uses the same {@code translate(-camera)} convention as {@link InstantBulletRenderer}.
     */
    public static void renderTrailDebug(float partialTicks) {
        if (!RenderGunEnhanced.debugMarkerNodes) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.getRenderViewEntity() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (DebugRay r : DEBUG_RAYS) {
            if (now > r.expireMs) {
                DEBUG_RAYS.remove(r);
            }
        }

        Entity camera = mc.getRenderViewEntity();
        double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTicks;
        double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks;
        double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTicks;

        boolean prevDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevTex = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean prevLighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.pushMatrix();
        try {
            // Same as InstantBulletRenderer: camera rot already in MV; subtract entity pos.
            GL11.glTranslated(-cx, -cy + 0.1D, -cz);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
            GlStateManager.glLineWidth(2.5F);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);

            for (DebugRay r : DEBUG_RAYS) {
                drawWorldLineAbs(r.ax, r.ay, r.az, r.bx, r.by, r.bz, r.cr, r.cg, r.cb, 1f);
            }

            drawLiveTrailOriginRay(mc);
        } catch (Throwable t) {
            ModularWarfare.LOGGER.warn("Trail debug render failed: {}", t.toString());
        } finally {
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GlStateManager.glLineWidth(1.0F);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            if (prevLighting) {
                GlStateManager.enableLighting();
            }
            if (prevTex) {
                GlStateManager.enableTexture2D();
            }
            if (prevDepth) {
                GlStateManager.enableDepth();
            } else {
                GlStateManager.disableDepth();
            }
            GlStateManager.popMatrix();
        }
    }

    private static void drawLiveTrailOriginRay(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        ItemStack stack = mc.player.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGun)) {
            return;
        }
        GunType gunType = ((ItemGun) stack.getItem()).type;
        if (gunType == null || gunType.animationType != WeaponAnimationType.ENHANCED) {
            return;
        }
        GunNodeWorld.trackTrailOriginNode(gunType);
        boolean firstPerson = mc.gameSettings.thirdPersonView == 0;
        GunNodeWorld.NodeRef ref = GunNodeWorld.resolveTrailOrigin(gunType);
        GunNodeWorld.NodePose pose = firstPerson ? GunNodeWorld.getFp(mc.player, ref)
                : GunNodeWorld.getTp(mc.player, ref);
        if (pose == null || pose.pos == null || pose.dir == null) {
            return;
        }
        Vec3d start = GunNodeWorld.applyTrailWorldOffset(pose.pos, gunType);
        if (start == null) {
            start = pose.pos;
        }
        Vec3d dir = pose.dir;
        double len = Math.sqrt(dir.x * dir.x + dir.y * dir.y + dir.z * dir.z);
        if (len < 1.0E-8D) {
            return;
        }
        double s = LIVE_RAY_LENGTH / len;
        Vec3d end = start.add(dir.x * s, dir.y * s, dir.z * s);
        drawWorldLineAbs(start.x, start.y, start.z, end.x, end.y, end.z, 1f, 0.92f, 0.15f, 1f);
        if (Math.abs(start.x - pose.pos.x) + Math.abs(start.y - pose.pos.y)
                + Math.abs(start.z - pose.pos.z) > 1.0E-4D) {
            drawWorldLineAbs(pose.pos.x, pose.pos.y, pose.pos.z, start.x, start.y, start.z, 0.2f, 0.9f, 1f, 1f);
        }
    }

    private static void drawWorldLineAbs(double ax, double ay, double az, double bx, double by, double bz, float r,
            float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(ax, ay, az);
        GL11.glVertex3d(bx, by, bz);
        GL11.glEnd();
    }

    private static void recordDebugRay(Vector3f a, Vector3f b, float cr, float cg, float cb) {
        if (!RenderGunEnhanced.debugMarkerNodes || a == null || b == null) {
            return;
        }
        while (DEBUG_RAYS.size() >= MAX_DEBUG_RAYS) {
            DEBUG_RAYS.remove(0);
        }
        DEBUG_RAYS.add(new DebugRay(a.x, a.y, a.z, b.x, b.y, b.z, cr, cg, cb,
                System.currentTimeMillis() + DEBUG_RAY_TTL_MS));
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

    private static final class DebugRay {
        final float ax, ay, az, bx, by, bz, cr, cg, cb;
        final long expireMs;

        DebugRay(float ax, float ay, float az, float bx, float by, float bz, float cr, float cg, float cb,
                long expireMs) {
            this.ax = ax;
            this.ay = ay;
            this.az = az;
            this.bx = bx;
            this.by = by;
            this.bz = bz;
            this.cr = cr;
            this.cg = cg;
            this.cb = cb;
            this.expireMs = expireMs;
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
            Vector3f packetOrigin = new Vector3f((float) posX, (float) posY, (float) posZ);
            GunType gunType = resolveGunType(gunTypeName);
            Vector3f origin = resolveOrigin(shooterEntityId, gunType, new Vector3f(packetOrigin));
            origin = applyThirdPersonOriginForward(shooterEntityId, gunType, origin, targetX - posX, targetY - posY,
                    targetZ - posZ);
            Vector3f target = new Vector3f((float) targetX, (float) targetY, (float) targetZ);
            recordDebugRay(packetOrigin, target, 1f, 0.2f, 0.2f);
            recordDebugRay(new Vector3f(origin), target, 0.25f, 1f, 0.3f);
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
            Vector3f packetOrigin = new Vector3f((float) posX, (float) posY, (float) posZ);
            Vector3f packetHit = new Vector3f((float) (posX + dirX * range), (float) (posY + dirY * range),
                    (float) (posZ + dirZ * range));
            Vector3f origin = resolveOrigin(shooterEntityId, type, new Vector3f(packetOrigin));
            origin = applyThirdPersonOriginForward(shooterEntityId, type, origin, dirX, dirY, dirZ);
            // Keep aim direction/range after origin correction (do not keep eye-based hit).
            Vector3f hit = new Vector3f(origin.x + (float) (dirX * range), origin.y + (float) (dirY * range),
                    origin.z + (float) (dirZ * range));
            recordDebugRay(packetOrigin, packetHit, 1f, 0.2f, 0.2f);
            recordDebugRay(new Vector3f(origin), hit, 0.25f, 1f, 0.3f);
            InstantBulletRenderer.AddTrail(
                    new InstantBulletRenderer.InstantShotTrail(type, model, tex, glow, origin, hit, bulletSpeed));
        }
    }
}
