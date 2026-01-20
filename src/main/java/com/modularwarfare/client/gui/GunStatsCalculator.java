package com.modularwarfare.client.gui;

import com.modularwarfare.ModConfig;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.client.model.ModelGun;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.common.guns.AttachmentType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAttachment;
import com.modularwarfare.common.guns.WeaponAnimationType;

import net.minecraft.item.ItemStack;

public class GunStatsCalculator {
    
    public static class GunStats {
        public float damage;
        public float headshotBonus;
        public float accuracy;
        public float recoilPitch;
        public float recoilYaw;
        public float fireRate;
        public float aimSpeed;
        public float shakeBackwards;
        public float shakeUpwards;
        public float shakeSide;
        
        public GunStats() {}
    }
    
    public static GunStats calculateStats(ItemStack gunStack, GunType gunType) {
        GunStats stats = new GunStats();
        
        stats.damage = gunType.gunDamage;
        stats.headshotBonus = gunType.gunDamageHeadshotBonus;
        stats.accuracy = gunType.bulletSpread > 0 ? (1.0f / gunType.bulletSpread) : 1.0f;
        stats.recoilPitch = gunType.recoilPitch;
        stats.recoilYaw = gunType.recoilYaw;
        stats.fireRate = gunType.roundsPerMin;
        
        float baseAimSpeed = 0.001f;
        if (gunType.animationType == WeaponAnimationType.ENHANCED) {
            if (gunType.enhancedModel != null && gunType.enhancedModel.config != null) {
                EnhancedRenderConfig config = (EnhancedRenderConfig) gunType.enhancedModel.config;
                if (config.animations != null && config.animations.get(AnimationType.AIM) != null) {
                    baseAimSpeed = (float)(config.animations.get(AnimationType.AIM).getSpeed(config.FPS) / config.FPS);
                }
            }
        } else if (gunType.animationType == WeaponAnimationType.BASIC) {
            baseAimSpeed = 0.001f;
        }
        
        float baseShakeBackwards = 0.15f;
        float baseShakeUpwards = 1.0f;
        float baseShakeSide = 0.5f;
        
        if (gunType.animationType == WeaponAnimationType.ENHANCED) {
            if (gunType.enhancedModel != null && gunType.enhancedModel.config != null) {
                GunEnhancedRenderConfig config = (GunEnhancedRenderConfig) gunType.enhancedModel.config;
                baseShakeBackwards = config.extra.modelRecoilBackwards;
                baseShakeUpwards = config.extra.modelRecoilUpwards;
                baseShakeSide = config.extra.modelRecoilShake;
            }
        } else if (gunType.animationType == WeaponAnimationType.BASIC) {
            if (gunType.model != null) {
                ModelGun modelGun = (ModelGun) gunType.model;
                if (modelGun.config != null) {
                    baseShakeBackwards = modelGun.config.extra.modelRecoilBackwards;
                    baseShakeUpwards = modelGun.config.extra.modelRecoilUpwards;
                    baseShakeSide = modelGun.config.extra.modelRecoilShake;
                }
            }
        }
        stats.aimSpeed = baseAimSpeed;
        
        float recoilPitchFactor = 1.0f;
        float recoilYawFactor = 1.0f;
        float accuracyFactor = 1.0f;
        float aimSpeedFactor = 1.0f;
        float shakeBackwardsFactor = 1.0f;
        float shakeUpwardsFactor = 1.0f;
        float shakeSideFactor = 1.0f;
        
        for (AttachmentPresetEnum attachmentType : AttachmentPresetEnum.values()) {
            ItemStack attachmentStack = GunType.getAttachment(gunStack, attachmentType);
            if (attachmentStack != null && !attachmentStack.isEmpty() && 
                attachmentStack.getItem() instanceof ItemAttachment) {
                
                ItemAttachment itemAttachment = (ItemAttachment) attachmentStack.getItem();
                AttachmentType type = itemAttachment.type;
                
                switch (attachmentType) {
                    case Barrel:
                        recoilPitchFactor *= type.barrel.recoilPitchFactor;
                        recoilYawFactor *= type.barrel.recoilYawFactor;
                        accuracyFactor *= type.barrel.accuracyFactor;
                        break;
                    case Grip:
                        recoilPitchFactor *= type.grip.recoilPitchFactor;
                        recoilYawFactor *= type.grip.recoilYawFactor;
                        break;
                    case Stock:
                        recoilPitchFactor *= type.stock.recoilPitchFactor;
                        recoilYawFactor *= type.stock.recoilYawFactor;
                        aimSpeedFactor *= type.stock.aimSpeedFactor;
                        if (type.model != null) {
                            ModelAttachment modelAttachment = (ModelAttachment) type.model;
                            if (modelAttachment.config != null && modelAttachment.config.stock != null) {
                                shakeBackwardsFactor *= modelAttachment.config.stock.modelRecoilBackwardsFactor;
                                shakeUpwardsFactor *= modelAttachment.config.stock.modelRecoilUpwardsFactor;
                                shakeSideFactor *= modelAttachment.config.stock.modelRecoilShakeFactor;
                            }
                        }
                        break;
                    case Laser:
                        accuracyFactor *= type.laser.accuracyFactor;
                        aimSpeedFactor *= type.laser.aimSpeedFactor;
                        recoilPitchFactor *= type.laser.recoilPitchFactor;
                        recoilYawFactor *= type.laser.recoilYawFactor;
                        break;
                    case Sight:
                        aimSpeedFactor *= type.sight.aimSpeedFactor;
                        break;
                    case Pistolgrip:
                        aimSpeedFactor *= type.pistolgrip.aimSpeedFactor;
                        recoilPitchFactor *= type.pistolgrip.recoilPitchFactor;
                        recoilYawFactor *= type.pistolgrip.recoilYawFactor;
                        if (type.model != null) {
                            ModelAttachment modelAttachment = (ModelAttachment) type.model;
                            if (modelAttachment.config != null && modelAttachment.config.pistolgrip != null) {
                                shakeBackwardsFactor *= modelAttachment.config.pistolgrip.modelRecoilBackwardsFactor;
                                shakeUpwardsFactor *= modelAttachment.config.pistolgrip.modelRecoilUpwardsFactor;
                                shakeSideFactor *= modelAttachment.config.pistolgrip.modelRecoilShakeFactor;
                            }
                        }
                        break;
                    case Handguard:
                        aimSpeedFactor *= type.handguard.aimSpeedFactor;
                        recoilPitchFactor *= type.handguard.recoilPitchFactor;
                        recoilYawFactor *= type.handguard.recoilYawFactor;
                        if (type.model != null) {
                            ModelAttachment modelAttachment = (ModelAttachment) type.model;
                            if (modelAttachment.config != null && modelAttachment.config.handguard != null) {
                                shakeBackwardsFactor *= modelAttachment.config.handguard.modelRecoilBackwardsFactor;
                                shakeUpwardsFactor *= modelAttachment.config.handguard.modelRecoilUpwardsFactor;
                                shakeSideFactor *= modelAttachment.config.handguard.modelRecoilShakeFactor;
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        
        stats.recoilPitch *= recoilPitchFactor;
        stats.recoilYaw *= recoilYawFactor;
        stats.accuracy /= accuracyFactor;
        stats.aimSpeed *= aimSpeedFactor;
        stats.shakeBackwards = baseShakeBackwards * shakeBackwardsFactor;
        stats.shakeUpwards = baseShakeUpwards * shakeUpwardsFactor;
        stats.shakeSide = baseShakeSide * shakeSideFactor;
        
        return stats;
    }
    
    public static float getStatPercentage(float value, float maxRef, boolean inverse) {
        if (inverse) {
            float percentage = 1.0f - Math.min(value / maxRef, 1.0f);
            return Math.max(0.0f, Math.min(1.0f, percentage));
        } else {
            float percentage = Math.min(value / maxRef, 1.0f);
            return Math.max(0.0f, Math.min(1.0f, percentage));
        }
    }
    
    public static float getMaxDamageReference() {
        return ModConfig.INSTANCE != null ? 
            ModConfig.INSTANCE.gunStats.maxDamageReference : 20.0f;
    }
    
    public static float getMaxHeadshotBonusReference() {
        return ModConfig.INSTANCE != null ? 
            ModConfig.INSTANCE.gunStats.maxHeadshotBonusReference : 10.0f;
    }
}
