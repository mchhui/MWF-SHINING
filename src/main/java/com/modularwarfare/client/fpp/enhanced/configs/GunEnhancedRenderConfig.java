package com.modularwarfare.client.fpp.enhanced.configs;

import com.modularwarfare.client.fpp.enhanced.AnimationType;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GunEnhancedRenderConfig extends EnhancedRenderConfig {

    public HashMap<AnimationType, Animation> animations = new HashMap<>();
    public HashMap<String, ObjectControl> objectControl = new HashMap<>();

    public GunEnhancedRenderConfig.Global global = new GunEnhancedRenderConfig.Global();
    public GunEnhancedRenderConfig.Sprint sprint = new GunEnhancedRenderConfig.Sprint();
    public GunEnhancedRenderConfig.Aim aim = new GunEnhancedRenderConfig.Aim();
    public GunEnhancedRenderConfig.Extra extra = new GunEnhancedRenderConfig.Extra();
    public HashMap<String, Attachment> attachment = new HashMap<>();
    public HashMap<String, AttachmentGroup> attachmentGroup = new HashMap<>();
    public HashSet<String> defaultHidePart = new HashSet<>();
    public HashSet<String> thirdHidePart = new HashSet<>();
    public HashSet<String> thirdShowPart = new HashSet<>();

    public GunEnhancedRenderConfig.ThirdPerson thirdPerson = new GunEnhancedRenderConfig.ThirdPerson();


    public static class Transform {
        public Vector3f translate = new Vector3f(0, 0, 0);
        public Vector3f scale = new Vector3f(1, 1, 1);
        public Vector3f rotate = new Vector3f(0, 0, 0);
    }

    public static class ObjectControl extends Transform {
        public boolean progress;
    }

    public static class Animation {
        public double startTime = 0;
        public double endTime = 1;
        public double speed = 1;

        public double getStartTime(double fps) {
            return startTime * 1 / fps;
        }

        public double getEndTime(double fps) {
            return endTime * 1 / fps;
        }

        public double getSpeed(double fps) {
            double a = (getEndTime(fps) - getStartTime(fps));
            if (a <= 0) {
                a = 1;
            }
            return speed / a;
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
        public HashSet<String> hidePart = new HashSet<String>();
        public HashSet<String> showPart = new HashSet<String>();
        public boolean renderInsideSightModel = false;
        public float renderInsideGunOffset = 5;
        public Vector3f attachmentGuiOffset = new Vector3f(0F, 0F, 0F);
    }

    public static class AttachmentGroup extends Transform {
        public HashSet<String> hidePart = new HashSet<String>();
        public HashSet<String> showPart = new HashSet<String>();
    }

    public static class ThirdPerson {

        public HashMap<String, RenderElement> renderElements = new HashMap<String, GunEnhancedRenderConfig.ThirdPerson.RenderElement>() {{
            put(RenderType.PLAYER.serializedName, new RenderElement());
            put(RenderType.ITEMLOOT.serializedName, new RenderElement());
            put(RenderType.ITEMFRAME.serializedName, new RenderElement());
        }};

        public static class RenderElement {
            public Vector3f pos = new Vector3f(0F, 0F, 0F);
            public Vector3f rot = new Vector3f(0F, 0F, 0F);
            public Vector3f size = new Vector3f(1F, 1F, 1F);

            /**
             * loot only
             */
            public boolean randomYaw = false;
        }


    }

    public static class Extra {

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
    }
}
