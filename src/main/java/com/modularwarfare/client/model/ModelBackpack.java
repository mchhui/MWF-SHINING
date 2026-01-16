package com.modularwarfare.client.model;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.BackpackRenderConfig;
import com.modularwarfare.client.objloader.MWModelBase;
import com.modularwarfare.client.objloader.api.ObjModelLoader;
import com.modularwarfare.client.objloader.api.model.ObjModelRenderer;
import com.modularwarfare.common.type.BaseType;

import net.minecraft.client.renderer.GlStateManager;

public class ModelBackpack extends MWModelBase {

    public BackpackRenderConfig config;


    public ModelBackpack(BackpackRenderConfig config, BaseType type) {
        this.config = config;
        if (this.config.modelFileName.endsWith(".obj")) {
            if (type.isInDirectory) {
                this.staticModel = ObjModelLoader.load(type.contentPack + "/obj/" + type.getAssetDir() + "/" + this.config.modelFileName);
            } else {
                this.staticModel = ObjModelLoader.load(type, "obj/" + type.getAssetDir() + "/" + this.config.modelFileName);
            }
        } else {
            ModularWarfare.LOGGER.info("Internal error: " + this.config.modelFileName + " is not a valid format.");
        }
    }

    public void render(String modelPart, float scale, float modelScale) {
        GlStateManager.pushMatrix();

        ObjModelRenderer part = this.staticModel.getPart(modelPart);
        if (part != null) {
            if (part != null) {
                part.render(scale * modelScale);
            }
        }
        GlStateManager.popMatrix();

    }

}