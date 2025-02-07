package com.modularwarfare.client.fpp.enhanced.configs;

import java.util.HashMap;

import org.lwjgl.util.vector.Vector3f;

import com.google.gson.annotations.SerializedName;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ShowHandArmorType;

public class EnhancedRenderConfig {

    public String modelFileName = "";
    public int FPS=24;
    
    public ShowHandArmorType showHandArmorType=ShowHandArmorType.NONE;
    
    public HashMap<AnimationType, EnhancedRenderConfig.Animation> animations = new HashMap<>();
    public EnhancedRenderConfig.Global global = new EnhancedRenderConfig.Global();
    public EnhancedRenderConfig.ThirdPerson thirdPerson = new EnhancedRenderConfig.ThirdPerson();
    
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

    public static class Transform{
        public Vector3f translate = new Vector3f(0, 0, 0);
        public Vector3f scale = new Vector3f(1, 1, 1);
        public Vector3f rotate = new Vector3f(0, 0, 0);
    }

    public static class Animation {
        public double startTime = 0;
        public double endTime = 1;
        public double speed = 1;
    
        public double getStartTime(double FPS) {
            return startTime * 1/FPS;
        }
    
        public double getEndTime(double FPS) {
            return endTime * 1/FPS;
        }
        
        public double getSpeed(double FPS) {
            double a=(getEndTime(FPS)-getStartTime(FPS));
            if(a<=0) {
                a=1;
            }
            return speed/a;
        }
    }

    //1.translate 3.scale 2.rotate(yxz)
    public static class Global {
        public Vector3f globalTranslate = new Vector3f(0, 0, 0);
        //注:这并不会让你的枪看起来变大或变小 但是会影响剪裁空间
        public Vector3f globalScale = new Vector3f(1, 1, 1);
        public Vector3f globalRotate = new Vector3f(0, 0, 0);
    }

    public static class ThirdPerson {
    
        public static class RenderElement {
            public Vector3f pos = new Vector3f(0F, 0F, 0F);
            public Vector3f rot = new Vector3f(0F, 0F, 0F);
            public Vector3f size = new Vector3f(1F, 1F, 1F);
            
            /**
             * loot only
             * */
            public boolean randomYaw=false;
        }
    
        public HashMap<String, RenderElement> renderElements=new HashMap<String, EnhancedRenderConfig.ThirdPerson.RenderElement>(){{
            put(RenderType.PLAYER.serializedName, new RenderElement());
            put(RenderType.PLAYER_OFFHAND.serializedName, new RenderElement());
            put(RenderType.ITEMLOOT.serializedName, new RenderElement());
            put(RenderType.ITEMFRAME.serializedName, new RenderElement());
            put(RenderType.GRENADE.serializedName, new RenderElement());
        }};
    
        
        
    }

    public static class ObjectControl extends Transform{
        public boolean progress;
    }

    public static class DynamicTextureConfig{
        public String texhead;
        /**
         * 序列图后缀从从0开始数 数到frameCount-1
         * 用于panelAmmo时
         * frameCount表示循环周期
         * 比如当前弹量为4
         * frameCount为3
         * 则取后缀assets/modularwarfare/panel/+texhead+1.png的图片为纹理
         * */
        public int frameCount;
        public int FPS;
        public boolean linear=false;
    }

}
