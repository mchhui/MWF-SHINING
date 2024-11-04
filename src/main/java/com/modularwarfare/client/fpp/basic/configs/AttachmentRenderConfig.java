package com.modularwarfare.client.fpp.basic.configs;


import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.WeaponSoundType;
import com.modularwarfare.objects.SoundEntry;
import org.lwjgl.util.vector.Vector3f;

import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;

public class AttachmentRenderConfig {

    public String modelFileName = "";

    public AttachmentRenderConfig.Extra extra = new AttachmentRenderConfig.Extra();

    public AttachmentRenderConfig.Sight sight = new AttachmentRenderConfig.Sight();
    public AttachmentRenderConfig.Grip grip = new AttachmentRenderConfig.Grip();
    public AttachmentRenderConfig.Stock stock = new AttachmentRenderConfig.Stock();

    public static class Extra {

        public float modelScale = 1.0f;
    }

    public static class Sight {
        //cache
        private transient float[] stageFovZoomRange = null;


        //倍率切换(滚轮切换) 默认关闭 可以指定初始倍率
        public float[] fovZoomStage = null;
        public int fovZoomStageIndex = 0;


        //无极倍率(滚轮切换) 默认关闭
        public float fovZoomMin = -1F;
        public float fovZoomMax = -1F;

        public float fovZoom = 1f;

        public float mouseSensitivityFactor = 1.0f;
        public float rectileScale = 1.0f;
        
        public float factorCrossScale = 0.2f;
        public String maskTexture="default_mask";
        public float maskSize=0.75f;
        public float uniformMaskRange=0.1f;
        public float uniformDrawRange=245f/1600;
        public float uniformStrength=3f;
        public float uniformScaleRangeY=1f;
        public float uniformScaleStrengthY=1f;
        public float uniformVerticality=0f;

        public float[] GetStageFovZoomRange()
        {
            if(stageFovZoomRange!=null)return stageFovZoomRange;
            float max = Float.MIN_VALUE , min = Float.MAX_VALUE;
            for(float v : fovZoomStage)
            {
                max = Math.max(v,max);
                min = Math.min(min,v);
            }
            stageFovZoomRange = new float[]{min,max};
            return stageFovZoomRange;
        }

    }

    public static class Grip {
        public Vector3f leftArmOffset = new Vector3f(0F, 0F, 0F);
    }
    
    public static class Stock {
        public float modelRecoilBackwardsFactor = 1.0f;
        public float modelRecoilUpwardsFactor = 1.0f;
        public float modelRecoilShakeFactor = 1.0f;
    }

    
    public void init() {
        //sight.maskTextureLocation=new ResourceLocation(sight.maskTexture);
    }
}