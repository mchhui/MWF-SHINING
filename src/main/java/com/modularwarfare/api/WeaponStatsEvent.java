package com.modularwarfare.api;

import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.HashMap;

/**
 * 武器属性获取事件
 */
public class WeaponStatsEvent extends WeaponEvent {

    private final HashMap<String, Object> stats;

    public WeaponStatsEvent(EntityPlayer entityPlayer, ItemStack stackWeapon, ItemGun itemWeapon) {
        super(entityPlayer, stackWeapon, itemWeapon);
        this.stats = new HashMap<>();
        GunType type = itemWeapon.type;
        
        // 基础属性
        stats.put("damage", type.gunDamage);
        stats.put("fireRate", type.roundsPerMin);
        stats.put("reloadTime", type.reloadTime);
        stats.put("maxAmmo", type.internalAmmoStorage);
        stats.put("bulletSpread", type.bulletSpread);
        stats.put("maxRange", type.weaponMaxRange);
        stats.put("effectiveRange", type.weaponEffectiveRange);
        stats.put("devotionSpeed", type.devotionSpeed);
        stats.put("moveSpeedModifier", type.moveSpeedModifier);
        stats.put("gunDamageHeadshotBonus", type.gunDamageHeadshotBonus);
        stats.put("gunPenetrateSize", type.gunPenetrateSize);
        stats.put("gunPenetrationDamageFalloff", type.gunPenetrationDamageFalloff);
        stats.put("gunMaxPenetrateBlockResistance", type.gunMaxPenetrateBlockResistance);
        stats.put("gunPenetrateBlocksResistance", type.gunPenetrateBlocksResistance);
        stats.put("gunPenetrateBlocksDamageFalloffFactor", type.gunPenetrateBlocksDamageFalloffFactor);

        // 后坐力属性
        stats.put("recoilPitch", type.recoilPitch);
        stats.put("recoilYaw", type.recoilYaw);
        stats.put("recoilAimReducer", type.recoilAimReducer);
        stats.put("recoilCrawlYawFactor", type.recoilCrawlYawFactor);
        stats.put("recoilCrawlPitchFactor", type.recoilCrawlPitchFactor);
        stats.put("antiRecoilFactor", type.antiRecoilFactor);
        stats.put("antiRecoilStartTime", type.antiRecoilStartTime);
        stats.put("randomRecoilPitch", type.randomRecoilPitch);
        stats.put("randomRecoilYaw", type.randomRecoilYaw);

        // 精准度属性
        stats.put("accuracyAimFactor", type.accuracyAimFactor);
        stats.put("accuracyThirdAimFactor", type.accuracyThirdAimFactor);
        stats.put("accuracySneakFactor", type.accuracySneakFactor);
        stats.put("accuracyCrawlFactor", type.accuracyCrawlFactor);
        stats.put("accuracyMoveOffset", type.accuracyMoveOffset);
        stats.put("accuracySprintOffset", type.accuracySprintOffset);
        stats.put("accuracyHoverOffset", type.accuracyHoverOffset);
        
        // 其他属性
        stats.put("weaponType", type.weaponType);
    }

    /**
     * 获取武器所有属性
     * @return 武器属性Map
     */
    public HashMap<String, Object> getStats() {
        return stats;
    }

    /**
     * 获取指定属性值
     * @param key 属性名
     * @return 属性值
     */
    public Object getStat(String key) {
        return stats.get(key);
    }
} 