package com.modularwarfare.api;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.common.guns.ItemGun;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

import java.util.HashMap;

/**
 * 武器属性工具类；子弹独立查询见 {@link #getBulletStats(String)}、{@link BulletStats}。
 *
 * <p>枪械统计键的写入实现在 {@link #appendWeaponStats(HashMap, GunType, ItemStack)}；{@link WeaponStatsEvent} 仅委托 {@link #buildWeaponStatsMap(ItemGun, ItemStack)}。</p>
 */
public class WeaponStats {

    /**
     * 将一把枪的统计写入 {@code target}（含发射器/投掷参考子弹合并）。
     */
    public static void appendWeaponStats(HashMap<String, Object> target, GunType type, @Nullable ItemStack stackWeapon) {
        target.put("damage", type.gunDamage);
        target.put("fireRate", type.roundsPerMin);
        target.put("reloadTime", type.reloadTime);
        target.put("maxAmmo", type.internalAmmoStorage);
        target.put("bulletSpread", type.bulletSpread);
        target.put("maxRange", type.weaponMaxRange);
        target.put("effectiveRange", type.weaponEffectiveRange);
        target.put("devotionSpeed", type.devotionSpeed);
        target.put("moveSpeedModifier", type.moveSpeedModifier);
        target.put("gunDamageHeadshotBonus", type.gunDamageHeadshotBonus);
        target.put("gunPenetrateSize", type.gunPenetrateSize);
        target.put("gunPenetrationDamageFalloff", type.gunPenetrationDamageFalloff);
        target.put("gunMaxPenetrateBlockResistance", type.gunMaxPenetrateBlockResistance);
        target.put("gunPenetrateBlocksResistance", type.gunPenetrateBlocksResistance);
        target.put("gunPenetrateBlocksDamageFalloffFactor", type.gunPenetrateBlocksDamageFalloffFactor);

        target.put("recoilPitch", type.recoilPitch);
        target.put("recoilYaw", type.recoilYaw);
        target.put("recoilAimReducer", type.recoilAimReducer);
        target.put("recoilCrawlYawFactor", type.recoilCrawlYawFactor);
        target.put("recoilCrawlPitchFactor", type.recoilCrawlPitchFactor);
        target.put("antiRecoilFactor", type.antiRecoilFactor);
        target.put("antiRecoilStartTime", type.antiRecoilStartTime);
        target.put("randomRecoilPitch", type.randomRecoilPitch);
        target.put("randomRecoilYaw", type.randomRecoilYaw);

        target.put("accuracyAimFactor", type.accuracyAimFactor);
        target.put("accuracyThirdAimFactor", type.accuracyThirdAimFactor);
        target.put("accuracySneakFactor", type.accuracySneakFactor);
        target.put("accuracyCrawlFactor", type.accuracyCrawlFactor);
        target.put("accuracyMoveOffset", type.accuracyMoveOffset);
        target.put("accuracySprintOffset", type.accuracySprintOffset);
        target.put("accuracyHoverOffset", type.accuracyHoverOffset);

        target.put("weaponType", type.weaponType);

        BulletStats.mergeLauncherThrowerBulletStats(target, type, stackWeapon);
    }

    /**
     * 新建 Map 并写入完整枪械统计（与 {@link WeaponStatsEvent#getStats()} 一致）。
     */
    public static HashMap<String, Object> buildWeaponStatsMap(ItemGun itemGun, @Nullable ItemStack stackWeapon) {
        HashMap<String, Object> map = new HashMap<>();
        appendWeaponStats(map, itemGun.type, stackWeapon);
        return map;
    }

    /**
     * 通过武器注册名获取武器属性
     * @param internalName 武器注册名
     * @return 武器属性Map,如果武器不存在则返回null
     */
    public static HashMap<String, Object> getWeaponStats(String internalName) {
        ItemGun itemGun = ModularWarfare.gunTypes.get(internalName);
        if(itemGun == null) {
            return null;
        }
        return buildWeaponStatsMap(itemGun, null);
    }
    
    /**
     * 获取指定武器的特定属性值
     * @param internalName 武器注册名
     * @param statKey 属性名
     * @return 属性值,如果武器不存在则返回null
     */
    public static Object getWeaponStat(String internalName, String statKey) {
        HashMap<String, Object> stats = getWeaponStats(internalName);
        if(stats == null) {
            return null;
        }
        return stats.get(statKey);
    }
    
    /**
     * 获取玩家当前手持武器的属性
     * @param player 玩家实体
     * @return 武器属性Map,如果玩家未手持武器则返回null
     */
    public static HashMap<String, Object> getHeldWeaponStats(EntityPlayer player) {
        ItemStack heldItem = player.getHeldItemMainhand();
        if(heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemGun)) {
            return null;
        }
        
        ItemGun itemGun = (ItemGun)heldItem.getItem();
        return buildWeaponStatsMap(itemGun, heldItem);
    }

    /**
     * 按武器注册名获取发射器/投掷类武器的参考子弹统计子集（与 {@link #getWeaponStats} 中合并的子弹键一致）。
     *
     * @param internalName 武器注册名
     * @return 子弹相关 Map；武器不存在返回 null；非发射器/投掷或无法解析参考弹时返回空 Map
     */
    public static HashMap<String, Object> getWeaponBulletStats(String internalName) {
        ItemGun itemGun = ModularWarfare.gunTypes.get(internalName);
        if (itemGun == null) {
            return null;
        }
        return BulletStats.buildLauncherThrowerBulletStatsMap(itemGun.type, null);
    }

    /**
     * 玩家主手为枪时，获取当前栈对应的参考子弹统计子集。
     */
    public static HashMap<String, Object> getHeldWeaponBulletStats(EntityPlayer player) {
        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemGun)) {
            return null;
        }
        ItemGun itemGun = (ItemGun) heldItem.getItem();
        return BulletStats.buildLauncherThrowerBulletStatsMap(itemGun.type, heldItem);
    }

    /**
     * 获取指定武器在子弹子集中的单项属性（键名与完整武器统计中的子弹键相同）。
     */
    public static Object getWeaponBulletStat(String internalName, String statKey) {
        HashMap<String, Object> map = getWeaponBulletStats(internalName);
        if (map == null) {
            return null;
        }
        return map.get(statKey);
    }

    /**
     * 按武器注册名、无 ItemStack 时解析其参考子弹类型（与 {@link BulletStats#resolveReferenceBulletForWeapon} 相同）。
     */
    @Nullable
    public static BulletType getWeaponResolvedReferenceBullet(String weaponInternalName) {
        return BulletStats.resolveReferenceBulletForWeapon(weaponInternalName);
    }

    /**
     * 主手为枪时，按当前栈解析参考 {@link BulletType}（含已装填弹药逻辑）。
     */
    @Nullable
    public static BulletType getHeldWeaponResolvedReferenceBullet(EntityPlayer player) {
        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemGun)) {
            return null;
        }
        ItemGun itemGun = (ItemGun) heldItem.getItem();
        return BulletStats.resolveReferenceBullet(heldItem, itemGun.type);
    }

    // -------------------------------------------------------------------------
    // 子弹物品（独立于枪械）
    // -------------------------------------------------------------------------

    /**
     * 按子弹注册名获取完整属性 Map（与 {@link BulletStats#getBulletStats} 相同）。
     */
    @Nullable
    public static HashMap<String, Object> getBulletStats(String bulletInternalName) {
        return BulletStats.getBulletStats(bulletInternalName);
    }

    /**
     * 主手为子弹物品时返回其属性 Map。
     */
    @Nullable
    public static HashMap<String, Object> getHeldBulletStats(EntityPlayer player) {
        return BulletStats.getHeldBulletStats(player);
    }

    /**
     * 获取子弹单项属性。
     */
    @Nullable
    public static Object getBulletStat(String bulletInternalName, String statKey) {
        return BulletStats.getBulletStat(bulletInternalName, statKey);
    }

    /**
     * 解析已注册的 {@link com.modularwarfare.common.guns.ItemBullet}；不存在返回 null。
     */
    @Nullable
    public static ItemBullet getBulletItem(String bulletInternalName) {
        return BulletStats.getBulletItem(bulletInternalName);
    }

    /**
     * 解析已注册的 {@link BulletType}；不存在返回 null。
     */
    @Nullable
    public static BulletType getBulletType(String bulletInternalName) {
        return BulletStats.getBulletType(bulletInternalName);
    }
} 