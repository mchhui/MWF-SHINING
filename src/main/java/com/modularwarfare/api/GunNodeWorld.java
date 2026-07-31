package com.modularwarfare.api;

import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.common.guns.GunType;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端第一人称枪口/手电节点世界坐标缓存（尾迹起点 / debugnode / 手电 FP）。 */
@SideOnly(Side.CLIENT)
public final class GunNodeWorld {

    private static final String DEFAULT_TRAIL_ORIGIN_NODE = "flashModel";
    private static final String DEFAULT_FLASHLIGHT_ORIGIN_NODE = "flashModel";

    private static final Set<String> TRAIL_TRACK_NODES = ConcurrentHashMap.newKeySet();
    private static final Set<String> FLASHLIGHT_TRACK_NODES = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, NodePose> FP_WORLD_CACHE = new ConcurrentHashMap<>();

    private GunNodeWorld() {}

    public static final class NodePose {
        public final Vec3d pos;
        public final Vec3d dir;

        public NodePose(Vec3d pos, Vec3d dir) {
            this.pos = pos;
            this.dir = dir != null ? dir : Vec3d.ZERO;
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

    public static void trackTrailOriginNode(GunType gunType) {
        if (gunType == null) {
            return;
        }
        String node = trailOriginNodeName(gunType).trim();
        if (!node.isEmpty()) {
            TRAIL_TRACK_NODES.add(node);
        }
    }

    public static void trackFlashlightOriginNode(GunType gunType) {
        if (gunType == null) {
            return;
        }
        String node = flashlightOriginNodeName(gunType).trim();
        if (!node.isEmpty()) {
            FLASHLIGHT_TRACK_NODES.add(node);
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
        if (holder == null || gltfNodeName == null) {
            return null;
        }
        return FP_WORLD_CACHE.get(nodeCacheKey(holder.getUniqueID(), gltfNodeName.trim()));
    }

    @Nullable
    public static Vec3d firstPersonMuzzle(EntityLivingBase holder, GunType gunType) {
        if (holder == null || gunType == null) {
            return null;
        }
        return playerHeldFp(holder, trailOriginNodeName(gunType));
    }

    @Nullable
    public static NodePose firstPersonFlashlight(EntityLivingBase holder, GunType gunType) {
        if (holder == null || gunType == null) {
            return null;
        }
        return playerHeldFpPose(holder, flashlightOriginNodeName(gunType));
    }

    public static void clearFpCache(EntityLivingBase holder) {
        if (holder == null) {
            return;
        }
        String prefix = holder.getUniqueID().toString() + '|';
        FP_WORLD_CACHE.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public static void putFpCache(EntityLivingBase holder, String gltfNodeName, Vec3d world) {
        putFpCache(holder, gltfNodeName, world, null);
    }

    public static void putFpCache(EntityLivingBase holder, String gltfNodeName, Vec3d world, @Nullable Vec3d dir) {
        if (holder == null || gltfNodeName == null || world == null) {
            return;
        }
        FP_WORLD_CACHE.put(nodeCacheKey(holder.getUniqueID(), gltfNodeName.trim()), new NodePose(world, dir));
    }

    public static Set<String> trackedTrailNodes() {
        return Collections.unmodifiableSet(TRAIL_TRACK_NODES);
    }

    public static Set<String> trackedFlashlightNodes() {
        return Collections.unmodifiableSet(FLASHLIGHT_TRACK_NODES);
    }

    static String nodeCacheKey(UUID holderId, String nodeName) {
        return holderId.toString() + '|' + nodeName;
    }
}
