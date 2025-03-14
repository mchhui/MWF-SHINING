package com.modularwarfare.common.grenades;

import com.modularwarfare.common.guns.PotionEntry;

public class GrenadeGasType {
    // GAS爆炸配置
    public boolean hasFirstExplosion = false;
    public float explosionDelay = 1.0f;
    public int explosionCount = 5;
    public float explosionInterval = 1.0f;
    
    // GAS爆炸效果参数
    public float explosionDamage = 10f;
    public float explosionRange = 3f;
    public float explosionKnockback = 0.5f;
    public boolean damageWorld = false;
    public boolean allowBlockDrops = false;
    public boolean throwerVulnerable = false;
    public boolean explosionThroughWalls = false;
    public PotionEntry[] explosionPotionEffects;
    public int explosionFireLevel = 0;
    public float explosionKnockLevel = 0;
    public boolean causesFire = false;
} 