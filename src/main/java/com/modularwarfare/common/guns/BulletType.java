package com.modularwarfare.common.guns;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.BulletRenderConfig;
import com.modularwarfare.client.model.ModelBullet;
import com.modularwarfare.client.model.ModelShell;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.loader.MWModelBase;

import java.util.HashMap;

public class BulletType extends BaseType {

    // Bullet Type
    public HashMap<String, BulletProperty> bulletProperties;

    public float bulletDamageFactor = 1.0f;
    public float bulletAccuracyFactor = 1.0f;
    public float bulletPenetrateFactor = 1.0f;
    public float bulletBlockPenetrateFactor = 1.0f;
    public boolean isSlug = false;
    
    public boolean isFireDamage=false;
    public boolean isAbsoluteDamage=false;
    public boolean isBypassesArmorDamage=false;
    public boolean isExplosionDamage=false;
    public boolean isMagicDamage=false;

    /**
     * If the ammo model need to be rendered on guns
     */
    public boolean renderBulletModel = false;

    public transient String defaultModel = "default.obj";
    public String shellModelFileName = defaultModel;
    public transient MWModelBase shell;

    public String shellSound = "casing";
    
    /**
     * Explosives type only for launcher
     */
    public float impactDamage = 0.5f;
    public float projectileVelocity= 3f;
    public float explosionDamage = 30f;
    public float explosionRange = 6f;
    public float explosionKnockback = 1f;
    public float gravity = -0.01f;
    public boolean isSmoke = true;
    public boolean isExplosion = true;
    public boolean damageWorld = true;
    public boolean allowBlockDrops = true;
    
    public boolean isDynamicBullet=false;
    public boolean sameTextureAsGun=false;

    public String customExplosionModel;
    public String customExplosionTexture;
    
    /**
     * Custom Bullet Trail
     * */
    public String trailModel;
    public String trailTex;
    //trailModel trailTex至少有一个不为null trailGlow才生效
    public boolean trailGlow=false;
    
    /** 粒子特效的生命周期（单位：tick） */
    public int particleMaxAge = 110;

    @Override
    public void loadExtraValues() {
        if (maxStackSize == null)
            maxStackSize = 64;

        loadBaseValues();
    }

    @Override
    public void reloadModel() {
        if (renderBulletModel) {
            model = new ModelBullet(ModularWarfare.getRenderConfig(this, BulletRenderConfig.class), this);
        }
        if (!shellModelFileName.equals(defaultModel)) {
            shell = new ModelShell(this, false);
        } else {
            shell = new ModelShell(this, true);
        }
    }

    @Override
    public String getAssetDir() {
        return "bullets";
    }

}
