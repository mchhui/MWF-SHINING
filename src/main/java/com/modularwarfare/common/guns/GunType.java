package com.modularwarfare.common.guns;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.GunRenderConfig;
import com.modularwarfare.client.model.ModelGun;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.common.network.PacketPlaySound;
import com.modularwarfare.common.textures.TextureEnumType;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.utility.MWSound;
import com.modularwarfare.utility.SoundEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class GunType extends BaseType {

    /**
     * Weapon Classification for later use with default animations etc
     */
    public WeaponType weaponType;

    //public WeaponScopeType scopeType = WeaponScopeType.DEFAULT;
    public WeaponScopeModeType scopeModeType = WeaponScopeModeType.SIMPLE;

    public WeaponAnimationType animationType = WeaponAnimationType.BASIC;

    // if this is true, you cant shoot until the all fire animation finishes,
    public boolean restrictingFireAnimation = false;
    // if this is true, you cant reload until the all fire animation finishes,
    public boolean firingReload = true;
    
    //Munition variables
    /**
     * Damage inflicted per bullet. Multiplied by the bullet damage value.
     */
    public float gunDamage = 0;

    public float moveSpeedModifier = 1F;

    /**
     * Damage inflicted per bullet. Multiplied by the bullet damage value.
     */
    public float gunDamageHeadshotBonus = 0;

    /**
     * 最大可连续穿透的碰撞箱大小累积值
     */
    public float gunPenetrateSize = 0.1f;
    /**
     * 穿透后伤害线性衰减
     */
    public boolean gunPenetrationDamageFalloff = true;
    /**
     *  可以穿透的最大方块爆炸阻力
     */
    public float gunMaxPenetrateBlockResistance = 0;
    /**
     *  最大可连续穿透的方块爆炸阻力累积值
     */
    public float gunPenetrateBlocksResistance = 0;
    /**
     * 穿透后的伤害衰减系数 原始伤害 * ((剩余连续穿透的阻力 / 最大连续穿透阻力) * 衰减系数)
     */
    public float gunPenetrateBlocksDamageFalloffFactor = 0.8f;
    /**
     * Weapon block range
     */
    public int weaponMaxRange = 200;
    /**
     * 命中超过这个距离的实体开始产生伤害衰减
     */
    public int weaponEffectiveRange = 150;
    /**
     * The number of bullet entities created by each shot
     */
    public int numBullets = 1;

    /**
     * 卸子弹时,要求客户端少播放unload动画的次数。
     * Reduce the actual playing times of unload animation on the client.
     */
    public int modifyUnloadBullets = 0;

    /**
     * The amount that bullets spread out when fired from this gun
     */
    public float bulletSpread;

    /**
     * 枪械手持长度影响
     */
    
    public float gunHoldLength = 1.5f;
    
    /**
     * The fire rate of the gun in RPM, 1200 = MAX
     */
    public int roundsPerMin = 1;
    
    /**
     * 连射中 每射一发对下一发的加速强度
     * 类似专注轻机枪
     * */
    public float devotionSpeed=0;
    
    //2025.12.1 发现并没有实际作用
    /**
     * For when RPM is converted to ticks - Do not use
     */
    public transient int fireTickDelay = 0;
    /**
     * The number of bullets to fire per burst in burst mode
     */
    public int numBurstRounds = 3;


    public boolean isEnergyGun = false;

    //Recoil Variables
    /**
     * Base value for Upwards cursor/view recoil
     */
    public float recoilPitch = 10.0F;
    /**
     * Base value for Left/Right cursor/view recoil
     */
    public float recoilYaw = 1.0F;
    /**
     * Modifier for setting the maximum pitch divergence when randomizing recoil (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range)
     */

    /**
     * Factor of accuracy when sneaking
     */
    public float accuracySneakFactor = 0.75f;
    
    /**
     * Factor of accuracy when crawling
     */
    public float accuracyCrawlFactor = 0.75f;
    
    /**
     * Factor of accuracy when aimming in first person
     */
    public float accuracyAimFactor = 0.75f;
    
    /**
     * Factor of accuracy when aimming in third person
     */
    public float accuracyThirdAimFactor=0.65f;
    /**
     * 移动时偏移
     * */
    public float accuracyMoveOffset=0.75f;
    /**
     * 奔跑时偏移
     * */
    public float accuracySprintOffset=0.25f;
    /**
     * 浮空时偏移
     * */
    public float accuracyHoverOffset=1.5f;
    

    public float randomRecoilPitch = 0.5F;

    /**
     * Modifier for setting the maximum yaw divergence when randomizing recoil (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range)
     * the first line is outdated;
     */
    public float randomRecoilYaw = 0.5F;

    /**
     * Modifier for setting the maximum yaw divergence when randomizing recoil (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range)
     * the first line is outdated;
     */
    //2025.12.1发现并没有实际作用
    public float recoilAimReducer = 0.8F;
    
    public float recoilCrawlYawFactor = 0.5F;
    public float recoilCrawlPitchFactor = 0.5F;

    public float antiRecoilFactor = 6.F;
    public int antiRecoilStartTime = 40;

    /**
     * The firing modes of the gun. SEMI, FULL, BURST
     */
    public WeaponFireMode[] fireModes = new WeaponFireMode[]{WeaponFireMode.SEMI};

    /**
     * Attachment Types
     */
    public HashMap<AttachmentPresetEnum, ArrayList<String>> acceptedAttachments;
    
    public HashMap<AttachmentPresetEnum, String> defaultAttachments;

    /**
     * 配件前置要求
     * key: 配件的internalName
     * value: 该配件在此枪械上需要的前置配件internalName列表
     */
    public HashMap<String, String[]> attachmentRequirements;

    /**
     * 物品创建时自带的默认弹药或弹匣
     */
    public String defaultBullet;
    public String defaultAmmo;

    // Reload Variables
    /**
     * The time (in ticks) it takes to reload this gun
     */
    public int reloadTime = 40;
    /**
     * The time (in ticks) it takes to offhand reload this gun
     */
    public Integer offhandReloadTime;

    // Ammo Variables
    /**
     * Ammo types which can be used in the gun
     */
    public String[] acceptedAmmo;

    public int chamberCapacity = 1;

    public boolean dropBulletCasing = true;

    @SideOnly(value = Side.CLIENT)
    public ModelBiped.ArmPose armPose;

    @SideOnly(value = Side.CLIENT)
    public ModelBiped.ArmPose armPoseAiming;

    /**
     * If true && != null, ammo staticModel will be set by ammo type used. Used built-in ammo staticModel by default
     */
    public boolean dynamicAmmo = false;

    // Bullet Variables
    public Integer internalAmmoStorage;
    public String[] acceptedBullets;
    // Misc Settings
    public boolean allowEquipSounds = true;
    // Misc Settings
    public boolean drawForce = false;
    // Misc Settings
    public boolean allowSprintFiring = true;

    //是否启用新后坐力反馈
    public boolean useNewRecoilSystem = false;

    public float viewKickPitchScale = 0.1f;

    public float viewKickYawScale = 0.1f;

    //Only Enhanced ASM
    public boolean allowReloadFiring = false;

    //Only Enhanced ASM
    public boolean allowReloadingSprint=true;

    //Only Enhanced ASM
    public boolean allowFiringSprint=true;

    //Only Enhanced ASM
    public boolean allowAimingSprint = true;

    /** 是否使用增强瞄准系统 */
    public boolean useEnhancedAiming = true;

    /**
     * Custom flash textures
     */
    public String customFlashTexture;
    public transient TextureType flashType;

    /**
     * Custom tesla textures
     */
    public String customTeslaTexture;
    public transient TextureType teslaType;

    /**
     * Custom transform wheel texture
     */
    public String customWheelTexture;
    public transient TextureType wheelType;

    /**
     * Extra Lore
     */
    public String extraLore;

    /**
     * Shell casing
     */
    public Vec3d shellEjectOffsetNormal = new Vec3d(-1.0f, 0.0f, 1.0f);//X正左负右,Y正下负上,Z正前负后
    public Vec3d shellEjectOffsetAiming = new Vec3d(0.0f, 0.12f, 1.0f);//X正左负右,Y正下负上,Z正前负后


    /**
     * Custom hands textures (enhanced models
     */
    public String customHandsTexture;
    public transient TextureType handsTextureType;
    
    public String customTrailTexture;
    public String customTrailModel;
    public boolean customTrailGlow;

    /** 是否使用特斯拉电击效果 */
    public boolean useTeslaTrails = false;

    /**
     * 枪械变换配置
     * key: 变换状态ID
     * value: 目标枪械的注册名
     */
    public HashMap<Integer, String> transformations;
    
    /**
     * 变换弹药选项配置
     * key: 变换状态ID
     * value: 弹药处理选项
     */
    public HashMap<Integer, TransformAmmoOption> transformAmmoOptions;
    
    /**
     * 形态变换所需的配件条件
     * key: 变换状态ID
     * value: 所需配件Map<AttachmentPresetEnum, String>，键为配件类型，值为配件内部名称
     */
    public HashMap<Integer, HashMap<AttachmentPresetEnum, String>> transformationRequirements;

    /**
     * 变换弹药选项类
     */
    public static class TransformAmmoOption {
        /**
         * 是否按比例转换弹药
         */
        public boolean ammoRatio = true;
        
        /**
         * 是否完全同步弹匣NBT（仅对弹匣类武器有效）
         */
        public boolean sameAmmo = false;
        
        public TransformAmmoOption() {}
        
        public TransformAmmoOption(boolean ammoRatio, boolean sameAmmo) {
            this.ammoRatio = ammoRatio;
            this.sameAmmo = sameAmmo;
        }
    }

    public static WeaponFireMode getFireMode(ItemStack heldStack) {
        if (heldStack.getTagCompound() != null) {
            NBTTagCompound nbtTagCompound = heldStack.getTagCompound();
            return nbtTagCompound.hasKey("firemode") ? WeaponFireMode.fromString(nbtTagCompound.getString("firemode")) : null;
        }
        return null;
    }

    public static void setFireMode(ItemStack heldStack, WeaponFireMode fireMode) {
        if (heldStack.getTagCompound() != null) {
            NBTTagCompound nbtTagCompound = heldStack.getTagCompound();
            nbtTagCompound.setString("firemode", fireMode.name().toLowerCase());
            heldStack.setTagCompound(nbtTagCompound);
        }
    }

    public static ItemStack getAttachment(ItemStack heldStack, AttachmentPresetEnum type) {
        if (heldStack.getTagCompound() != null) {
            NBTTagCompound nbtTagCompound = heldStack.getTagCompound();
            return nbtTagCompound.hasKey("attachment_" + type.typeName) ? new ItemStack(nbtTagCompound.getCompoundTag("attachment_" + type.typeName)) : null;
        }
        return null;
    }

    public static void addAttachment(ItemStack heldStack, AttachmentPresetEnum type, ItemStack attachment) {
        if (!(attachment.getItem() instanceof ItemAttachment)) {
            return;
        }
        if (heldStack.getTagCompound() != null) {
            NBTTagCompound nbtTagCompound = heldStack.getTagCompound();
            nbtTagCompound.setTag("attachment_" + type.typeName, attachment.writeToNBT(new NBTTagCompound()));
        }
    }

    public static void removeAttachment(ItemStack heldStack, AttachmentPresetEnum type) {
        if (heldStack.getTagCompound() != null) {
            NBTTagCompound nbtTagCompound = heldStack.getTagCompound();
            nbtTagCompound.removeTag("attachment_" + type.typeName);
        }
    }

    public boolean canAcceptAttachment(ItemStack itemStack) {
        if (itemStack == null || !(itemStack.getItem() instanceof ItemAttachment)) {
            return false;
        }
        ItemAttachment attachment = (ItemAttachment) itemStack.getItem();
        AttachmentType attachType = attachment.type;
        ArrayList<String> acceptItems = acceptedAttachments.get(attachType.attachmentType);
        return acceptItems != null && acceptItems.contains(attachType.internalName);
    }

    public static List<String> checkRequiredAttachments(ItemStack gunStack, GunType gunType, AttachmentType attachType) {
        List<String> missingAttachments = new ArrayList<>();
        
        if (gunType.attachmentRequirements == null) {
            return missingAttachments;
        }
        
        String[] requiredAttachments = gunType.attachmentRequirements.get(attachType.internalName);
        if (requiredAttachments == null || requiredAttachments.length == 0) {
            return missingAttachments;
        }
        
        for (String requiredAttachName : requiredAttachments) {
            boolean found = false;
            
            for (AttachmentPresetEnum preset : AttachmentPresetEnum.values()) {
                ItemStack installedAttach = getAttachment(gunStack, preset);
                if (installedAttach != null && installedAttach.getItem() instanceof ItemAttachment) {
                    ItemAttachment installedItem = (ItemAttachment) installedAttach.getItem();
                    if (installedItem.type.internalName.equals(requiredAttachName)) {
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                missingAttachments.add(requiredAttachName);
            }
        }
        
        return missingAttachments;
    }

    @Override
    public void loadExtraValues() {
        if (maxStackSize == null)
            maxStackSize = 1;

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            armPose = ModelBiped.ArmPose.BLOCK;
            armPoseAiming = ModelBiped.ArmPose.BOW_AND_ARROW;

            if (customFlashTexture != null) {
                if (ModularWarfare.textureTypes.containsKey(customFlashTexture)) {
                    flashType = ModularWarfare.textureTypes.get(customFlashTexture);
                } else {
                    flashType = new TextureType();
                    flashType.initDefaultTextures(TextureEnumType.Flash);
                }
            } else {
                flashType = new TextureType();
                flashType.initDefaultTextures(TextureEnumType.Flash);
            }

            if (customTeslaTexture != null) {
                if (ModularWarfare.textureTypes.containsKey(customTeslaTexture)) {
                    teslaType = ModularWarfare.textureTypes.get(customTeslaTexture);
                }
            }

            if (customWheelTexture != null) {
                if (ModularWarfare.textureTypes.containsKey(customWheelTexture)) {
                    wheelType = ModularWarfare.textureTypes.get(customWheelTexture);
                }
            }

            if (customHandsTexture != null) {
                if (ModularWarfare.textureTypes.containsKey(customHandsTexture)) {
                    handsTextureType = ModularWarfare.textureTypes.get(customHandsTexture);
                }
            }
        }
        loadBaseValues();
        fireTickDelay = 1200 / roundsPerMin;
        loadWeaponSoundMap();
    }

    @Override
    public void reloadModel() {
        try {
            if (animationType == WeaponAnimationType.BASIC) {
                model = new ModelGun(ModularWarfare.getRenderConfig(this, GunRenderConfig.class), this);
            } else {
                enhancedModel = new ModelEnhancedGun(ModularWarfare.getRenderConfig(this, GunEnhancedRenderConfig.class), this);
                ((GunEnhancedRenderConfig)enhancedModel.config).extra.preloadDynamicTexture();
            }  
        }catch(Throwable t) {
            ModularWarfare.LOGGER.warn("Something is going wrong when reloading model:"+internalName);
            t.printStackTrace();
            FMLCommonHandler.instance().exitJava(0, false);
        }
    }

    public boolean hasFireMode(WeaponFireMode fireMode) {
        if (fireModes != null) {
            for (int i = 0; i < fireModes.length; i++) {
                if (fireModes[i] == fireMode) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getAssetDir() {
        return "guns";
    }
}