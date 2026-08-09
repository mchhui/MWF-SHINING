package com.modularwarfare.api;

import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.common.guns.AttachmentType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAttachment;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side gun/attachment node world poses (trail origin / debugnode / flashlight FP+TP).
 * Poses are sampled from the live GL modelview at the render-stack node depth (projection sampling).
 */
@SideOnly(Side.CLIENT)
public final class GunNodeWorld {

    private static final String DEFAULT_TRAIL_ORIGIN_NODE = "flashModel";
    private static final String DEFAULT_FLASHLIGHT_ORIGIN_NODE = "flashModel";

    private static final Set<NodeRef> TRAIL_TRACK_REFS = ConcurrentHashMap.newKeySet();
    private static final Set<NodeRef> FLASHLIGHT_TRACK_REFS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, NodePose> FP_WORLD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, NodePose> TP_WORLD_CACHE = new ConcurrentHashMap<>();

    private GunNodeWorld() {}

    public static final class NodePose {
        public final Vec3d pos;
        public final Vec3d dir;

        public NodePose(Vec3d pos, Vec3d dir) {
            this.pos = pos;
            this.dir = dir != null ? dir : Vec3d.ZERO;
        }
    }

    /** Gun model node, or a node on the attachment equipped in {@code slot}. */
    public static final class NodeRef {
        @Nullable
        public final AttachmentPresetEnum slot;
        public final String nodeName;

        private NodeRef(@Nullable AttachmentPresetEnum slot, String nodeName) {
            this.slot = slot;
            this.nodeName = nodeName;
        }

        public static NodeRef ofGun(String node) {
            if (node == null) {
                return null;
            }
            String t = node.trim();
            return t.isEmpty() ? null : new NodeRef(null, t);
        }

        public static NodeRef ofAttachment(AttachmentPresetEnum slot, String node) {
            if (slot == null || node == null) {
                return null;
            }
            String t = node.trim();
            return t.isEmpty() ? null : new NodeRef(slot, t);
        }

        public boolean isGun() {
            return slot == null;
        }

        public String cacheSuffix() {
            if (slot == null) {
                return "GUN|" + nodeName;
            }
            return "ATT|" + slot.typeName + "|" + nodeName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NodeRef)) {
                return false;
            }
            NodeRef other = (NodeRef) o;
            return slot == other.slot && Objects.equals(nodeName, other.nodeName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(slot, nodeName);
        }

        @Override
        public String toString() {
            return cacheSuffix();
        }
    }

    @Nullable
    public static GunEnhancedRenderConfig renderConfig(GunType gunType) {
        if (gunType == null || gunType.enhancedModel == null
                || !(gunType.enhancedModel.config instanceof GunEnhancedRenderConfig)) {
            return null;
        }
        return (GunEnhancedRenderConfig) gunType.enhancedModel.config;
    }

    public static String trailOriginNodeName(GunType gunType) {
        GunEnhancedRenderConfig cfg = renderConfig(gunType);
        if (cfg == null || cfg.trailOriginNode == null) {
            return DEFAULT_TRAIL_ORIGIN_NODE;
        }
        String t = cfg.trailOriginNode.trim();
        return t.isEmpty() ? DEFAULT_TRAIL_ORIGIN_NODE : t;
    }

    public static String flashlightOriginNodeName(GunType gunType) {
        GunEnhancedRenderConfig cfg = renderConfig(gunType);
        if (cfg == null || cfg.flashlightOriginNode == null) {
            return DEFAULT_FLASHLIGHT_ORIGIN_NODE;
        }
        String t = cfg.flashlightOriginNode.trim();
        return t.isEmpty() ? DEFAULT_FLASHLIGHT_ORIGIN_NODE : t;
    }

    /**
     * Prefer flashlight attachment GLTF {@code originNode} when present; else gun
     * {@code flashlightOriginNode}.
     */
    @Nullable
    public static NodeRef resolveFlashlightOrigin(@Nullable ItemStack gunStack, GunType gunType) {
        if (gunStack != null) {
            ItemStack flashAtt = GunType.getAttachment(gunStack, AttachmentPresetEnum.Flashlight);
            if (flashAtt != null && flashAtt.getItem() instanceof ItemAttachment) {
                AttachmentType attType = ((ItemAttachment) flashAtt.getItem()).type;
                if (attType != null && attType.model instanceof ModelAttachment) {
                    ModelAttachment attModel = (ModelAttachment) attType.model;
                    if (attModel.isGltf()) {
                        String origin = ModelAttachment.NODE_FLASHLIGHT_POINT;
                        if (attModel.config != null && attModel.config.flashlight != null
                                && attModel.config.flashlight.originNode != null
                                && !attModel.config.flashlight.originNode.trim().isEmpty()) {
                            origin = attModel.config.flashlight.originNode.trim();
                        }
                        if (attModel.existPart(origin)) {
                            return NodeRef.ofAttachment(AttachmentPresetEnum.Flashlight, origin);
                        }
                    }
                }
            }
        }
        if (gunType == null) {
            return null;
        }
        return NodeRef.ofGun(flashlightOriginNodeName(gunType));
    }

    @Nullable
    public static NodeRef resolveTrailOrigin(GunType gunType) {
        if (gunType == null) {
            return null;
        }
        return NodeRef.ofGun(trailOriginNodeName(gunType));
    }

    public static void trackTrailOriginNode(GunType gunType) {
        if (gunType == null) {
            return;
        }
        if (gunType.enhancedModel != null) {
            gunType.enhancedModel.ensureRequested(mchhui.hegltf.GltfLoadPriority.HIGH);
        }
        NodeRef ref = resolveTrailOrigin(gunType);
        if (ref != null) {
            TRAIL_TRACK_REFS.add(ref);
        }
    }

    public static void trackFlashlightOriginNode(GunType gunType) {
        trackFlashlightOriginNode(null, gunType);
    }

    public static void trackFlashlightOriginNode(@Nullable ItemStack gunStack, GunType gunType) {
        if (gunType == null) {
            return;
        }
        NodeRef ref = resolveFlashlightOrigin(gunStack, gunType);
        if (ref != null) {
            FLASHLIGHT_TRACK_REFS.add(ref);
        }
    }

    public static Vec3d applyTrailWorldOffset(Vec3d world, GunType gunType) {
        if (world == null) {
            return null;
        }
        GunEnhancedRenderConfig cfg = renderConfig(gunType);
        if (cfg == null || cfg.trailNodeWorldThirdPos == null) {
            return world;
        }
        Vector3f off = cfg.trailNodeWorldThirdPos;
        return world.add(off.x, off.y, off.z);
    }

    @Nullable
    public static Vec3d playerHeldFp(EntityLivingBase holder, String gltfNodeName) {
        NodePose pose = playerHeldFpPose(holder, gltfNodeName);
        return pose != null ? pose.pos : null;
    }

    @Nullable
    public static NodePose playerHeldFpPose(EntityLivingBase holder, String gltfNodeName) {
        return getFp(holder, NodeRef.ofGun(gltfNodeName));
    }

    @Nullable
    public static NodePose getFp(EntityLivingBase holder, @Nullable NodeRef ref) {
        if (holder == null || ref == null) {
            return null;
        }
        return FP_WORLD_CACHE.get(cacheKey(holder.getUniqueID(), ref));
    }

    @Nullable
    public static NodePose getTp(EntityLivingBase holder, @Nullable NodeRef ref) {
        if (holder == null || ref == null) {
            return null;
        }
        return TP_WORLD_CACHE.get(cacheKey(holder.getUniqueID(), ref));
    }

    @Nullable
    public static Vec3d firstPersonMuzzle(EntityLivingBase holder, GunType gunType) {
        if (holder == null || gunType == null) {
            return null;
        }
        NodePose pose = getFp(holder, resolveTrailOrigin(gunType));
        return pose != null ? pose.pos : null;
    }

    @Nullable
    public static NodePose firstPersonFlashlight(EntityLivingBase holder, GunType gunType) {
        return firstPersonFlashlight(holder, null, gunType);
    }

    @Nullable
    public static NodePose firstPersonFlashlight(EntityLivingBase holder, @Nullable ItemStack gunStack,
            GunType gunType) {
        if (holder == null || gunType == null) {
            return null;
        }
        return getFp(holder, resolveFlashlightOrigin(gunStack, gunType));
    }

    @Nullable
    public static NodePose thirdPersonFlashlight(EntityLivingBase holder, @Nullable ItemStack gunStack,
            GunType gunType) {
        if (holder == null || gunType == null) {
            return null;
        }
        return getTp(holder, resolveFlashlightOrigin(gunStack, gunType));
    }

    public static void clearFpCache(EntityLivingBase holder) {
        if (holder == null) {
            return;
        }
        String prefix = holder.getUniqueID().toString() + '|';
        FP_WORLD_CACHE.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public static void clearTpCache(EntityLivingBase holder) {
        if (holder == null) {
            return;
        }
        String prefix = holder.getUniqueID().toString() + '|';
        TP_WORLD_CACHE.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public static void putFpCache(EntityLivingBase holder, String gltfNodeName, Vec3d world) {
        putFpCache(holder, NodeRef.ofGun(gltfNodeName), world, null);
    }

    public static void putFpCache(EntityLivingBase holder, String gltfNodeName, Vec3d world, @Nullable Vec3d dir) {
        putFpCache(holder, NodeRef.ofGun(gltfNodeName), world, dir);
    }

    public static void putFpCache(EntityLivingBase holder, @Nullable NodeRef ref, Vec3d world, @Nullable Vec3d dir) {
        if (holder == null || ref == null || world == null) {
            return;
        }
        FP_WORLD_CACHE.put(cacheKey(holder.getUniqueID(), ref), new NodePose(world, dir));
    }

    public static void putTpCache(EntityLivingBase holder, @Nullable NodeRef ref, Vec3d world, @Nullable Vec3d dir) {
        if (holder == null || ref == null || world == null) {
            return;
        }
        TP_WORLD_CACHE.put(cacheKey(holder.getUniqueID(), ref), new NodePose(world, dir));
    }

    public static Set<NodeRef> trackedTrailRefs() {
        return Collections.unmodifiableSet(TRAIL_TRACK_REFS);
    }

    public static Set<NodeRef> trackedFlashlightRefs() {
        return Collections.unmodifiableSet(FLASHLIGHT_TRACK_REFS);
    }

    /** @deprecated use {@link #trackedTrailRefs()} */
    @Deprecated
    public static Set<String> trackedTrailNodes() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (NodeRef ref : TRAIL_TRACK_REFS) {
            if (ref != null && ref.isGun()) {
                names.add(ref.nodeName);
            }
        }
        return Collections.unmodifiableSet(names);
    }

    /** @deprecated use {@link #trackedFlashlightRefs()} */
    @Deprecated
    public static Set<String> trackedFlashlightNodes() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (NodeRef ref : FLASHLIGHT_TRACK_REFS) {
            if (ref != null && ref.isGun()) {
                names.add(ref.nodeName);
            }
        }
        return Collections.unmodifiableSet(names);
    }

    /**
     * Yaw-safe camera basis for hand-space → world (avoids look×worldUp singularity at pitch ±90°).
     * Returns {@code [right, up, forward]}.
     */
    public static Vec3d[] yawSafeCameraBasis(EntityLivingBase viewEntity, float partialTicks,
            boolean invertForward) {
        Vec3d forward = viewEntity.getLook(partialTicks);
        if (invertForward) {
            forward = forward.scale(-1.0D);
        }
        if (forward.lengthSquared() < 1.0E-12D) {
            forward = new Vec3d(0, 0, 1);
        } else {
            forward = forward.normalize();
        }
        Vec3d worldUp = new Vec3d(0.0D, 1.0D, 0.0D);
        Vec3d right;
        if (Math.abs(forward.y) > 0.999D) {
            float yaw = viewEntity.rotationYaw * 0.017453292F;
            Vec3d horizFwd = new Vec3d(-MathHelper.sin(yaw), 0.0D, MathHelper.cos(yaw));
            right = horizFwd.crossProduct(worldUp);
            if (right.lengthSquared() < 1.0E-12D) {
                right = new Vec3d(1.0D, 0.0D, 0.0D);
            } else {
                right = right.normalize();
            }
        } else {
            right = forward.crossProduct(worldUp);
            if (right.lengthSquared() < 1.0E-12D) {
                right = new Vec3d(1.0D, 0.0D, 0.0D);
            } else {
                right = right.normalize();
            }
        }
        Vec3d up = right.crossProduct(forward);
        if (up.lengthSquared() < 1.0E-12D) {
            up = worldUp;
        } else {
            up = up.normalize();
        }
        return new Vec3d[] { right, up, forward };
    }

    /** Apply local (right, up, forward) offset onto a world origin using a yaw-safe basis. */
    public static Vec3d applyLocalOffsetYawSafe(Vec3d origin, Vec3d look, Vector3f offset) {
        if (origin == null) {
            return Vec3d.ZERO;
        }
        if (offset == null || (offset.x == 0f && offset.y == 0f && offset.z == 0f)) {
            return origin;
        }
        Vec3d forward = look != null && look.lengthSquared() > 1.0E-12D ? look.normalize() : new Vec3d(0, 0, 1);
        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right;
        if (Math.abs(forward.y) > 0.999D) {
            // No entity yaw here — derive a stable perpendicular from an alternate up.
            Vec3d altUp = Math.abs(forward.x) < 0.9D ? new Vec3d(1, 0, 0) : new Vec3d(0, 0, 1);
            right = forward.crossProduct(altUp);
            if (right.lengthSquared() < 1.0E-12D) {
                right = new Vec3d(1, 0, 0);
            } else {
                right = right.normalize();
            }
        } else {
            right = forward.crossProduct(worldUp);
            if (right.lengthSquared() < 1.0E-12D) {
                right = new Vec3d(1, 0, 0);
            } else {
                right = right.normalize();
            }
        }
        Vec3d up = right.crossProduct(forward).normalize();
        return origin.add(right.scale(offset.x)).add(up.scale(offset.y)).add(forward.scale(offset.z));
    }

    static String cacheKey(UUID holderId, NodeRef ref) {
        return holderId.toString() + '|' + ref.cacheSuffix();
    }
}
