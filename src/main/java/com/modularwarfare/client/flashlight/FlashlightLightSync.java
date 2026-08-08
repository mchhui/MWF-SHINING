package com.modularwarfare.client.flashlight;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.GunNodeWorld;
import com.modularwarfare.client.compat.AtomicShaderCompat;
import com.modularwarfare.client.fpp.basic.configs.AttachmentRenderConfig;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.common.guns.AttachmentType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAttachment;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flashlight → Atomic dynamic light sync. Soft-depends on Atomic: without the mod,
 * all entry points no-op (no Atomic class resolution).
 */
@SideOnly(Side.CLIENT)
public final class FlashlightLightSync {

    private static final String ID_FP_PREFIX = "mwf:flashlight:fp:";
    private static final String ID_TP_PREFIX = "mwf:flashlight:tp:";

    private static final Set<UUID> ACTIVE_LIGHTS = new HashSet<>();
    private static final ConcurrentHashMap<UUID, GunNodeWorld.NodePose> TP_ARM_POSE = new ConcurrentHashMap<>();

    private FlashlightLightSync() {}

    public static void putTpArmPose(EntityLivingBase holder, Vec3d world, Vec3d dir) {
        if (holder == null || world == null) {
            return;
        }
        if (!AtomicShaderCompat.isAtomicLoaded()) {
            return;
        }
        if (AtomicShaderCompat.isShadowDepthActive()) {
            return;
        }
        TP_ARM_POSE.put(holder.getUniqueID(), new GunNodeWorld.NodePose(world, dir));
        onPoseCached(holder);
    }

    @Nullable
    public static GunNodeWorld.NodePose getTpArmPose(EntityLivingBase holder) {
        if (holder == null) {
            return null;
        }
        return TP_ARM_POSE.get(holder.getUniqueID());
    }

    public static void onPoseCached(EntityLivingBase holder) {
        if (!(holder instanceof EntityPlayer) || !AtomicShaderCompat.isAtomicLoaded()) {
            return;
        }
        if (!FlashlightAtomicLightBridge.isLightApiReady()) {
            return;
        }
        if (AtomicShaderCompat.isShadowDepthActive()) {
            return;
        }
        syncPlayer((EntityPlayer) holder);
    }

    public static void tick() {
        if (!AtomicShaderCompat.isAtomicLoaded()) {
            clearLocalOnly();
            return;
        }
        if (!FlashlightAtomicLightBridge.isLightApiReady()) {
            clearAll();
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
            clearAll();
            return;
        }

        Set<UUID> keep = new HashSet<>();
        for (EntityPlayer player : mc.world.playerEntities) {
            if (syncPlayer(player)) {
                keep.add(player.getUniqueID());
                ACTIVE_LIGHTS.add(player.getUniqueID());
            }
        }

        for (UUID stale : new HashSet<>(ACTIVE_LIGHTS)) {
            if (!keep.contains(stale)) {
                removePlayer(stale);
                ACTIVE_LIGHTS.remove(stale);
                TP_ARM_POSE.remove(stale);
            }
        }
    }

    private static boolean syncPlayer(EntityPlayer player) {
        if (player == null || player.isDead) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return false;
        }
        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemGun)) {
            return false;
        }
        GunType gunType = ((ItemGun) stack.getItem()).type;
        if (gunType == null || gunType.animationType != WeaponAnimationType.ENHANCED) {
            return false;
        }
        ItemStack flashAtt = GunType.getAttachment(stack, AttachmentPresetEnum.Flashlight);
        if (flashAtt == null || !(flashAtt.getItem() instanceof ItemAttachment)) {
            return false;
        }
        UUID id = player.getUniqueID();
        if (!FlashlightRenderManager.getInstance().isFlashlightOnFor(id)) {
            return false;
        }

        AttachmentRenderConfig.Flashlight cfg = resolveCfg(flashAtt);
        GunNodeWorld.trackFlashlightOriginNode(gunType);
        float partial = mc.getRenderPartialTicks();
        boolean isLocal = mc.player.getUniqueID().equals(id);

        if (isLocal) {
            syncLocal(player, gunType, cfg, partial, mc.gameSettings.thirdPersonView == 0);
        } else {
            syncRemote(player, cfg, partial);
        }
        return true;
    }

    private static void syncLocal(EntityPlayer player, GunType gunType, AttachmentRenderConfig.Flashlight cfg,
            float partial, boolean firstPersonCam) {
        UUID uuid = player.getUniqueID();
        String fpId = ID_FP_PREFIX + uuid;
        String tpId = ID_TP_PREFIX + uuid;
        float[] rgb = resolveRgb(cfg);

        if (firstPersonCam) {
            GunNodeWorld.NodePose fpPose = GunNodeWorld.firstPersonFlashlight(player, gunType);
            if (fpPose == null || fpPose.pos == null) {
                FlashlightAtomicLightBridge.remove(fpId);
                FlashlightAtomicLightBridge.remove(tpId);
                return;
            }
            Vec3d look = player.getLook(partial);
            Vec3d dir = resolveDir(fpPose, look);
            AttachmentRenderConfig.Flashlight.ViewTune tune = cfg.firstPerson;
            Vec3d pos = applyPosOffset(fpPose.pos, dir, tune != null ? tune.posOffset : null);
            dir = applyRotOffset(dir, tune != null ? tune.rotOffset : null);
            if (isBlockedByGeometry(player, pos, partial)) {
                FlashlightAtomicLightBridge.remove(fpId);
                FlashlightAtomicLightBridge.remove(tpId);
                return;
            }
            FlashlightAtomicLightBridge.upsertFpSpot(fpId, pos, dir, rgb, cfg);
            FlashlightAtomicLightBridge.remove(tpId);
        } else {
            if (!upsertTpArmPose(player, cfg, partial, tpId, rgb, false)) {
                FlashlightAtomicLightBridge.remove(tpId);
            }
            FlashlightAtomicLightBridge.remove(fpId);
        }
    }

    private static void syncRemote(EntityPlayer player, AttachmentRenderConfig.Flashlight cfg, float partial) {
        UUID uuid = player.getUniqueID();
        float[] rgb = resolveRgb(cfg);
        String tpId = ID_TP_PREFIX + uuid;
        if (!upsertTpArmPose(player, cfg, partial, tpId, rgb, true)) {
            FlashlightAtomicLightBridge.remove(tpId);
        }
        FlashlightAtomicLightBridge.remove(ID_FP_PREFIX + uuid);
    }

    /** @param remote true = ALWAYS visibility; false = THIRD_PERSON */
    private static boolean upsertTpArmPose(EntityPlayer player, AttachmentRenderConfig.Flashlight cfg,
            float partial, String id, float[] rgb, boolean remote) {
        GunNodeWorld.NodePose tpPose = getTpArmPose(player);
        if (tpPose == null || tpPose.pos == null) {
            return false;
        }
        Vec3d look = player.getLook(partial);
        Vec3d dir = resolveDir(tpPose, look);
        AttachmentRenderConfig.Flashlight.ViewTune tune = cfg.thirdPerson;
        Vec3d pos = applyPosOffset(tpPose.pos, dir, tune != null ? tune.posOffset : null);
        dir = applyRotOffset(dir, tune != null ? tune.rotOffset : null);
        if (isBlockedByGeometry(player, pos, partial)) {
            return false;
        }
        if (remote) {
            FlashlightAtomicLightBridge.upsertRemoteTpSpot(id, pos, dir, rgb, cfg);
        } else {
            FlashlightAtomicLightBridge.upsertLocalTpSpot(id, pos, dir, rgb, cfg);
        }
        return true;
    }

    /**
     * Wall-clip gate: ray from flashlight origin to the player's eye (FP screen-center camera /
     * TP eye — never the third-person camera). Uses {@link ModularWarfare#RAY_CASTING} so the
     * shared penetrable-block list (tallgrass / double_plant / barrier) is skipped. Any earlier
     * solid hit cancels the light.
     */
    private static boolean isBlockedByGeometry(EntityPlayer player, Vec3d flashPos, float partial) {
        if (player == null || player.world == null || flashPos == null) {
            return true;
        }
        Vec3d eye = player.getPositionEyes(partial);
        double eyeDistSq = flashPos.squareDistanceTo(eye);
        if (eyeDistSq < 1.0E-8D) {
            return false;
        }
        List<RayTraceResult> hits = null;
        if (ModularWarfare.INSTANCE != null && ModularWarfare.INSTANCE.RAY_CASTING != null) {
            hits = ModularWarfare.INSTANCE.RAY_CASTING.rayTraceBlocks(
                    player.world, flashPos, eye, 0f, 0f, false, true, false);
        } else {
            RayTraceResult single = player.world.rayTraceBlocks(flashPos, eye, false, true, false);
            if (single != null && single.typeOfHit == RayTraceResult.Type.BLOCK) {
                hits = java.util.Collections.singletonList(single);
            }
        }
        if (hits == null || hits.isEmpty()) {
            return false;
        }
        RayTraceResult hit = hits.get(0);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.hitVec == null) {
            return false;
        }
        double hitDistSq = flashPos.squareDistanceTo(hit.hitVec);
        return hitDistSq + 1.0E-4D < eyeDistSq;
    }

    private static Vec3d applyPosOffset(Vec3d origin, Vec3d look, Vector3f offset) {
        if (origin == null) {
            return Vec3d.ZERO;
        }
        if (offset == null || (offset.x == 0f && offset.y == 0f && offset.z == 0f)) {
            return origin;
        }
        Vec3d forward = look != null && look.lengthSquared() > 1.0E-12D ? look.normalize() : new Vec3d(0, 0, 1);
        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(worldUp);
        if (right.lengthSquared() < 1.0E-12D) {
            right = new Vec3d(1, 0, 0);
        } else {
            right = right.normalize();
        }
        Vec3d up = right.crossProduct(forward).normalize();
        return origin.add(right.scale(offset.x)).add(up.scale(offset.y)).add(forward.scale(offset.z));
    }

    private static Vec3d applyRotOffset(Vec3d dir, Vector3f rotDeg) {
        if (dir == null || dir.lengthSquared() < 1.0E-12D) {
            return new Vec3d(0, 0, 1);
        }
        Vec3d forward = dir.normalize();
        if (rotDeg == null || (rotDeg.x == 0f && rotDeg.y == 0f)) {
            return forward;
        }
        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(worldUp);
        if (right.lengthSquared() < 1.0E-12D) {
            right = new Vec3d(1, 0, 0);
        } else {
            right = right.normalize();
        }
        Vec3d up = right.crossProduct(forward).normalize();

        double yaw = Math.toRadians(rotDeg.y);
        double pitch = Math.toRadians(rotDeg.x);
        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        Vec3d yawed = forward.scale(cy).add(right.scale(sy));
        if (yawed.lengthSquared() < 1.0E-12D) {
            yawed = forward;
        } else {
            yawed = yawed.normalize();
        }
        right = yawed.crossProduct(up);
        if (right.lengthSquared() < 1.0E-12D) {
            right = new Vec3d(1, 0, 0);
        } else {
            right = right.normalize();
        }
        up = right.crossProduct(yawed).normalize();
        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        Vec3d pitched = yawed.scale(cp).add(up.scale(sp));
        if (pitched.lengthSquared() < 1.0E-12D) {
            return yawed;
        }
        return pitched.normalize();
    }

    private static Vec3d resolveDir(GunNodeWorld.NodePose pose, Vec3d look) {
        if (pose != null && pose.dir != null && pose.dir.lengthSquared() > 1.0E-8D) {
            return pose.dir.normalize();
        }
        if (look != null && look.lengthSquared() > 1.0E-8D) {
            return look.normalize();
        }
        return new Vec3d(0, 0, 1);
    }

    private static AttachmentRenderConfig.Flashlight resolveCfg(ItemStack flashAtt) {
        AttachmentType attType = ((ItemAttachment) flashAtt.getItem()).type;
        if (attType.model instanceof ModelAttachment) {
            AttachmentRenderConfig renderCfg = ((ModelAttachment) attType.model).config;
            if (renderCfg != null && renderCfg.flashlight != null) {
                return renderCfg.flashlight;
            }
        }
        return defaultFlashCfg();
    }

    private static float[] resolveRgb(AttachmentRenderConfig.Flashlight cfg) {
        if (cfg.color != null && cfg.color.length >= 3) {
            return new float[]{Math.max(0f, cfg.color[0]), Math.max(0f, cfg.color[1]), Math.max(0f, cfg.color[2])};
        }
        return new float[]{1f, 0.95f, 0.85f};
    }

    private static AttachmentRenderConfig.Flashlight defaultFlashCfg() {
        return new AttachmentRenderConfig.Flashlight();
    }

    private static void removePlayer(UUID uuid) {
        FlashlightAtomicLightBridge.remove(ID_FP_PREFIX + uuid);
        FlashlightAtomicLightBridge.remove(ID_TP_PREFIX + uuid);
    }

    private static void clearLocalOnly() {
        ACTIVE_LIGHTS.clear();
        TP_ARM_POSE.clear();
    }

    private static void clearAll() {
        if (!ACTIVE_LIGHTS.isEmpty() && AtomicShaderCompat.isAtomicLoaded()
                && FlashlightAtomicLightBridge.isLightApiReady()) {
            for (UUID uuid : ACTIVE_LIGHTS) {
                removePlayer(uuid);
            }
        }
        clearLocalOnly();
    }
}
