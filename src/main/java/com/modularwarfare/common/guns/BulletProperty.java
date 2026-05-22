package com.modularwarfare.common.guns;

public class BulletProperty {

    /**
     * Damage inflicted per bullet. Multiplied by the gun damage value.
     */
    
    public float bulletDamageFactor = 1.0f;
    public PotionEntry[] potionEffects;
    public int fireLevel=0;
    public float explosionLevel=0;
    public boolean explosionBroken=false;
    public boolean explosionOnBlock = false;
    public float knockLevel=0;
    public Float knockVerticalLevel = null;
    public boolean banShield=false;

}
