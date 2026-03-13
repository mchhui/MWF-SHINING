package com.modularwarfare.client.fpp.enhanced.configs;

import com.google.gson.annotations.SerializedName;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.GunRenderConfig;
import com.modularwarfare.client.fpp.enhanced.AnimationType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GunEnhancedRenderConfig  extends EnhancedRenderConfig {
    public HashMap<String, EnhancedRenderConfig.ObjectControl> objectControl = new HashMap<>();
    
    public GunEnhancedRenderConfig.Sprint sprint = new GunEnhancedRenderConfig.Sprint();
    public GunEnhancedRenderConfig.Aim aim = new GunEnhancedRenderConfig.Aim();
    public GunEnhancedRenderConfig.SpecialEffect specialEffect = new GunEnhancedRenderConfig.SpecialEffect();
    public GunEnhancedRenderConfig.Extra extra = new GunEnhancedRenderConfig.Extra();
    public HashMap<String, Attachment> attachment=new HashMap<String, GunEnhancedRenderConfig.Attachment>();
    public HashMap<String, AttachmentGroup> attachmentGroup=new HashMap<String, GunEnhancedRenderConfig.AttachmentGroup>();
    public HashSet<String> defaultHidePart=new HashSet<String>();
    public HashSet<String> thirdHidePart=new HashSet<String>();
    public HashSet<String> thirdShowPart=new HashSet<String>();
    
    public boolean renderOffhandPart=false;
    public HashSet<String> thirdHideOffhandPart=new HashSet<String>();
    public HashSet<String> thirdShowOffhandPart=new HashSet<String>();
    
   

    public static class Sprint {
        public Vector3f sprintRotate = new Vector3f(-20.0F, 30.0F, -0.0F);
        public Vector3f sprintTranslate = new Vector3f(0.5F, -0.10F, -0.65F);
    }

    public static class Aim {

        //Advanced configuration - Allows you to change how the gun is held without effecting the sight alignment
        public Vector3f rotateHipPosition = new Vector3f(0F, 0F, 0F);
        //Advanced configuration - Allows you to change how the gun is held without effecting the sight alignment
        public Vector3f translateHipPosition = new Vector3f(0F, 0F, 0F);
        //Advanced configuration - Allows you to change how the gun is held while aiming
        public Vector3f rotateAimPosition = new Vector3f(0F, 0F, 0F);
        //Advanced configuration - Allows you to change how the gun is held while aiming
        public Vector3f translateAimPosition = new Vector3f(0F, 0F, 0F);
    }
    
    public static class Attachment extends EnhancedRenderConfig.Transform {
        public String binding = "gunModel";
        public Vector3f sightAimPosOffset = new Vector3f(0F, 0F, 0F);
        public Vector3f sightAimRotOffset = new Vector3f(0F, 0F, 0F);
        public ArrayList<EnhancedRenderConfig.Transform> multiMagazineTransform;
        public HashSet<String> hidePart=new HashSet<String>();
        public HashSet<String> showPart=new HashSet<String>();
        public boolean renderInsideSightModel=false;
        public float renderInsideGunOffset=5f;
        public Vector3f attachmentGuiOffset = new Vector3f(0F, 0F, 0F);
        public Vector3f flashModelOffset = new Vector3f(0F, 0F, 0F);
        public Boolean rotateFlashModel=null;
        public float modelRecoilBackwardsFactor = 1f;
        public float modelRecoilUpwardsFactor = 1f;
        public float modelRecoilShakeFactor = 1f;
        public HashMap<String, HandguardInfluence> handguardInfluence = new HashMap<String, HandguardInfluence>();
    }
    
    public static class AttachmentGroup extends EnhancedRenderConfig.Transform {
        public HashSet<String> hidePart=new HashSet<String>();
        public HashSet<String> showPart=new HashSet<String>();
        public HashMap<String, HandguardInfluence> handguardInfluence = new HashMap<String, HandguardInfluence>();
    }
    
    public static class HandguardInfluence extends EnhancedRenderConfig.Transform {}

    public static class SpecialEffect{
        //是否启用对传统的FlashModel对象的渲染
        public boolean oldFlashModel=true;
        
        //是否启用火焰模型X轴随机旋转
        public boolean rotateFlashModel=false;
        
        //枪口过热烟的系数 这个数越大 烟越容易出现
        public float postSmokeFactor=1;
        
        //填入该组的对象会被当作FlashModel对象渲染
        public ArrayList<FlashModelGroup> flashModelGroups=new ArrayList<GunEnhancedRenderConfig.SpecialEffect.FlashModelGroup>();
        
        //填入该组的对象会成为枪口过热烟的绑定点
        public ArrayList<PostSmokeGroup> postSmokeGroups=new ArrayList<GunEnhancedRenderConfig.SpecialEffect.PostSmokeGroup>();
        
        //填入该组的对象会成为抛壳口的绑定点
        public ArrayList<EjectionGroup> ejectionGroups=new ArrayList<GunEnhancedRenderConfig.SpecialEffect.EjectionGroup>();
        
        // X正左负右,Y正下负上,Z正前负后
        public Vector3f firstPersonShellEjectPos=new Vector3f(0, 0f, -0.2f);
        public Vector3f thirdPersonShellEjectPos;
        
        public static class FlashModelGroup{
            public String name;
        }
        
        public static class PostSmokeGroup{
            public String name;
        }
        
        public static class EjectionGroup{
            public String name;
            public float throwShellFrame;
            public Vector3f throwShellMaxForce;
            
            public boolean ejectSmoke;
            public Vector3f ejectSmokeForce;
        }
    }

    public static class Extra {
        
        public EnhancedRenderConfig.DynamicTextureConfig panelAmmo;
        public HashMap<Integer, EnhancedRenderConfig.DynamicTextureConfig> panelSpecialAmmo;
        public EnhancedRenderConfig.DynamicTextureConfig panelLogo;
        public EnhancedRenderConfig.DynamicTextureConfig panelReload;
        public EnhancedRenderConfig.DynamicTextureConfig panelInspect;

        /**
         * Adds backwards recoil translations to the gun staticModel when firing
         */
        public float modelRecoilBackwards = 0.15F;
        /**
         * Adds upwards/downwards recoil translations to the gun staticModel when firing
         */
        public float modelRecoilUpwards = 1.0F;
        /**
         * Adds a left-right staticModel shaking motion when firing, default 0.5
         */
        public float modelRecoilShake = 0.5F;
        /**
         * 瞄准时抖动影响额外参数
         */
        public float modelRecoilBackwardsADSFactor = 1.0f;
        public float modelRecoilUpwardsADSFactor = 1.0f;
        public float modelRecoilShakeADSFactor = 1.0f;

        public float modelGuiScale=1f;
        public Vector2f modelGuiRotateCenter=new Vector2f(0,0);
        
        public float bobbingFactor=1.0f;
        
        /**
         * shell offset
         * */
        public float shellYawOffset;
        public float shellPitchOffset;
        public float shellForwardOffset;
        
        public void preloadDynamicTexture() {
            ModularWarfare.preloadTasklist.add(()->{
                ArrayList<EnhancedRenderConfig.DynamicTextureConfig> list=new ArrayList<EnhancedRenderConfig.DynamicTextureConfig>();
                list.add(panelAmmo);
                list.add(panelLogo);
                list.add(panelReload);
                if(panelSpecialAmmo!=null) {
                    panelSpecialAmmo.values().forEach((v)->{
                        list.add(v);
                    });  
                }
                list.forEach((tex)->{
                    if(tex==null) {
                        return;
                    }
                    for(int i=0;i<tex.frameCount;i++) {
                        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "panel/"+tex.texhead+i+".png"));  
                        if(tex.linear) {
                            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                        }else {
                            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                        }
                    }
                });
            });
        }
    }
}
