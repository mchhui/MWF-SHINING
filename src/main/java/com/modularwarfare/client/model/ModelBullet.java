package com.modularwarfare.client.model;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.BulletRenderConfig;
import com.modularwarfare.client.objloader.MWModelBase;
import com.modularwarfare.client.objloader.api.ObjModelLoader;
import com.modularwarfare.common.type.BaseType;

public class ModelBullet extends MWModelBase {

    public BulletRenderConfig config;

    public ModelBullet(BulletRenderConfig config, BaseType type) {
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


    public void renderBullet(float f) {
        renderPart("bulletModel", f);
    }

    public void renderBullet(int num, float f) {
        for (int i = 1; i <= num; i++) {
            renderPart("bulletModel_" + i, f);
        }
    }


}
