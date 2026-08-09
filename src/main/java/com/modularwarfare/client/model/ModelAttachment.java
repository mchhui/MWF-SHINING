package com.modularwarfare.client.model;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.AttachmentRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.objloader.MWModelBase;
import com.modularwarfare.client.objloader.api.ObjModelLoader;
import com.modularwarfare.common.type.BaseType;

import mchhui.hegltf.GltfLoadPriority;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ModelAttachment extends MWModelBase {

    public static final String NODE_SCOPE_POINT = "mwf_scope_point";
    public static final String NODE_FLASH_POINT = "mwf_flash_point";
    public static final String NODE_FLASHLIGHT_POINT = "mwf_flashlight_point";

    /** Parts drawn outside {@link #renderAttachment} (overlay / scope / laser). */
    private static final Set<String> GLTF_SPECIAL_PARTS;
    static {
        HashSet<String> set = new HashSet<>();
        set.add("overlayModel");
        set.add("overlaySolidModel");
        set.add("scopeModel");
        set.add("laserModel");
        GLTF_SPECIAL_PARTS = Collections.unmodifiableSet(set);
    }

    public AttachmentRenderConfig config;

    /** Non-null when model is {@code .gltf}/{@code .glb}. */
    public EnhancedModel enhancedModel;

    public float renderOffset = 0F;

    public ModelAttachment(AttachmentRenderConfig config, BaseType type) {
        this.config = config;
        String file = this.config.modelFileName == null ? "" : this.config.modelFileName;
        String lower = file.toLowerCase();
        if (lower.endsWith(".obj")) {
            if (type.isInDirectory) {
                this.staticModel = ObjModelLoader.load(type.contentPack + "/obj/" + type.getAssetDir() + "/" + file);
            } else {
                this.staticModel = ObjModelLoader.load(type, "obj/" + type.getAssetDir() + "/" + file);
            }
        } else if (lower.endsWith(".gltf") || lower.endsWith(".glb")) {
            EnhancedRenderConfig erc = new EnhancedRenderConfig(file, 24);
            this.enhancedModel = new EnhancedModel(erc, type);
        } else if (!file.isEmpty()) {
            ModularWarfare.LOGGER.info("Internal error: " + file + " is not a valid attachment model format.");
        }
    }

    public boolean isGltf() {
        return enhancedModel != null;
    }

    public void ensureGltfReady() {
        if (enhancedModel == null) {
            return;
        }
        enhancedModel.ensureRequested(GltfLoadPriority.HIGH);
        if (enhancedModel.isAnimReady() && !enhancedModel.initCal) {
            enhancedModel.updateAnimation(0f, true);
        }
    }

    public boolean existPart(String part) {
        if (!isGltf() || part == null || part.isEmpty()) {
            return false;
        }
        ensureGltfReady();
        return enhancedModel.existPart(part);
    }

    public Matrix4f getGlobalTransform(String name) {
        if (!isGltf()) {
            return new Matrix4f();
        }
        ensureGltfReady();
        return enhancedModel.getGlobalTransform(name);
    }

    public void applyGlobalTransformToOther(String binding, Runnable run) {
        if (!isGltf()) {
            if (run != null) {
                run.run();
            }
            return;
        }
        ensureGltfReady();
        enhancedModel.applyGlobalTransformToOther(binding, run);
    }

    public void renderAttachment(float f) {
        if (isGltf()) {
            ensureGltfReady();
            if (enhancedModel.initCal && enhancedModel.isAnimReady()) {
                if (enhancedModel.existPart("attachmentModel")) {
                    enhancedModel.renderPart("attachmentModel");
                } else {
                    enhancedModel.renderPartExcept(new HashSet<>(GLTF_SPECIAL_PARTS));
                }
            }
            return;
        }
        renderPart("attachmentModel", f);
    }

    public void renderScope(float f) {
        if (isGltf()) {
            renderGltfPart("scopeModel");
            return;
        }
        renderPart("scopeModel", f);
    }

    public void renderOverlay(float f) {
        if (isGltf()) {
            renderGltfPart("overlayModel");
            return;
        }
        renderPart("overlayModel", f);
    }

    public void renderOverlaySolid(float f) {
        if (isGltf()) {
            renderGltfPart("overlaySolidModel");
            return;
        }
        renderPart("overlaySolidModel", f);
    }

    public void renderLaser(float f) {
        if (isGltf()) {
            renderGltfPart("laserModel");
            return;
        }
        renderPart("laserModel", f);
    }

    private void renderGltfPart(String part) {
        ensureGltfReady();
        if (enhancedModel != null && enhancedModel.initCal && enhancedModel.existPart(part)) {
            enhancedModel.renderPart(part);
        }
    }

}

