package com.modularwarfare.client.fpp.enhanced.models;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.IMWModel;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.common.type.BaseType;

import mchhui.hegltf.DataAnimation;
import mchhui.hegltf.DataAnimation.Transform;
import mchhui.hegltf.DataNode;
import mchhui.hegltf.GltfDataModel;
import mchhui.hegltf.GltfFeatureFlags;
import mchhui.hegltf.GltfLoadPriority;
import mchhui.hegltf.GltfModelHandle;
import mchhui.hegltf.GltfModelManager;
import mchhui.hegltf.GltfRenderModel;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import mchhui.hegltf.GltfRenderModel.NodeAnimationMapper;
import mchhui.hegltf.GltfRenderModel.NodeState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashSet;

import de.javagl.jgltf.model.NodeModel;

public class EnhancedModel implements IMWModel {
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    public EnhancedRenderConfig config;
    public BaseType baseType;
    public GltfRenderModel model;
    public boolean initCal = false;

    private ResourceLocation boundLocation;
    private int boundGeneration = -1;
    private boolean pinned;

    private float cachedPoseTime = Float.NaN;
    private float cachedSprintTime = Float.NaN;
    private float cachedSprintAlpha = Float.NaN;
    private float cachedAimTime = Float.NaN;
    private float cachedAdsAlpha = Float.NaN;
    private float cachedAmmoPer = Float.NaN;
    private boolean cachedBasicSprint;
    private boolean poseCacheValid = false;
    private boolean skinnedForCachedPose = false;

    public EnhancedModel(EnhancedRenderConfig config, BaseType baseType) {
        this.config = config;
        this.baseType = baseType;
        this.model = new GltfRenderModel(null);
        if (!GltfModelManager.isLazyEnabled()) {
            ensureRequested(GltfLoadPriority.HIGH);
        }
    }

    public ResourceLocation getModelLocation() {
        return new ResourceLocation(ModularWarfare.MOD_ID,
            "gltf/" + baseType.getAssetDir() + "/" + this.config.modelFileName);
    }

    public void ensureRequested() {
        ensureRequested(GltfLoadPriority.NORMAL);
    }

    public void ensureRequested(GltfLoadPriority priority) {
        ResourceLocation loc = getModelLocation();
        GltfModelHandle handle = GltfModelManager.get().request(loc, priority);
        syncFromHandle(handle);
    }

    public void forceReload() {
        ResourceLocation loc = getModelLocation();
        GltfModelManager.get().forceUnload(loc);
        if (model != null) {
            model.bindGeoModel(null);
        }
        initCal = false;
        invalidatePoseCache();
        pinned = false;
        ensureRequested(GltfLoadPriority.HIGH);
    }

    public void pin() {
        ensureRequested(GltfLoadPriority.HIGH);
        if (!pinned) {
            GltfModelManager.get().pin(getModelLocation());
            pinned = true;
        }
    }

    public void unpin() {
        if (pinned) {
            GltfModelManager.get().unpin(getModelLocation());
            pinned = false;
        }
    }

    private void syncFromHandle(GltfModelHandle handle) {
        if (handle == null) {
            return;
        }
        GltfDataModel data = handle.getDataModel();
        int gen = handle.getGeneration();
        if (data == null) {
            if (model != null && model.geoModel != null) {
                model.bindGeoModel(null);
                initCal = false;
                invalidatePoseCache();
            }
            boundGeneration = gen;
            return;
        }
        if (model.geoModel != data || boundGeneration != gen) {
            if (model.geoModel != data) {
                model.bindGeoModel(data);
                initCal = false;
                invalidatePoseCache();
            } else {
                model.invalidateMeshNodes();
            }
            boundLocation = handle.location;
            boundGeneration = gen;
        }
    }

    public boolean isAnimReady() {
        ensureRequested(GltfLoadPriority.HIGH);
        return model != null && model.geoModel != null && model.geoModel.isAnimReady();
    }

    public boolean isMeshReady() {
        return model != null && model.geoModel != null && model.geoModel.isMeshReady();
    }

    public static void clearCache() {
        GltfModelManager.get().clearAll();
    }

    public void loadAnimation(EnhancedModel other, boolean skin) {
        if (model == null || other == null || other.model == null) {
            return;
        }
        ensureRequested(GltfLoadPriority.HIGH);
        other.ensureRequested(GltfLoadPriority.HIGH);
        if (!isAnimReady() || !other.isAnimReady()) {
            return;
        }
        model.loadAnimation(other.model, skin);
        initCal = true;
        invalidatePoseCache();
    }

    public void invalidatePoseCache() {
        poseCacheValid = false;
        skinnedForCachedPose = false;
        cachedPoseTime = Float.NaN;
    }

    public void updateAnimationBlended(float time, boolean skin, boolean basicSprint, float sprintTime,
            float sprintAlpha, float aimTime, float adsAlpha, float ammoPer) {
        if (model == null || !isAnimReady()) {
            return;
        }
        boolean forceSkinFirst = !initCal;
        boolean samePose = GltfFeatureFlags.skinAnimOpt() && poseCacheValid && initCal
                && Float.compare(cachedPoseTime, time) == 0
                && Float.compare(cachedSprintTime, sprintTime) == 0
                && Float.compare(cachedSprintAlpha, sprintAlpha) == 0
                && Float.compare(cachedAimTime, aimTime) == 0
                && Float.compare(cachedAdsAlpha, adsAlpha) == 0
                && Float.compare(cachedAmmoPer, ammoPer) == 0
                && cachedBasicSprint == basicSprint;
        if (!samePose) {
            initCal = model.updatePose(time);
            cachedPoseTime = time;
            cachedSprintTime = sprintTime;
            cachedSprintAlpha = sprintAlpha;
            cachedAimTime = aimTime;
            cachedAdsAlpha = adsAlpha;
            cachedAmmoPer = ammoPer;
            cachedBasicSprint = basicSprint;
            poseCacheValid = initCal;
            skinnedForCachedPose = false;
        }
        if (skin) {
            model.skinFromPose();
            skinnedForCachedPose = true;
            initCal = true;
        } else if ((forceSkinFirst || !initCal) && !skinnedForCachedPose) {
            model.skinFromPose();
            skinnedForCachedPose = true;
            initCal = true;
        }
    }

    public void updateAnimation(float time, boolean skin) {
        if (!isAnimReady()) {
            return;
        }
        boolean forceSkinFirst = !initCal;
        invalidatePoseCache();
        initCal = model.updateAnimation(time, skin || forceSkinFirst);
        if (initCal) {
            poseCacheValid = true;
            cachedPoseTime = time;
            if (skin || forceSkinFirst) {
                skinnedForCachedPose = true;
            }
        }
    }

    public void updatePose(float time) {
        if (!isAnimReady()) {
            return;
        }
        initCal = model.updatePose(time);
        skinnedForCachedPose = false;
        if (initCal) {
            poseCacheValid = true;
            cachedPoseTime = time;
        } else {
            poseCacheValid = false;
            cachedPoseTime = Float.NaN;
        }
    }

    public void skinFromPose() {
        if (model == null || !isAnimReady()) {
            return;
        }
        model.skinFromPose();
        skinnedForCachedPose = true;
        initCal = true;
    }

    public Transform findLocalTransform(String name, float time) {
        if (model == null || !isAnimReady()) {
            return null;
        }
        DataNode node = model.geoModel.nodes.get(name);
        if (node == null) {
            return null;
        }
        DataAnimation ani = model.geoModel.animations.get(name);
        if (ani == null) {
            return null;
        }
        return model.geoModel.animations.get(name).findTransform(time, node.pos, node.size, node.rot);
    }

    public void setAnimationCalBlender(NodeAnimationBlender blender) {
        model.setNodeAnimationCalBlender(blender);
    }

    public void setAnimationLoadMapper(NodeAnimationMapper mapper) {
        model.setNodeAnimationLoadMapper(mapper);
    }

    @Deprecated
    public void updateAnimation(float time) {
        updateAnimation(time, true);
    }

    public boolean existPart(String part) {
        if (!isAnimReady()) {
            return false;
        }
        return model.geoModel.nodes.containsKey(part);
    }

    @Deprecated
    public NodeModel getPart(String part) {
        if (!isAnimReady()) {
            return null;
        }
        DataNode node = model.geoModel.nodes.get(part);
        if (node == null) {
            return null;
        }
        return node.unsafeNode;
    }

    public void beginDrawScope() {
        if (model != null) {
            model.beginDrawScope();
        }
    }

    public void endDrawScope() {
        if (model != null) {
            model.endDrawScope();
        }
    }

    public void invalidateDrawScopeBase() {
        if (model != null) {
            model.invalidateDrawScopeBase();
        }
    }

    public void renderOnly(HashSet<String> parts) {
        if (!initCal || parts == null || parts.isEmpty() || !isAnimReady()) {
            return;
        }
        model.renderOnly(parts);
    }

    @Override
    public void renderPart(String part, float scale) {
        if (!initCal || !isAnimReady()) {
            return;
        }
        model.renderPart(part);
    }

    public void renderPart(String part) {
        if (!initCal || !isAnimReady()) {
            return;
        }
        model.renderPart(part);
    }

    public void renderPartExcept(HashSet<String> set) {
        if (!initCal || !isAnimReady()) {
            return;
        }
        model.renderExcept(set);
    }

    public void renderPart(String[] only) {
        if (!initCal || !isAnimReady()) {
            return;
        }
        model.renderOnly(only);
    }

    public Matrix4f getGlobalTransform(String name) {
        if (!initCal || !isAnimReady()) {
            return new Matrix4f();
        }
        NodeState state = model.nodeStates.get(name);
        if (state == null) {
            return new Matrix4f();
        }
        return state.mat;
    }

    public void applyGlobalTransformToOther(String binding, Runnable run) {
        if (!initCal || !isAnimReady()) {
            return;
        }
        NodeState state = model.nodeStates.get(binding);
        if (state == null) {
            return;
        }
        invalidateDrawScopeBase();
        GlStateManager.pushMatrix();
        GlStateManager.multMatrix(state.mat.get(MATRIX_BUFFER));
        run.run();
        GlStateManager.popMatrix();
        invalidateDrawScopeBase();
    }
}
