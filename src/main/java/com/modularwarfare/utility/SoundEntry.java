package com.modularwarfare.utility;

import com.modularwarfare.common.guns.WeaponSoundType;

public class SoundEntry {

    public WeaponSoundType soundEvent;
    public String soundName;
    public int soundDelay = 0;
    public float soundVolumeMultiplier = 1f;
    public float soundFarVolumeMultiplier = 1f;
    public float soundPitch = 1f;
    public float soundRandomPitch = 0.2f;
    public float emptyPitch = 0.05f;
    public Integer soundRange = 16;

    public String soundNameDistant;
    public Integer soundMaxRange;

}