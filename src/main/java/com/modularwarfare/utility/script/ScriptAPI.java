package com.modularwarfare.utility.script;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.input.Keyboard;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.ArmorRenderConfig;
import com.modularwarfare.client.fpp.basic.configs.AttachmentRenderConfig;
import net.minecraft.inventory.EntityEquipmentSlot;
import com.modularwarfare.common.armor.ArmorType;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.backpacks.BackpackType;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.AmmoType;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.common.guns.AttachmentType;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.BulletProperty;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAmmo;
import com.modularwarfare.common.guns.ItemAttachment;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.PotionEntry;
import com.modularwarfare.common.guns.WeaponFireMode;
import com.modularwarfare.common.melee.ItemMelee;
import com.modularwarfare.common.melee.MeleeType;
import com.modularwarfare.client.fpp.enhanced.AnimationMeleeType;

import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ScriptAPI {
    public Lang Lang = new Lang();
    public Stack Stack = new Stack();
    public Gun Gun = new Gun();
    public Ammo Ammo = new Ammo();
    public Input Input =new Input();
    public Bullet Bullet =new Bullet();
    public Attachment Attachment = new Attachment();
    public Grenade Grenade = new Grenade();
    public Armor Armor = new Armor();
    public Backpack Backpack = new Backpack();
    public Melee Melee = new Melee();
    public Logger Logger = new Logger();

    public static class Lang {
        public String format(String key, Object... parms) {
            return I18n.format(key, parms);
        }
    }
    
    public static class Stack {
        public boolean hasNbt(ItemStack stack) {
            return stack.hasTagCompound();
        }

        public NBTTagCompound getNbt(ItemStack stack) {
            if(stack.getTagCompound()==null) {
                return new NBTTagCompound();
            }
            return stack.getTagCompound().copy();
        }
        
        public ItemStack getStack(int itemid) {
            return new ItemStack(Item.getItemById(itemid));
        }
        
        public String getDisplayName(ItemStack stack) {
            return stack.getDisplayName();
        }
        
        public boolean isEmpty(ItemStack stack) {
            return stack.isEmpty();
        }
    }

    public static class Gun {
        public boolean isGun(ItemStack stack) {
            return stack.getItem() instanceof ItemGun;
        }

        public boolean hasAmmoLoaded(ItemStack stack) {
            return ItemGun.hasAmmoLoaded(stack);
        }

        public ItemStack getAmmoStack(ItemStack gunStack) {
            if (hasAmmoLoaded(gunStack)) {
                ItemStack ammoStack = new ItemStack(gunStack.getTagCompound().getCompoundTag("ammo"));
                return ammoStack;
            }
            return ItemStack.EMPTY;
        }
        
        public boolean isBulletGun(ItemStack itemStack) {
            if (!isGun(itemStack)) {
                return false;
            }
            if (((ItemGun) itemStack.getItem()).type.acceptedBullets != null
                    && ((ItemGun) itemStack.getItem()).type.acceptedBullets.length > 0) {
                return true;
            }
            return false;
        }
        
        public String getGunExtraLore(ItemStack stack) {
            if (!isGun(stack)) {
                return "";
            }
            return ((ItemGun)stack.getItem()).type.extraLore;
        }
        
        public ArrayList<String> getInstalledAttachments(ItemStack stack){
            ArrayList<String> list=new ArrayList<>();
            if (!isGun(stack)) {
                return list;
            }
            for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                ItemStack itemStack = GunType.getAttachment(stack, attachment);
                if (itemStack != null && itemStack.getItem() != Items.AIR) {
                    try {
                        AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                        if (attachmentType != null && attachmentType.internalName != null) {
                            list.add(attachmentType.internalName);
                        } else {
                            if (ModularWarfare.DEV_ENV) {
                                ModularWarfare.LOGGER.warn("[ScriptAPI] Installed attachment type or internalName is null for attachment: " + attachment.name());
                            }
                        }
                    } catch (Exception e) {
                        if (ModularWarfare.DEV_ENV) {
                            ModularWarfare.LOGGER.warn("[ScriptAPI] Error processing installed attachment: " + attachment.name() + " - " + e.getMessage());
                        }
                    }
                }
            }
            return list;
        }
        
        public int getAmmoStorage(ItemStack itemStack) {
            if(!isBulletGun(itemStack)) {
                return 0;
            }
            return ((ItemGun)itemStack.getItem()).type.internalAmmoStorage;
        }
        
        public int getUsedBulletItem(ItemStack stack) {
            if (!isGun(stack)) {
                return Item.getIdFromItem(Items.AIR);
            }
            if(ItemGun.getUsedBullet(stack, ((ItemGun)stack.getItem()).type)!=null) {
                return Item.getIdFromItem(ItemGun.getUsedBullet(stack, ((ItemGun)stack.getItem()).type));
            }
            return Item.getIdFromItem(Items.AIR);
        }
        
        public float getGunBulletSpread(ItemStack itemStack) {
            if (!isGun(itemStack)) {
                return 0;
            }
            return ((ItemGun)itemStack.getItem()).type.bulletSpread;
        }
        
        public float getGunDamage(ItemStack itemStack) {
            if (!isGun(itemStack)) {
                return 0;
            }
            return ((ItemGun)itemStack.getItem()).type.gunDamage;
        }
        
        public float getGunNumBullets(ItemStack itemStack) {
            if (!isGun(itemStack)) {
                return 0;
            }
            return ((ItemGun)itemStack.getItem()).type.numBullets;
        }

        public WeaponFireMode getFireMode(ItemStack stack) {
            return GunType.getFireMode(stack);
        }
        
        public HashMap<String,ArrayList<String>> getAcceptedAttachment(ItemStack stack){
            HashMap<String,ArrayList<String>> map=new HashMap<>();
            if (!isGun(stack)) {
                return map;
            }
            if(((ItemGun)stack.getItem()).type.acceptedAttachments==null) {
                return map;
            }
            ((ItemGun)stack.getItem()).type.acceptedAttachments.forEach((k,v)->{
                if(!map.containsKey(k.typeName)) {
                    map.put(k.typeName, new ArrayList<String>());
                }
                v.forEach((name)->{
                    if (ModularWarfare.attachmentTypes.containsKey(name) && ModularWarfare.attachmentTypes.get(name) != null) {
                        AttachmentType attachmentType = ModularWarfare.attachmentTypes.get(name).type;
                        if (attachmentType != null && attachmentType.internalName != null) {
                            map.get(k.typeName).add(attachmentType.internalName);
                        } else {
                            if (ModularWarfare.DEV_ENV) {
                                ModularWarfare.LOGGER.warn("[ScriptAPI] Attachment type or internalName is null for: " + name);
                            }
                        }
                    } else {
                        if (ModularWarfare.DEV_ENV) {
                            ModularWarfare.LOGGER.warn("[ScriptAPI] Attachment not registered: " + name);
                        }
                    }
                });
            });
            return map;
        }
        
        public ArrayList<String> getAcceptedAmmoOrBullet(ItemStack stack){
            ArrayList<String> list=new ArrayList<String>();
            if (!isGun(stack)) {
                return list;
            }
            if(((ItemGun)stack.getItem()).type.acceptedAmmo!=null) {
                for(String name:((ItemGun)stack.getItem()).type.acceptedAmmo) {
                    if (ModularWarfare.ammoTypes.containsKey(name) && ModularWarfare.ammoTypes.get(name) != null) {
                        AmmoType ammoType = ModularWarfare.ammoTypes.get(name).type;
                        if (ammoType != null && ammoType.internalName != null) {
                            list.add(ammoType.internalName);
                        } else {
                            if (ModularWarfare.DEV_ENV) {
                                ModularWarfare.LOGGER.warn("[ScriptAPI] Ammo type or internalName is null for: " + name);
                            }
                        }
                    } else {
                        if (ModularWarfare.DEV_ENV) {
                            ModularWarfare.LOGGER.warn("[ScriptAPI] Ammo not registered: " + name);
                        }
                    }
                }  
            }
            if(((ItemGun)stack.getItem()).type.acceptedBullets!=null) {
                for(String name:((ItemGun)stack.getItem()).type.acceptedBullets) {
                    if (ModularWarfare.bulletTypes.containsKey(name) && ModularWarfare.bulletTypes.get(name) != null) {
                        BulletType bulletType = ModularWarfare.bulletTypes.get(name).type;
                        if (bulletType != null && bulletType.internalName != null) {
                            list.add(bulletType.internalName);
                        } else {
                            if (ModularWarfare.DEV_ENV) {
                                ModularWarfare.LOGGER.warn("[ScriptAPI] Bullet type or internalName is null for: " + name);
                            }
                        }
                    } else {
                        if (ModularWarfare.DEV_ENV) {
                            ModularWarfare.LOGGER.warn("[ScriptAPI] Bullet not registered: " + name);
                        }
                    }
                }
            }
            return list;
        }
    }

    public static class Ammo {
        
        public boolean isAmmo(ItemStack stack) {
            return stack.getItem() instanceof ItemAmmo;
        }
        
        public int getUsedBulletItem(ItemStack stack) {
            if(!isAmmo(stack)) {
                return Item.getIdFromItem(Items.AIR);
            }
            if (stack.getTagCompound() != null) {
                if (stack.getTagCompound().hasKey("bullet")) {
                    ItemStack usedBullet = new ItemStack(stack.getTagCompound().getCompoundTag("bullet"));
                    return Item.getIdFromItem(usedBullet.getItem());
                }
            }
            return Item.getIdFromItem(Items.AIR);
        }
        
        public ArrayList<String> getAcceptedBullet(ItemStack stack){
            ArrayList<String> list=new ArrayList<String>();
            if (!isAmmo(stack)) {
                return list;
            }
            if(((ItemAmmo)stack.getItem()).type.subAmmo!=null) {
                for(String name:((ItemAmmo)stack.getItem()).type.subAmmo) {
                    if (ModularWarfare.bulletTypes.containsKey(name) && ModularWarfare.bulletTypes.get(name) != null) {
                        BulletType bulletType = ModularWarfare.bulletTypes.get(name).type;
                        if (bulletType != null && bulletType.internalName != null) {
                            list.add(bulletType.internalName);
                        } else {
                            if (ModularWarfare.DEV_ENV) {
                                ModularWarfare.LOGGER.warn("[ScriptAPI] Bullet type or internalName is null for: " + name);
                            }
                        }
                    } else {
                        if (ModularWarfare.DEV_ENV) {
                            ModularWarfare.LOGGER.warn("[ScriptAPI] Bullet not registered: " + name);
                        }
                    }
                }
            }
            return list;
        }

        public int getAmmoCapacity(ItemStack stack) {
            return ((ItemAmmo) stack.getItem()).type.ammoCapacity;
        }
        
        public int getMagazineCount(ItemStack stack) {
            return ((ItemAmmo) stack.getItem()).type.magazineCount;
        }
        
    }
    
    public static class Bullet{
        public boolean isBullet(ItemStack stack) {
            return stack.getItem() instanceof ItemBullet;
        }
        
        public float getDamageFactor(ItemStack itemStack) {
            if(itemStack.getItem() instanceof ItemBullet) {
                return ((ItemBullet)itemStack.getItem()).type.bulletDamageFactor;
            }
            return 1;
        }
        
        public float getAccuracyFactor(ItemStack itemStack) {
            if(itemStack.getItem() instanceof ItemBullet) {
                return ((ItemBullet)itemStack.getItem()).type.bulletAccuracyFactor;
            }
            return 1;
        }

        public HashMap<String, BulletProperty> getBulletProperties(ItemStack itemStack) {
            if(itemStack.getItem() instanceof ItemBullet) {
                return ((ItemBullet)itemStack.getItem()).type.bulletProperties;
            }
            return null;
        }
    }

    public static class Input{
        public boolean isKeyHolding(int key) {
            return Keyboard.isKeyDown(key);
        }
    }
    
    public static class Attachment {
        // Functional interface for property extraction
        @FunctionalInterface
        private interface PropertyExtractor<T> {
            T extract(AttachmentType type, AttachmentRenderConfig config);
        }
        
        public boolean isAttachment(ItemStack stack) {
            return stack.getItem() instanceof ItemAttachment;
        }
        
        public AttachmentType getAttachmentType(ItemStack stack) {
            if (!isAttachment(stack)) {
                return null;
            }
            return ((ItemAttachment) stack.getItem()).type;
        }
        
        public AttachmentRenderConfig getRenderConfig(ItemStack stack) {
            AttachmentType type = getAttachmentType(stack);
            if (type == null) {
                return null;
            }
            try {
                return ModularWarfare.getRenderConfig(type, AttachmentRenderConfig.class);
            } catch (Exception e) {
                if (ModularWarfare.DEV_ENV) {
                    ModularWarfare.LOGGER.warn("[ScriptAPI] Failed to get render config: " + e.getMessage());
                }
                return null;
            }
        }
        
        public String getAttachmentTypeName(ItemStack stack) {
            AttachmentType type = getAttachmentType(stack);
            if (type == null || type.attachmentType == null) {
                return "";
            }
            return type.attachmentType.typeName;
        }
        
        // Generic method to get float properties with type check
        private float getFloatProperty(ItemStack stack, AttachmentPresetEnum requiredType, 
                                      PropertyExtractor<Float> extractor, float defaultValue) {
            AttachmentType type = getAttachmentType(stack);
            if (type == null || (requiredType != null && type.attachmentType != requiredType)) {
                return defaultValue;
            }
            AttachmentRenderConfig config = getRenderConfig(stack);
            Float result = extractor.extract(type, config);
            return result != null ? result : defaultValue;
        }
        
        // Generic method for boolean properties
        private boolean getBooleanProperty(ItemStack stack, AttachmentPresetEnum requiredType, 
                                          PropertyExtractor<Boolean> extractor) {
            AttachmentType type = getAttachmentType(stack);
            if (type == null || (requiredType != null && type.attachmentType != requiredType)) {
                return false;
            }
            AttachmentRenderConfig config = getRenderConfig(stack);
            Boolean result = extractor.extract(type, config);
            return result != null ? result : false;
        }
        
        // === Sight properties ===
        public float getSightAimSpeedFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Sight, (t, c) -> t.sight.aimSpeedFactor, 1.0f);
        }
        
        public float getSightMouseSensitivityFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Sight, (t, c) -> c != null && c.sight != null ? c.sight.mouseSensitivityFactor : null, 1.0f);
        }
        
        public float getSightFovZoom(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Sight, (t, c) -> c != null && c.sight != null ? c.sight.fovZoom : null, 1.0f);
        }
        
        public float getSightFovZoomMin(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Sight, (t, c) -> c != null && c.sight != null ? c.sight.fovZoomMin : null, -1.0f);
        }
        
        public float getSightFovZoomMax(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Sight, (t, c) -> c != null && c.sight != null ? c.sight.fovZoomMax : null, -1.0f);
        }
        
        public float[] getSightFovZoomStage(ItemStack stack) {
            AttachmentRenderConfig config = getRenderConfig(stack);
            if (config == null || config.sight == null || config.sight.fovZoomStage == null) {
                return null;
            }
            return config.sight.fovZoomStage;
        }
        
        // === Barrel properties ===
        public boolean isSuppressor(ItemStack stack) {
            return getBooleanProperty(stack, AttachmentPresetEnum.Barrel, (t, c) -> t.barrel.isSuppressor);
        }
        
        public boolean hideFlash(ItemStack stack) {
            return getBooleanProperty(stack, AttachmentPresetEnum.Barrel, (t, c) -> t.barrel.hideFlash);
        }
        
        public float getBarrelRecoilPitchFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Barrel, (t, c) -> t.barrel.recoilPitchFactor, 1.0f);
        }
        
        public float getBarrelRecoilYawFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Barrel, (t, c) -> t.barrel.recoilYawFactor, 1.0f);
        }
        
        public float getBarrelAccuracyFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Barrel, (t, c) -> t.barrel.accuracyFactor, 1.0f);
        }
        
        // === Grip properties ===
        public float getGripRecoilPitchFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Grip, (t, c) -> t.grip.recoilPitchFactor, 1.0f);
        }
        
        public float getGripRecoilYawFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Grip, (t, c) -> t.grip.recoilYawFactor, 1.0f);
        }
        
        // === Stock properties ===
        public float getStockAimSpeedFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Stock, (t, c) -> t.stock.aimSpeedFactor, 1.0f);
        }
        
        public float getStockRecoilPitchFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Stock, (t, c) -> t.stock.recoilPitchFactor, 1.0f);
        }
        
        public float getStockRecoilYawFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Stock, (t, c) -> t.stock.recoilYawFactor, 1.0f);
        }
        
        public float getStockModelRecoilBackwardsFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Stock, (t, c) -> c != null && c.stock != null ? c.stock.modelRecoilBackwardsFactor : null, 1.0f);
        }
        
        public float getStockModelRecoilUpwardsFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Stock, (t, c) -> c != null && c.stock != null ? c.stock.modelRecoilUpwardsFactor : null, 1.0f);
        }
        
        public float getStockModelRecoilShakeFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Stock, (t, c) -> c != null && c.stock != null ? c.stock.modelRecoilShakeFactor : null, 1.0f);
        }
        
        // === Laser properties ===
        public float getLaserAccuracyFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Laser, (t, c) -> t.laser.accuracyFactor, 1.0f);
        }
        
        public float getLaserAimSpeedFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Laser, (t, c) -> t.laser.aimSpeedFactor, 1.0f);
        }
        
        public float getLaserRecoilPitchFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Laser, (t, c) -> t.laser.recoilPitchFactor, 1.0f);
        }
        
        public float getLaserRecoilYawFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Laser, (t, c) -> t.laser.recoilYawFactor, 1.0f);
        }
        
        public float[] getLaserColor(ItemStack stack) {
            AttachmentRenderConfig config = getRenderConfig(stack);
            if (config == null || config.laser == null) {
                return new float[]{1.0f, 0.0f, 0.0f};
            }
            return config.laser.laserColor;
        }
        
        public double getLaserMaxDistance(ItemStack stack) {
            AttachmentRenderConfig config = getRenderConfig(stack);
            if (config == null || config.laser == null) {
                return 100.0;
            }
            return config.laser.maxDistance;
        }
        
        // === Pistolgrip properties ===
        public float getPistolgripAimSpeedFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Pistolgrip, (t, c) -> t.pistolgrip.aimSpeedFactor, 1.0f);
        }
        
        public float getPistolgripRecoilPitchFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Pistolgrip, (t, c) -> t.pistolgrip.recoilPitchFactor, 1.0f);
        }
        
        public float getPistolgripRecoilYawFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Pistolgrip, (t, c) -> t.pistolgrip.recoilYawFactor, 1.0f);
        }
        
        public float getPistolgripModelRecoilBackwardsFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Pistolgrip, (t, c) -> c != null && c.pistolgrip != null ? c.pistolgrip.modelRecoilBackwardsFactor : null, 1.0f);
        }
        
        public float getPistolgripModelRecoilUpwardsFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Pistolgrip, (t, c) -> c != null && c.pistolgrip != null ? c.pistolgrip.modelRecoilUpwardsFactor : null, 1.0f);
        }
        
        public float getPistolgripModelRecoilShakeFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Pistolgrip, (t, c) -> c != null && c.pistolgrip != null ? c.pistolgrip.modelRecoilShakeFactor : null, 1.0f);
        }
        
        // === Handguard properties ===
        public float getHandguardAimSpeedFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Handguard, (t, c) -> t.handguard.aimSpeedFactor, 1.0f);
        }
        
        public float getHandguardRecoilPitchFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Handguard, (t, c) -> t.handguard.recoilPitchFactor, 1.0f);
        }
        
        public float getHandguardRecoilYawFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Handguard, (t, c) -> t.handguard.recoilYawFactor, 1.0f);
        }
        
        public float getHandguardModelRecoilBackwardsFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Handguard, (t, c) -> c != null && c.handguard != null ? c.handguard.modelRecoilBackwardsFactor : null, 1.0f);
        }
        
        public float getHandguardModelRecoilUpwardsFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Handguard, (t, c) -> c != null && c.handguard != null ? c.handguard.modelRecoilUpwardsFactor : null, 1.0f);
        }
        
        public float getHandguardModelRecoilShakeFactor(ItemStack stack) {
            return getFloatProperty(stack, AttachmentPresetEnum.Handguard, (t, c) -> c != null && c.handguard != null ? c.handguard.modelRecoilShakeFactor : null, 1.0f);
        }
    }
    
    public static class Grenade {
        public boolean isGrenade(ItemStack stack) {
            return stack.getItem() instanceof ItemGrenade;
        }
        
        public GrenadeType getGrenadeType(ItemStack stack) {
            if (!isGrenade(stack)) {
                return null;
            }
            return ((ItemGrenade) stack.getItem()).type;
        }
        
        public String getGrenadeTypeName(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            if (type == null || type.grenadeType == null) {
                return "";
            }
            return type.grenadeType.typeName;
        }
        
        // Basic properties
        public float getFuseTime(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.fuseTime : 0f;
        }
        
        public float getThrowStrength(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.throwStrength : 0f;
        }
        
        public float getThrowStrengthLow(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.throwStrengthLow : 0f;
        }
        
        public boolean isSticky(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null && type.isSticky;
        }
        
        public boolean isInstantExplode(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null && type.instantExplode;
        }
        
        public float getImpactDamage(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.impactDamage : 0f;
        }
        
        // Explosion properties
        public float getExplosionDamage(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.explosionDamage : 0f;
        }
        
        public float getExplosionRange(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.explosionRange : 0f;
        }
        
        public float getExplosionKnockback(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.explosionKnockback : 0f;
        }
        
        public boolean getDamageWorld(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null && type.damageWorld;
        }
        
        public boolean getExplosionThroughWalls(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null && type.explosionThroughWalls;
        }
        
        // Fire properties
        public boolean getCausesFire(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null && type.causesFire;
        }
        
        public float getFireDamage(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.fireDamage : 0f;
        }
        
        public int getFireDuration(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.fireDuration : 0;
        }
        
        // Smoke properties
        public float getSmokeTime(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.smokeTime : 0f;
        }
        
        // Special effects
        public PotionEntry[] getExplosionPotionEffects(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.explosionPotionEffects : null;
        }
        
        public int getExplosionFireLevel(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.explosionFireLevel : 0;
        }
        
        public float getExplosionKnockLevel(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null ? type.explosionKnockLevel : 0f;
        }
        
        public boolean getBanShield(ItemStack stack) {
            GrenadeType type = getGrenadeType(stack);
            return type != null && type.banShield;
        }
    }
    
    public static class Armor {
        public boolean isArmor(ItemStack stack) {
            return stack.getItem() instanceof ItemMWArmor || stack.getItem() instanceof ItemSpecialArmor;
        }
        
        public ArmorType getArmorType(ItemStack stack) {
            if (stack.getItem() instanceof ItemMWArmor) {
                return ((ItemMWArmor) stack.getItem()).type;
            } else if (stack.getItem() instanceof ItemSpecialArmor) {
                return ((ItemSpecialArmor) stack.getItem()).type;
            }
            return null;
        }
        
        public ArmorRenderConfig getRenderConfig(ItemStack stack) {
            ArmorType type = getArmorType(stack);
            if (type == null) {
                return null;
            }
            try {
                return ModularWarfare.getRenderConfig(type, ArmorRenderConfig.class);
            } catch (Exception e) {
                if (ModularWarfare.DEV_ENV) {
                    ModularWarfare.LOGGER.warn("[ScriptAPI] Failed to get armor render config: " + e.getMessage());
                }
                return null;
            }
        }
        
        public int getDurability(ItemStack stack) {
            ArmorType type = getArmorType(stack);
            return type != null && type.durability != null ? type.durability : 0;
        }
        
        public double getDefense(ItemStack stack) {
            ArmorType type = getArmorType(stack);
            return type != null ? type.defense : 0.0;
        }
        
        public boolean isSuit(ItemStack stack) {
            ArmorRenderConfig config = getRenderConfig(stack);
            return config != null && config.extra != null && config.extra.isSuit;
        }
        
        public String getArmorSlot(ItemStack stack) {
            if (stack.getItem() instanceof ItemMWArmor) {
                ItemMWArmor armor = (ItemMWArmor) stack.getItem();
                // Get slot from EntityEquipmentSlot
                EntityEquipmentSlot slot = armor.armorType;
                if (slot != null) {
                    return slot.getName();
                }
            } else if (stack.getItem() instanceof ItemSpecialArmor) {
                ItemSpecialArmor armor = (ItemSpecialArmor) stack.getItem();
                if (armor.armorType != null) {
                    return armor.armorType.name().toLowerCase();
                }
            }
            return "";
        }
    }
    
    public static class Backpack {
        public boolean isBackpack(ItemStack stack) {
            return stack.getItem() instanceof ItemBackpack;
        }
        
        public BackpackType getBackpackType(ItemStack stack) {
            if (stack.getItem() instanceof ItemBackpack) {
                return ((ItemBackpack) stack.getItem()).type;
            }
            return null;
        }
        
        public int getSize(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null ? type.size : 0;
        }
        
        public boolean getAllowSmallerBackpackStorage(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null && type.allowSmallerBackpackStorage;
        }
        
        public int getMaxWeaponStorage(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null && type.maxWeaponStorage != null ? type.maxWeaponStorage : 0;
        }
        
        public boolean isElytra(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null && type.isElytra;
        }
        
        public boolean isElytraStoppable(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null && type.elytraStoppable;
        }
        
        public boolean isJet(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null && type.isJet;
        }
        
        public boolean getJetSneakHover(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null && type.jetSneakHover;
        }
        
        public boolean getJetGroundDust(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null && type.jetGroundDust;
        }
        
        public float getJetWorkForce(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null ? type.jetWorkForce : 0.0f;
        }
        
        public float getJetIdleForce(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null ? type.jetIdleForce : 0.0f;
        }
        
        public float getJetMaxForce(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null ? type.jetMaxForce : 0.0f;
        }
        
        public float getJetElytraBoost(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null ? type.jetElytraBoost : 0.0f;
        }
        
        public int getJetElytraBoostDuration(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null ? type.jetElytraBoostDuration : 0;
        }
        
        public int getJetElytraBoostCoolTime(ItemStack stack) {
            BackpackType type = getBackpackType(stack);
            return type != null ? type.jetElytraBoostCoolTime : 0;
        }
    }
    
    public static class Melee {
        public boolean isMelee(ItemStack stack) {
            return stack.getItem() instanceof ItemMelee;
        }
        
        public MeleeType getMeleeType(ItemStack stack) {
            if (stack.getItem() instanceof ItemMelee) {
                return ((ItemMelee) stack.getItem()).type;
            }
            return null;
        }
        
        // Light attack methods
        public int getLightAttackCount(ItemStack stack) {
            MeleeType type = getMeleeType(stack);
            if (type == null || type.attack == null) {
                return 0;
            }
            return type.attack.length;
        }
        
        public float getLightAttackDamage(ItemStack stack, int index) {
            MeleeType type = getMeleeType(stack);
            if (type == null) {
                return 0;
            }
            MeleeType.AnimationInfo info = type.getAnimationInfo(AnimationMeleeType.ATTACK, index);
            return info != null ? info.damage : 0;
        }
        
        public float getLightAttackRange(ItemStack stack, int index) {
            MeleeType type = getMeleeType(stack);
            if (type == null) {
                return 0;
            }
            MeleeType.AnimationInfo info = type.getAnimationInfo(AnimationMeleeType.ATTACK, index);
            return info != null ? info.range : 0;
        }
        
        public boolean getLightAttackPenetration(ItemStack stack, int index) {
            MeleeType type = getMeleeType(stack);
            if (type == null) {
                return false;
            }
            MeleeType.AnimationInfo info = type.getAnimationInfo(AnimationMeleeType.ATTACK, index);
            return info != null && info.attackPenetration;
        }
        
        public boolean getLightAttackCanBounced(ItemStack stack, int index) {
            MeleeType type = getMeleeType(stack);
            if (type == null) {
                return false;
            }
            MeleeType.AnimationInfo info = type.getAnimationInfo(AnimationMeleeType.ATTACK, index);
            return info != null && info.canBounced;
        }
        
        // Heavy attack methods
        public int getHeavyAttackCount(ItemStack stack) {
            MeleeType type = getMeleeType(stack);
            if (type == null || type.attackHeavy == null) {
                return 0;
            }
            return type.attackHeavy.length;
        }
        
        public float getHeavyAttackDamage(ItemStack stack, int index) {
            MeleeType type = getMeleeType(stack);
            if (type == null) {
                return 0;
            }
            MeleeType.AnimationInfo info = type.getAnimationInfo(AnimationMeleeType.HEAVYATTACK, index);
            return info != null ? info.damage : 0;
        }
        
        public float getHeavyAttackRange(ItemStack stack, int index) {
            MeleeType type = getMeleeType(stack);
            if (type == null) {
                return 0;
            }
            MeleeType.AnimationInfo info = type.getAnimationInfo(AnimationMeleeType.HEAVYATTACK, index);
            return info != null ? info.range : 0;
        }
        
        public boolean getHeavyAttackPenetration(ItemStack stack, int index) {
            MeleeType type = getMeleeType(stack);
            if (type == null) {
                return false;
            }
            MeleeType.AnimationInfo info = type.getAnimationInfo(AnimationMeleeType.HEAVYATTACK, index);
            return info != null && info.attackPenetration;
        }
        
        public boolean getHeavyAttackCanBounced(ItemStack stack, int index) {
            MeleeType type = getMeleeType(stack);
            if (type == null) {
                return false;
            }
            MeleeType.AnimationInfo info = type.getAnimationInfo(AnimationMeleeType.HEAVYATTACK, index);
            return info != null && info.canBounced;
        }
    }
    
    public static class Logger {
        public void warn(String message) {
            if (ModularWarfare.DEV_ENV) {
                ModularWarfare.LOGGER.warn("[ScriptAPI] " + message);
            }
        }
        
        public void error(String message) {
            if (ModularWarfare.DEV_ENV) {
                ModularWarfare.LOGGER.error("[ScriptAPI] " + message);
            }
        }
        
        public void info(String message) {
            if (ModularWarfare.DEV_ENV) {
                ModularWarfare.LOGGER.info("[ScriptAPI] " + message);
            }
        }
    }
}
