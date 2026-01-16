package com.modularwarfare.common.melee;

import java.util.ArrayList;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.enhanced.AnimationMeleeType;
import com.modularwarfare.client.fpp.enhanced.configs.MeleeRenderConfig;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.utility.SoundEntry;

public class MeleeType extends BaseType {

    // public HashMap<AnimationMeleeType, ArrayList<AnimationInfo>> animations = new
    // HashMap<>();
    public AnimationInfo[] attack;
    public AnimationInfo[] attackHeavy;

    public float damage = 0;
    public double attackspeed = -2.4000000953674316D;
    public float moveSpeedModifier = 1;

    public static class AnimationInfo {
        public float damage = 0;
        public float range = 1;
        public float yawAngle = 0;
        public float pitchAngle = 0;
        public float yawStart = 0;
        public float pitchStart = 0;
        public int yawStep = 5;
        public int pitchStep = 5;
        public boolean canBounced = false;
        public boolean attackPenetration = false;
        public int nextPhase = -1;
        public boolean keepOrder = false;
        public CheckBounced checkBounced = null;
        public String animationName = "none";
        public String hitAnimationName = "none";

        public static class CheckBounced {
            public float range = 1;
            public float yawAngle = 1;
            public float pitchAngle = 1;
            public float yawStart = 1;
            public float pitchStart = 1;
            public String animationName = "none";
        }
    }

    public AnimationInfo getAnimationInfo(AnimationMeleeType type, int index) {
        boolean isHeavy = type.serializedName.lastIndexOf("heavy") != -1;
        AnimationInfo[] targetArray = isHeavy ? attackHeavy : attack;
        
        if (targetArray == null || index < 0 || index >= targetArray.length) {
            return null;
        }
        
        return targetArray[index];
    }

    public boolean resetAttackOnClick = false;
    public boolean resetPostOnClick = false;

    public boolean destroyBlocks = false;
    // public boolean swing = true;

    public MeleeType() {
        maxStackSize = 1;
    }

    @Override
    public void loadExtraValues() {
        maxStackSize = 1;

        loadBaseValues();
        try {
            for (ArrayList<SoundEntry> entryList : weaponSoundMap.values()) {
                for (SoundEntry soundEntry : entryList) {
                    if (soundEntry.soundName != null) {
                        ModularWarfare.PROXY.registerSound(soundEntry.soundName);
                        if (soundEntry.soundNameDistant != null)
                            ModularWarfare.PROXY.registerSound(soundEntry.soundNameDistant);
                    } else {
                        ModularWarfare.LOGGER
                                .error(String.format("Sound entry event '%s' has null soundName for type '%s'",
                                        soundEntry.soundEvent, internalName));
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void reloadModel() {
        enhancedModel = new EnhancedModel(ModularWarfare.getRenderConfig(this, MeleeRenderConfig.class), this);
    }

    @Override
    public String getAssetDir() {
        return "melee";
    }
}
