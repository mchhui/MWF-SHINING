package com.modularwarfare.common.grenades;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.GrenadeRenderConfig;
import com.modularwarfare.client.fpp.basic.configs.GunRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GrenadeEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGrenade;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.model.ModelGrenade;
import com.modularwarfare.client.model.ModelGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.type.BaseType;

import net.minecraftforge.fml.common.FMLCommonHandler;

public class GrenadeType extends BaseType {

    public WeaponAnimationType animationType = WeaponAnimationType.BASIC;
    
    public GrenadesEnumType grenadeType = GrenadesEnumType.Frag;
    
    public float fuseTime = 5.0f;
    public boolean damageWorld = false;
    public float explosionDamage = 30f;
    public float explosionRange = 6f;
    public float explosionKnockback = 1f;
    public float throwStrength = 1f;
    public boolean throwerVulnerable = false;

    public float smokeTime = 10f;

    @Override
    public void loadExtraValues() {
        if (maxStackSize == null)
            maxStackSize = 1;
        loadBaseValues();
    }

    @Override
    public void reloadModel() {
        try {
            if (animationType == WeaponAnimationType.BASIC) {
                model = new ModelGrenade(ModularWarfare.getRenderConfig(this, GrenadeRenderConfig.class), this);
            } else {
                enhancedModel = new ModelEnhancedGrenade(ModularWarfare.getRenderConfig(this, GrenadeEnhancedRenderConfig.class), this);
            }  
        }catch(Throwable t) {
            ModularWarfare.LOGGER.warn("Something is going wrong when reloading model:"+internalName);
            t.printStackTrace();
            FMLCommonHandler.instance().exitJava(0, false);
        }
    }

    @Override
    public String getAssetDir() {
        return "grenades";
    }

}
