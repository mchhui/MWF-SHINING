package com.modularwarfare.client.fpp.enhanced.configs;

import com.modularwarfare.client.fpp.basic.configs.GrenadeRenderConfig;
import com.modularwarfare.client.fpp.enhanced.AnimationMeleeType;

import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MeleeRenderConfig extends EnhancedRenderConfig {

    public HashMap<AnimationMeleeType, ArrayList<Animation>> meleeAnimations = new HashMap<>();
    public HashSet<String> defaultHidePart=new HashSet<String>();
    public HashSet<String> thirdHidePart=new HashSet<String>();
    public HashSet<String> thirdShowPart=new HashSet<String>();
    public MeleeRenderConfig.Extra extra = new MeleeRenderConfig.Extra();


    public static class Extra {
//        public boolean thirdPersonRender3D = true;
//        public Vector3f thirdPersonOffset = new Vector3f(0F, 0F, 0F);
//        public Vector3f thirdPersonRotation = new Vector3f(0F, 0F, 0F);
//        public float thirdPersonScale = 1.0f;
        public float bobbingFactor = 1.0f;
        public Vector3f sprintOffset = new Vector3f(0F, 0F, 0F);
        public Vector3f sprintRotation = new Vector3f(0F, 0F, 0F);
    }

    public static class Animation {
        public double startTime = 0;
        public double endTime = 1;
        public double speed = 1;

        public double getStartTime(double FPS) {
            return startTime * 1 / FPS;
        }

        public double getEndTime(double FPS) {
            return endTime * 1 / FPS;
        }

        public double getSpeed(double FPS) {
            double a = (getEndTime(FPS) - getStartTime(FPS));
            if (a <= 0) {
                a = 1;
            }
            return speed / a;
        }
    }

}
