package com.modularwarfare.client.flashlight;

import cloud.siz.atomic.api.light.AtomicLightApi;
import cloud.siz.atomic.api.light.AtomicLightSpec;
import cloud.siz.atomic.api.light.LightPoolPolicy;
import cloud.siz.atomic.api.light.LightVisibility;
import com.modularwarfare.client.compat.AtomicShaderCompat;
import com.modularwarfare.client.fpp.basic.configs.AttachmentRenderConfig;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Isolated Atomic Light API calls. Only load this class after
 * {@link AtomicShaderCompat#isAtomicLoaded()} is true (soft-depend).
 */
@SideOnly(Side.CLIENT)
final class FlashlightAtomicLightBridge {

    private static final String OWNER = "mwf:flashlight";

    private FlashlightAtomicLightBridge() {}

    static boolean canTalkToApi() {
        return AtomicLightApi.isAvailable();
    }

    static boolean isLightApiReady() {
        return AtomicShaderCompat.isPipelineEnabled() && canTalkToApi();
    }

    static void remove(String id) {
        if (!canTalkToApi()) {
            return;
        }
        AtomicLightApi.remove(id);
    }

    static void removeByPrefix(String prefix) {
        if (!canTalkToApi() || prefix == null || prefix.isEmpty()) {
            return;
        }
        AtomicLightApi.removeByPrefix(prefix);
    }

    static void upsertFpSpot(String id, Vec3d pos, Vec3d dir, float[] rgb,
            AttachmentRenderConfig.Flashlight cfg) {
        upsertSpot(id, pos, dir, rgb, cfg, LightVisibility.FIRST_PERSON, LightPoolPolicy.FORCE);
    }

    static void upsertLocalTpSpot(String id, Vec3d pos, Vec3d dir, float[] rgb,
            AttachmentRenderConfig.Flashlight cfg) {
        upsertSpot(id, pos, dir, rgb, cfg, LightVisibility.THIRD_PERSON, LightPoolPolicy.DISTANCE);
    }

    static void upsertRemoteTpSpot(String id, Vec3d pos, Vec3d dir, float[] rgb,
            AttachmentRenderConfig.Flashlight cfg) {
        upsertSpot(id, pos, dir, rgb, cfg, LightVisibility.ALWAYS, LightPoolPolicy.DISTANCE);
    }

    private static void upsertSpot(String id, Vec3d pos, Vec3d dir, float[] rgb,
            AttachmentRenderConfig.Flashlight cfg, LightVisibility visibility, LightPoolPolicy pool) {
        if (pos == null || dir == null || dir.lengthSquared() < 1.0E-12D) {
            AtomicLightApi.remove(id);
            return;
        }
        Vec3d d = dir.normalize();
        AtomicLightApi.upsert(AtomicLightSpec.spot(
                        id,
                        (float) pos.x, (float) pos.y, (float) pos.z,
                        (float) d.x, (float) d.y, (float) d.z,
                        rgb[0], rgb[1], rgb[2],
                        cfg.intensity, cfg.range, cfg.innerDeg, cfg.outerDeg)
                .withVisibility(visibility)
                .withPool(pool)
                .withBeamFactor(cfg.beamFactor)
                .withNearGeometryCull(cfg.nearGeometryCull)
                .withOwner(OWNER));
    }
}
