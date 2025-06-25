package com.modularwarfare.common.guns;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.common.network.PacketGunTransform;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;

public class GunTransformManager {
    
    private static final String TRANSFORM_AMMO_MEMORY = "transform_ammo_memory_";
    private static final String TRANSFORM_BULLET_MEMORY = "transform_bullet_memory_";
    private static final String STATE_AMMO_PREFIX = "stateAmmo_";
    private static final String STATE_BULLET_PREFIX = "stateBullet_";
    public static final String TRANSFORM_DRAW_SKIP = "transformDrawSkip";
    public static final String CURRENT_STATE = "currentState";
    private static final String EXACT_AMMO_RATIO = "exact_ammo_ratio";
    
    /**
     * 检查玩家是否正在进行变换动画
     * @param player 玩家
     * @return 是否正在变换
     */
    public static boolean isTransforming(EntityPlayer player) {
        if(player.world.isRemote) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if(heldItem.getItem() instanceof ItemGun) {
                GunType gunType = ((ItemGun)heldItem.getItem()).type;
                if(gunType.enhancedModel != null && gunType.enhancedModel.config != null) {
                    com.modularwarfare.client.fpp.enhanced.animation.AnimationController controller = 
                        com.modularwarfare.client.fpp.enhanced.animation.AnimationController.getController(player, gunType.enhancedModel.config);
                    if(controller != null) {
                        return controller.TRANSFORM < 1.0F || controller.pendingTransformGun != null;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 执行枪械变换
     * @param player 玩家
     * @param targetGunName 目标枪械注册名
     */
    public static void transformGun(EntityPlayer player, String targetGunName,UUID versionID) {
        ItemStack gunStack = player.getHeldItemMainhand();
        if(!(gunStack.getItem() instanceof ItemGun)) {
            ModularWarfare.LOGGER.warn("[Transform] Cannot transform non-gun item");
            return;
        }
        
        ItemGun currentGun = (ItemGun)gunStack.getItem();
        GunType currentType = currentGun.type;

        int currentState = getCurrentGunState(gunStack, currentType);

        int targetState = 0;
        ItemGun targetGun = ModularWarfare.gunTypes.get(targetGunName);
        if(targetGun != null) {
            for(Map.Entry<Integer, String> entry : targetGun.type.transformations.entrySet()) {
                if(entry.getValue().equals(targetGunName)) {
                    targetState = entry.getKey();
                    break;
                }
            }
        }

        if(!gunStack.hasTagCompound()) {
            gunStack.setTagCompound(new NBTTagCompound());
        }
//        gunStack.getTagCompound().setBoolean(TRANSFORM_DRAW_SKIP, true);
        
        AnimationController controller = null;
        if(player.world.isRemote) {
            controller = AnimationController.getController(player, currentType.enhancedModel.config);
        }

        if(controller != null) {
            gunStack.getTagCompound().setInteger(CURRENT_STATE, currentState);
            controller.TRANSFORM = 0F;
            controller.pendingTransformGun = targetGunName;
        } else {
            if(player.world.isRemote) {
                ModularWarfare.NETWORK.sendToServer(new PacketGunTransform(targetGunName,versionID));
            } else {
                handleTransformOnServer(player, targetGunName,versionID);
            }
        }
    }
    
    /**
     * 设置默认弹药
     * @param ratio 弹药比例(0.0-1.0)
     */
    private static void setupDefaultAmmo(GunType gunType, NBTTagCompound nbt, float ratio) {
        if(ratio <= 0) {
            return;
        }
        
        if(gunType.internalAmmoStorage != null) {
            ItemBullet defaultBullet = null;
            if(gunType.defaultBullet != null) {
                defaultBullet = ModularWarfare.bulletTypes.get(gunType.defaultBullet);
            }
            if(defaultBullet == null && gunType.acceptedBullets != null && gunType.acceptedBullets.length > 0) {
                defaultBullet = ModularWarfare.bulletTypes.get(gunType.acceptedBullets[0]);
            }
            
            if(defaultBullet != null) {
                int ammoCount = Math.round(gunType.internalAmmoStorage * ratio);
                nbt.setInteger("ammocount", ammoCount);
                nbt.setString("bulletType", defaultBullet.type.internalName);
            } else {
                ModularWarfare.LOGGER.warn("No available bullets found!");
            }
        } else {
            ItemAmmo defaultAmmo = null;
            ItemBullet defaultBullet = null;
            
            if(gunType.defaultAmmo != null) {
                defaultAmmo = ModularWarfare.ammoTypes.get(gunType.defaultAmmo);
            }
            
            if(defaultAmmo == null && gunType.acceptedAmmo != null && gunType.acceptedAmmo.length > 0) {
                defaultAmmo = ModularWarfare.ammoTypes.get(gunType.acceptedAmmo[0]);
            }
            
            if(defaultAmmo != null) {
                ItemStack ammoStack = new ItemStack(defaultAmmo);
                NBTTagCompound ammoNBT = new NBTTagCompound();
                
                int ammoCount = Math.round(defaultAmmo.type.ammoCapacity * ratio);
                ammoNBT.setInteger("ammocount", ammoCount);
                
                if(gunType.defaultBullet != null) {
                    defaultBullet = ModularWarfare.bulletTypes.get(gunType.defaultBullet);
                } else if(defaultAmmo.type.subAmmo != null && defaultAmmo.type.subAmmo.length > 0) {
                    defaultBullet = ModularWarfare.bulletTypes.get(defaultAmmo.type.subAmmo[0]);
                }
                
                if(defaultBullet != null) {
                    ItemStack bulletStack = new ItemStack(defaultBullet);
                    ammoNBT.setTag("bullet", bulletStack.writeToNBT(new NBTTagCompound()));
                } else {
                    ModularWarfare.LOGGER.warn("No available bullets found!");
                }
                
                ammoStack.setTagCompound(ammoNBT);
                nbt.setTag("ammo", ammoStack.writeToNBT(new NBTTagCompound()));
            } else {
                ModularWarfare.LOGGER.warn("No available magazines found!");
            }
        }
    }
    
    private static void saveStateAmmoData(ItemStack gunStack, int state) {
        if(gunStack.hasTagCompound()) {
            NBTTagCompound nbt = gunStack.getTagCompound();
            GunType gunType = ((ItemGun)gunStack.getItem()).type;
            
            if(gunType.internalAmmoStorage != null) {
                NBTTagCompound stateData = new NBTTagCompound();
                int ammoCount = 0;
                if(nbt.hasKey("ammocount")) {
                    ammoCount = nbt.getInteger("ammocount");
                    stateData.setInteger("ammocount", ammoCount);
                }
                if(nbt.hasKey("bullet")) {
                    stateData.setTag("bullet", nbt.getCompoundTag("bullet").copy());
                } else if(ammoCount > 0) {
                    String defaultBulletName = gunType.defaultBullet;
                    if(defaultBulletName == null && gunType.acceptedBullets != null && gunType.acceptedBullets.length > 0) {
                        defaultBulletName = gunType.acceptedBullets[0];
                    }
                    if(defaultBulletName != null) {
                        ItemBullet defaultBullet = ModularWarfare.bulletTypes.get(defaultBulletName);
                        if(defaultBullet != null) {
                            ItemStack bulletStack = new ItemStack(defaultBullet);
                            stateData.setTag("bullet", bulletStack.writeToNBT(new NBTTagCompound()));
                        }
                    }
                }
                if(stateData.hasKey("ammocount") || stateData.hasKey("bullet")) {
                    nbt.setTag(STATE_BULLET_PREFIX + state, stateData);
                }
            } else {
                if(nbt.hasKey("ammo")) {
                    NBTTagCompound ammoNBT = nbt.getCompoundTag("ammo").copy();
                    nbt.setTag(STATE_AMMO_PREFIX + state, ammoNBT);
                } else {
                    nbt.removeTag(STATE_AMMO_PREFIX + state);
                }
            }
            gunStack.setTagCompound(nbt);
        }
    }

    /**
     * 获取指定状态的弹药转换选项
     * @param gunType 枪械类型
     * @param targetState 目标状态
     * @return 弹药转换选项
     */
    private static GunType.TransformAmmoOption getAmmoOptionForState(GunType gunType, int targetState) {
        if(gunType.transformAmmoOptions != null && gunType.transformAmmoOptions.containsKey(targetState)) {
            return gunType.transformAmmoOptions.get(targetState);
        }
        return new GunType.TransformAmmoOption(true, false);
    }

    private static void loadStateAmmoData(ItemStack gunStack, int state, int stateBefore, GunType currentType, GunType targetType, GunType.TransformAmmoOption ammoOption) {
        if(!gunStack.hasTagCompound()) {
            gunStack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound nbt = gunStack.getTagCompound();

        if(targetType.internalAmmoStorage != null) {
            nbt.removeTag("ammocount");
            nbt.removeTag("bullet");
        } else {
            nbt.removeTag("ammo");
        }

        if(ammoOption.sameAmmo && currentType.internalAmmoStorage == null && targetType.internalAmmoStorage == null) {
            String beforeStateKey = STATE_AMMO_PREFIX + stateBefore;
            if(nbt.hasKey(beforeStateKey)) {
                NBTTagCompound beforeStateData = nbt.getCompoundTag(beforeStateKey);
                if(beforeStateData.hasKey("tag")) {
                    nbt.setTag("ammo", beforeStateData.copy());
                    gunStack.setTagCompound(nbt);
                    return;
                }
            }
        }

        String stateKey = targetType.internalAmmoStorage != null ? 
            STATE_BULLET_PREFIX + state : 
            STATE_AMMO_PREFIX + state;

        if(nbt.hasKey(stateKey)) {
            NBTTagCompound stateData = nbt.getCompoundTag(stateKey);
            
            float exactRatio;
            if(ammoOption.ammoRatio) {
                if(nbt.hasKey(EXACT_AMMO_RATIO)) {
                    exactRatio = nbt.getFloat(EXACT_AMMO_RATIO);
                } else {
                    exactRatio = 1.0f;
                    for(String key : nbt.getKeySet()) {
                        if(key.startsWith(STATE_BULLET_PREFIX)) {
                            NBTTagCompound bulletData = nbt.getCompoundTag(key);
                            if(bulletData != null && bulletData.hasKey("ammocount")) {
                                String stateId = key.substring(STATE_BULLET_PREFIX.length());
                                String stateGunName = null;
                                for(Map.Entry<Integer, String> entry : currentType.transformations.entrySet()) {
                                    if(String.valueOf(entry.getKey()).equals(stateId)) {
                                        stateGunName = entry.getValue();
                                        break;
                                    }
                                }
                                
                                if(stateGunName != null) {
                                    ItemGun stateGun = ModularWarfare.gunTypes.get(stateGunName);
                                    if(stateGun != null && stateGun.type.internalAmmoStorage != null) {
                                        float ratio = (float)bulletData.getInteger("ammocount") / stateGun.type.internalAmmoStorage;
                                        exactRatio = Math.min(exactRatio, ratio);
                                    }
                                }
                            }
                        } else if(key.startsWith(STATE_AMMO_PREFIX)) {
                            NBTTagCompound ammoData = nbt.getCompoundTag(key);
                            if(ammoData != null && ammoData.hasKey("tag")) {
                                NBTTagCompound ammoTag = ammoData.getCompoundTag("tag");
                                if(ammoTag != null && ammoTag.hasKey("ammocount")) {
                                    String ammoId = ammoData.getString("id").replace("modularwarfare:", "");
                                    ItemAmmo itemAmmo = ModularWarfare.ammoTypes.get(ammoId);
                                    if(itemAmmo != null) {
                                        float ratio = (float)ammoTag.getInteger("ammocount") / itemAmmo.type.ammoCapacity;
                                        exactRatio = Math.min(exactRatio, ratio);
                                    }
                                }
                            }
                        }
                    }
                    
                    if(stateData.hasKey("ammocount")) {
                        String currentGunName = currentType.internalName;
                        if(currentType.internalAmmoStorage != null) {
                            float ratio = (float)stateData.getInteger("ammocount") / currentType.internalAmmoStorage;
                            exactRatio = Math.min(exactRatio, ratio);
                        }
                    }
                    
                    if(stateData.hasKey("tag")) {
                        NBTTagCompound ammoTag = stateData.getCompoundTag("tag");
                        if(ammoTag.hasKey("ammocount")) {
                            String ammoId = stateData.getString("id").replace("modularwarfare:", "");
                            ItemAmmo itemAmmo = ModularWarfare.ammoTypes.get(ammoId);
                            if(itemAmmo != null) {
                                float ratio = (float)ammoTag.getInteger("ammocount") / itemAmmo.type.ammoCapacity;
                                exactRatio = Math.min(exactRatio, ratio);
                            }
                        }
                    }
                    
                    nbt.setFloat(EXACT_AMMO_RATIO, exactRatio);
                }
            } else {
                exactRatio = 1.0f;
            }
            
            if(targetType.internalAmmoStorage != null) {
                if(stateData.hasKey("ammocount")) {
                    if(ammoOption.ammoRatio) {
                        int newAmmoCount = Math.min(
                            Math.round(targetType.internalAmmoStorage * exactRatio),
                            stateData.getInteger("ammocount")
                        );
                        nbt.setInteger("ammocount", newAmmoCount);
                    } else {
                        nbt.setInteger("ammocount", stateData.getInteger("ammocount"));
                    }
                    
                    if(stateData.hasKey("bullet")) {
                        nbt.setTag("bullet", stateData.getCompoundTag("bullet").copy());
                    } else {
                        String defaultBulletName = targetType.defaultBullet;
                        if(defaultBulletName == null && targetType.acceptedBullets != null && targetType.acceptedBullets.length > 0) {
                            defaultBulletName = targetType.acceptedBullets[0];
                        }
                        if(defaultBulletName != null) {
                            ItemBullet defaultBullet = ModularWarfare.bulletTypes.get(defaultBulletName);
                            if(defaultBullet != null) {
                                ItemStack bulletStack = new ItemStack(defaultBullet);
                                nbt.setTag("bullet", bulletStack.writeToNBT(new NBTTagCompound()));
                            }
                        }
                    }
                }
            } else {
                if(stateData.hasKey("tag")) {
                    NBTTagCompound targetAmmo = stateData.copy();
                    NBTTagCompound targetAmmoTag = targetAmmo.getCompoundTag("tag");
                    
                    if(ammoOption.ammoRatio && targetAmmoTag.hasKey("ammocount")) {
                        String ammoId = targetAmmo.getString("id").replace("modularwarfare:", "");
                        ItemAmmo itemAmmo = ModularWarfare.ammoTypes.get(ammoId);
                        if(itemAmmo != null) {
                            int newAmmoCount = Math.min(
                                Math.round(itemAmmo.type.ammoCapacity * exactRatio),
                                targetAmmoTag.getInteger("ammocount")
                            );
                            targetAmmoTag.setInteger("ammocount", newAmmoCount);
                            targetAmmo.setTag("tag", targetAmmoTag);
                        }
                    }
                    
                    nbt.setTag("ammo", targetAmmo);
                }
            }
        }
        
        gunStack.setTagCompound(nbt);
    }
    
    /**
     * 处理服务器端的枪械变换
     */
    public static void handleTransformOnServer(EntityPlayer player, String targetGunName,UUID versionID) {
        ItemStack currentGun = player.getHeldItemMainhand();
        if(!(currentGun.getItem() instanceof ItemGun)) {
            ModularWarfare.LOGGER.warn("Server: Player's held item is not a gun");
            return;
        }
        
        ItemGun currentGunItem = (ItemGun) currentGun.getItem();
        GunType currentType = currentGunItem.type;
        
        ItemGun targetGun = ModularWarfare.gunTypes.get(targetGunName);
        if(targetGun == null) {
            ModularWarfare.LOGGER.warn("Server: Target gun not found: " + targetGunName);
            return;
        }
        
        ItemStack newGun = new ItemStack(targetGun);
        NBTTagCompound newNBT = new NBTTagCompound();
        
        if(currentGun.hasTagCompound()) {
            NBTTagCompound oldNBT = currentGun.getTagCompound();
 
            for(AttachmentPresetEnum type : AttachmentPresetEnum.values()) {
                String key = "attachment_" + type.typeName;
                if(oldNBT.hasKey(key)) {
                    newNBT.setTag(key, oldNBT.getTag(key).copy());
                }
            }
            
            GunType targetType = targetGun.type;
            
            int currentGunState = getCurrentGunState(currentGun, currentType);
            
            int targetGunState = -1;
            for(Map.Entry<Integer, String> entry : targetType.transformations.entrySet()) {
                if(entry.getValue().equals(targetGunName)) {
                    targetGunState = entry.getKey();
                    break;
                }
            }
            
            if(targetGunState != -1) {
                saveStateAmmoData(currentGun, currentGunState);
                
                for(String key : oldNBT.getKeySet()) {
                    if(key.startsWith(STATE_AMMO_PREFIX) || key.startsWith(STATE_BULLET_PREFIX)) {
                        newNBT.setTag(key, oldNBT.getTag(key).copy());
                    }
                }
                
                newGun.setTagCompound(newNBT);
                
                newNBT.setInteger(CURRENT_STATE, targetGunState);
                
                GunType.TransformAmmoOption ammoOption = getAmmoOptionForState(currentType, targetGunState);
                loadStateAmmoData(newGun, targetGunState, currentGunState, currentType, targetType, ammoOption);
            }
            
            newNBT.setInteger("init", 1);
            if(oldNBT.hasKey("skinId")) {
                newNBT.setInteger("skinId", oldNBT.getInteger("skinId"));
            }
            newNBT.setString("firemode", targetType.fireModes[0].name().toLowerCase());
            
            if(oldNBT.hasKey("shotsremaining")) {
                newNBT.setInteger("shotsremaining", oldNBT.getInteger("shotsremaining"));
            }
        }
        
        newNBT.setUniqueId(TRANSFORM_DRAW_SKIP, versionID);
        newGun.setTagCompound(newNBT);
        
        player.setHeldItem(player.getActiveHand() != null ? player.getActiveHand() : net.minecraft.util.EnumHand.MAIN_HAND, newGun);
        if(!player.world.isRemote) {
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }
    }

    private static int getCurrentGunState(ItemStack gunStack, GunType gunType) {
        if(gunStack.hasTagCompound()) {
            NBTTagCompound nbt = gunStack.getTagCompound();
            if(nbt.hasKey(CURRENT_STATE)) {
                return nbt.getInteger(CURRENT_STATE);
            }
        }
        
        String currentGunName = gunType.internalName;
        for(Map.Entry<Integer, String> entry : gunType.transformations.entrySet()) {
            if(entry.getValue().equals(currentGunName)) {
                return entry.getKey();
            }
        }
        
        return 0;
    }
}