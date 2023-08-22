package com.modularwarfare.client.fpp.enhanced.configs;

import com.google.gson.annotations.SerializedName;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ShowHandArmorType;

public class EnhancedRenderConfig {

    public String modelFileName = "";
    public int FPS=24;
    
    public ShowHandArmorType showHandArmorType=ShowHandArmorType.NONE;
    
    public EnhancedRenderConfig() {
        // TODO Auto-generated constructor stub
    }

    public EnhancedRenderConfig(String modelFileName, int fPS) {
        this.modelFileName = modelFileName;
        FPS = fPS;
    }
    
    public static enum ShowHandArmorType{
        @SerializedName("none")NONE,
        @SerializedName("static")STATIC,
        @SerializedName("skin")SKIN
    }

}
