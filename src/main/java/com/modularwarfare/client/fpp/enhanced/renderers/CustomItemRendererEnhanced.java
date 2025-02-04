 package com.modularwarfare.client.fpp.enhanced.renderers;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.UUID;

import org.lwjgl.BufferUtils;

import com.modularwarfare.api.RenderHandSleeveEnhancedEvent;
import com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreFirstLayer;
import com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreSecondLayer;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ShowHandArmorType;
import com.modularwarfare.client.fpp.enhanced.configs.GrenadeEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGrenade;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.model.ModelCustomArmor;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.WeaponAnimationType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.Timer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class CustomItemRendererEnhanced extends CustomItemRenderer{
    protected HashMap<String, EnhancedModel> thirdPersonModels = new HashMap<>();
    protected EnhancedModel firstPersonModel;
    protected static float sizeFactor = 10000f;
    protected static final float PI = 3.14159265f;
    private static Timer timer;
    protected FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
    
    protected static Timer getTimer() {
        if (timer == null) {
            timer = ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "timer", "field_71428_T");
        }
        return timer;
    }
    
    protected static float toRadians(float angdeg) {
        return angdeg / 180.0f * PI;
    }
    
    protected ModelEnhancedGun getOrCreateModel(GunType gunType, boolean isFirstPerson, UUID playerId) {
        // Added a check for the animation type
        if (isFirstPerson && gunType.animationType != WeaponAnimationType.BASIC) {
            if (firstPersonModel == null || firstPersonModel.baseType != gunType) {
                try {
                    firstPersonModel = new ModelEnhancedGun((GunEnhancedRenderConfig) gunType.enhancedModel.config, gunType);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return (ModelEnhancedGun)firstPersonModel;
        } else {
            String key = playerId != null ? playerId.toString() : "default";
            if (gunType.animationType != WeaponAnimationType.BASIC) {
                if (!thirdPersonModels.containsKey(key) || thirdPersonModels.get(key).baseType != gunType) {
                    ModelEnhancedGun newModel = new ModelEnhancedGun((GunEnhancedRenderConfig) gunType.enhancedModel.config, gunType);
                    thirdPersonModels.put(key, newModel);
                }
            }
            return (ModelEnhancedGun)thirdPersonModels.get(key);
        }
    }
    
    protected ModelEnhancedGrenade getOrCreateModel(GrenadeType greanadeType, boolean isFirstPerson, UUID playerId) {
        // Added a check for the animation type
        if (isFirstPerson && greanadeType.animationType != WeaponAnimationType.BASIC) {
            if (firstPersonModel == null || firstPersonModel.baseType != greanadeType) {
                try {
                    firstPersonModel = new ModelEnhancedGrenade((GrenadeEnhancedRenderConfig) greanadeType.enhancedModel.config, greanadeType);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return (ModelEnhancedGrenade)firstPersonModel;
        } else {
            String key = playerId != null ? playerId.toString() : "default";
            if (greanadeType.animationType != WeaponAnimationType.BASIC) {
                if (!thirdPersonModels.containsKey(key) || thirdPersonModels.get(key).baseType != greanadeType) {
                    ModelEnhancedGrenade newModel = new ModelEnhancedGrenade((GrenadeEnhancedRenderConfig) greanadeType.enhancedModel.config, greanadeType);
                    thirdPersonModels.put(key, newModel);
                }
            }
            return (ModelEnhancedGrenade)thirdPersonModels.get(key);
        }
    }

    //需要考虑实际删除模型 释放内存
    public void resetModels() {
        // 清空所有缓存的模型
        this.firstPersonModel = null;
        this.thirdPersonModels.clear();
        
        // 重置控制器
        AnimationController.resetClientController();
        AnimationController.getOtherControllers().clear();
    }
    
    public void renderHandAndArmor(EnumHandSide side, AbstractClientPlayer player, EnhancedRenderConfig config,
        ModelPlayer modelPlayer, EnhancedModel model) {
    if (side == EnumHandSide.LEFT) {
        if (config.showHandArmorType != ShowHandArmorType.NONE) {
            PreFirstLayer leftFirst = new PreFirstLayer(this, EnumHandSide.LEFT);
            PreSecondLayer leftSecond = new PreSecondLayer(this, EnumHandSide.LEFT);
            MinecraftForge.EVENT_BUS.post(leftFirst);
            MinecraftForge.EVENT_BUS.post(leftSecond);
            if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                if (!leftFirst.isCanceled()) {
                    if (modelPlayer.bipedLeftArm.showModel && !modelPlayer.bipedLeftArm.isHidden) {
                        model.renderPart("leftArmModel");
                    }
                }
                if (!leftSecond.isCanceled()) {
                    if (modelPlayer.bipedLeftArmwear.showModel && !modelPlayer.bipedLeftArmwear.isHidden) {
                        model.renderPart("leftArmLayerModel");
                    }
                }
            } else {
                if (!leftFirst.isCanceled()) {
                    if (modelPlayer.bipedLeftArm.showModel && !modelPlayer.bipedLeftArm.isHidden) {
                        model.renderPart("leftArmSlimModel");
                    }
                }
                if (!leftSecond.isCanceled()) {
                    if (modelPlayer.bipedLeftArmwear.showModel && !modelPlayer.bipedLeftArmwear.isHidden) {
                        model.renderPart("leftArmLayerSlimModel");
                    }
                }
            }
            if (player.inventory.armorItemInSlot(2) != null) {
                ItemStack armorStack = player.inventory.armorItemInSlot(2);
                if (armorStack.getItem() instanceof ItemMWArmor) {
                    int skinId = 0;
                    String path = skinId > 0
                            ? ((ItemMWArmor) armorStack.getItem()).type.modelSkins[skinId].getSkin()
                            : ((ItemMWArmor) armorStack.getItem()).type.modelSkins[0].getSkin();

                    if (!((ItemMWArmor) armorStack.getItem()).type.simpleArmor) {
                        ModelCustomArmor modelArmor = ((ModelCustomArmor) ((ItemMWArmor) armorStack
                                .getItem()).type.bipedModel);

                        bindTexture("armor", path);
                        if (modelArmor.enhancedArmModel != null) {
                            modelArmor.enhancedArmModel.loadAnimation(model,
                                    config.showHandArmorType == ShowHandArmorType.SKIN);
                            if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                                if (config.showHandArmorType == ShowHandArmorType.STATIC) {
                                    modelArmor.enhancedArmModel.renderPart("leftArmModel");
                                }
                                if (config.showHandArmorType == ShowHandArmorType.SKIN) {
                                    modelArmor.enhancedArmModel.renderPart("leftArmModel_bone");
                                }
                            } else {
                                if (config.showHandArmorType == ShowHandArmorType.STATIC) {
                                    modelArmor.enhancedArmModel.renderPart("leftArmSlimModel");
                                }
                                if (config.showHandArmorType == ShowHandArmorType.SKIN) {
                                    modelArmor.enhancedArmModel.renderPart("leftArmSlimModel_bone");
                                }
                            }
                        }
                    }
                }
            }
            MinecraftForge.EVENT_BUS.post(new RenderHandSleeveEnhancedEvent.Post(this, EnumHandSide.LEFT, model));
        } else {
            if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                model.renderPart(LEFT_HAND_PART);
            } else {
                model.renderPart(LEFT_SLIM_HAND_PART);
            }
        }
    } else {
        if (config.showHandArmorType != ShowHandArmorType.NONE) {
            PreFirstLayer rightFirst = new PreFirstLayer(this, EnumHandSide.RIGHT);
            PreSecondLayer rightSecond = new PreSecondLayer(this, EnumHandSide.RIGHT);
            MinecraftForge.EVENT_BUS.post(rightFirst);
            MinecraftForge.EVENT_BUS.post(rightSecond);
            if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                if (!rightFirst.isCanceled()) {
                    if (modelPlayer.bipedRightArm.showModel && !modelPlayer.bipedRightArm.isHidden) {
                        model.renderPart("rightArmModel");
                    }
                }
                if (!rightSecond.isCanceled()) {
                    if (modelPlayer.bipedRightArmwear.showModel && !modelPlayer.bipedRightArmwear.isHidden) {
                        model.renderPart("rightArmLayerModel");
                    }
                }
            } else {
                if (!rightFirst.isCanceled()) {
                    if (modelPlayer.bipedRightArm.showModel && !modelPlayer.bipedRightArm.isHidden) {
                        model.renderPart("rightArmSlimModel");
                    }
                }
                if (!rightSecond.isCanceled()) {
                    if (modelPlayer.bipedRightArmwear.showModel && !modelPlayer.bipedRightArmwear.isHidden) {
                        model.renderPart("rightArmLayerSlimModel");
                    }
                }
            }
            if (player.inventory.armorItemInSlot(2) != null) {
                ItemStack armorStack = player.inventory.armorItemInSlot(2);
                if (armorStack.getItem() instanceof ItemMWArmor) {
                    int skinId = 0;
                    String path = skinId > 0
                            ? ((ItemMWArmor) armorStack.getItem()).type.modelSkins[skinId].getSkin()
                            : ((ItemMWArmor) armorStack.getItem()).type.modelSkins[0].getSkin();

                    if (!((ItemMWArmor) armorStack.getItem()).type.simpleArmor) {
                        ModelCustomArmor modelArmor = ((ModelCustomArmor) ((ItemMWArmor) armorStack
                                .getItem()).type.bipedModel);

                        bindTexture("armor", path);
                        if (modelArmor.enhancedArmModel != null) {
                            modelArmor.enhancedArmModel.loadAnimation(model,
                                    config.showHandArmorType == ShowHandArmorType.SKIN);
                            if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                                if (config.showHandArmorType == ShowHandArmorType.STATIC) {
                                    modelArmor.enhancedArmModel.renderPart("rightArmModel");
                                }
                                if (config.showHandArmorType == ShowHandArmorType.SKIN) {
                                    modelArmor.enhancedArmModel.renderPart("rightArmModel_bone");
                                }
                            } else {
                                if (config.showHandArmorType == ShowHandArmorType.STATIC) {
                                    modelArmor.enhancedArmModel.renderPart("rightArmSlimModel");
                                }
                                if (config.showHandArmorType == ShowHandArmorType.SKIN) {
                                    modelArmor.enhancedArmModel.renderPart("rightArmSlimModel_bone");
                                }
                            }
                        }
                    }
                }
            }
            MinecraftForge.EVENT_BUS.post(new RenderHandSleeveEnhancedEvent.Post(this, EnumHandSide.RIGHT, model));
        } else {
            if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                model.renderPart(RIGHT_HAND_PART);
            } else {
                model.renderPart(RIGHT_SLIM_HAND_PART);
            }
        }
    }
}
}
