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

    public HashMap<AnimationType, Animation> animations = new HashMap<>();
    public HashMap<String, ObjectControl> objectControl = new HashMap<>();
    
    public GunEnhancedRenderConfig.Global global = new GunEnhancedRenderConfig.Global();
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

    public GunEnhancedRenderConfig.ThirdPerson thirdPerson = new GunEnhancedRenderConfig.ThirdPerson();
    
   

    public static class Transform{
        public Vector3f translate = new Vector3f(0, 0, 0);
        public Vector3f scale = new Vector3f(1, 1, 1);
        public Vector3f rotate = new Vector3f(0, 0, 0);
    }
    
    public static class ObjectControl extends Transform{
        public boolean progress;
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
    
    public static class Attachment extends Transform {
        public String binding = "gunModel";
        public Vector3f sightAimPosOffset = new Vector3f(0F, 0F, 0F);
        public Vector3f sightAimRotOffset = new Vector3f(0F, 0F, 0F);
        public ArrayList<Transform> multiMagazineTransform;
        public HashSet<String> hidePart=new HashSet<String>();
        public HashSet<String> showPart=new HashSet<String>();
        public boolean renderInsideSightModel=false;
        public float renderInsideGunOffset=5;
        public Vector3f attachmentGuiOffset = new Vector3f(0F, 0F, 0F);
        public Vector3f flashModelOffset = new Vector3f(0F, 0F, 0F);
    }
    
    public static class AttachmentGroup extends Transform {
        public HashSet<String> hidePart=new HashSet<String>();
        public HashSet<String> showPart=new HashSet<String>();
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

        public HashMap<String, RenderElement> renderElements=new HashMap<String, GunEnhancedRenderConfig.ThirdPerson.RenderElement>(){{
            put(RenderType.PLAYER.serializedName, new RenderElement());
            put(RenderType.PLAYER_OFFHAND.serializedName, new RenderElement());
            put(RenderType.ITEMLOOT.serializedName, new RenderElement());
            put(RenderType.ITEMFRAME.serializedName, new RenderElement());
        }};

        
        
    }
    
    public static class SpecialEffect{
        //是否启用对传统的FlashModel对象的渲染
        public boolean oldFlashModel=true;
        
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
        
        public DynamicTextureConfig panelAmmo;
        public HashMap<Integer, DynamicTextureConfig> panelSpecialAmmo;
        public DynamicTextureConfig panelLogo;
        public DynamicTextureConfig panelReload;

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
        public float modelGuiScale=1f;
        public Vector2f modelGuiRotateCenter=new Vector2f(0,0);
        
        public float bobbingFactor=1;
        
        /**
         * shell offset
         * */
        public float shellYawOffset;
        public float shellPitchOffset;
        public float shellForwardOffset;
        
        public void preloadDynamicTexture() {
            ModularWarfare.preloadTasklist.add(()->{
                ArrayList<DynamicTextureConfig> list=new ArrayList<GunEnhancedRenderConfig.Extra.DynamicTextureConfig>();
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
