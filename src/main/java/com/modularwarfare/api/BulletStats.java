package com.modularwarfare.api;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.BulletProperty;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAmmo;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.PotionEntry;
import com.modularwarfare.common.guns.WeaponType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 子弹属性 API：独立按注册名/物品栈查询 {@link BulletType} 统计，以及与枪械（发射器/投掷）参考弹合并。
 *
 * <p>统计键与 {@link BulletStatsEvent} 中写入的一致；枪械合并时额外包含 {@code referenceBullet}、{@code scaledDirectDamage}。</p>
 */
public final class BulletStats {

    private BulletStats() {
    }

    // -------------------------------------------------------------------------
    // 独立子弹：注册名 / 手持物品
    // -------------------------------------------------------------------------

    @Nullable
    public static ItemBullet getBulletItem(String internalName) {
        return ModularWarfare.bulletTypes.get(internalName);
    }

    @Nullable
    public static BulletType getBulletType(String internalName) {
        ItemBullet item = getBulletItem(internalName);
        return item != null ? item.type : null;
    }

    /**
     * 物品栈为子弹时返回对应 {@link ItemBullet}，否则 null。
     */
    @Nullable
    public static ItemBullet getBulletItemFromStack(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof ItemBullet) {
            return (ItemBullet) stack.getItem();
        }
        return null;
    }

    /**
     * 通过子弹注册名获取完整属性 Map；子弹不存在返回 null。
     */
    @Nullable
    public static HashMap<String, Object> getBulletStats(String internalName) {
        ItemBullet itemBullet = getBulletItem(internalName);
        if (itemBullet == null) {
            return null;
        }
        return buildBulletStatsMap(itemBullet);
    }

    /**
     * 获取子弹的单个属性键值。
     */
    @Nullable
    public static Object getBulletStat(String internalName, String statKey) {
        HashMap<String, Object> stats = getBulletStats(internalName);
        if (stats == null) {
            return null;
        }
        return stats.get(statKey);
    }

    /**
     * 主手为子弹物品时返回其属性 Map，否则 null。
     */
    @Nullable
    public static HashMap<String, Object> getHeldBulletStats(EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        ItemBullet itemBullet = getBulletItemFromStack(held);
        if (itemBullet == null) {
            return null;
        }
        return buildBulletStatsMap(itemBullet);
    }

    /**
     * 与 {@link BulletStatsEvent#getStats()} 等价；直接写入 Map，避免 Event 与工具类相互递归调用。
     */
    public static HashMap<String, Object> buildBulletStatsMap(ItemBullet itemBullet) {
        HashMap<String, Object> map = new HashMap<>();
        if (itemBullet != null && itemBullet.type != null) {
            appendBulletTypeStats(map, itemBullet.type);
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // 枪械上下文：发射器 / 投掷参考弹
    // -------------------------------------------------------------------------

    /**
     * 发射器 / 投掷：优先当前栈上已装填子弹；否则 {@link GunType#acceptedBullets} 首项；
     * 再否则 {@link GunType#acceptedAmmo} 首弹匣的 {@link com.modularwarfare.common.guns.AmmoType#subAmmo} 首项。
     */
    @Nullable
    public static BulletType resolveReferenceBullet(@Nullable ItemStack stackWeapon, GunType gunType) {
        if (stackWeapon != null && !stackWeapon.isEmpty()) {
            ItemBullet used = ItemGun.getUsedBullet(stackWeapon, gunType);
            if (used != null && used.type != null) {
                return used.type;
            }
        }
        if (gunType.acceptedBullets != null && gunType.acceptedBullets.length > 0) {
            String id = gunType.acceptedBullets[0];
            if (id != null) {
                ItemBullet itemBullet = ModularWarfare.bulletTypes.get(id);
                if (itemBullet != null && itemBullet.type != null) {
                    return itemBullet.type;
                }
            }
        }
        if (gunType.acceptedAmmo != null && gunType.acceptedAmmo.length > 0) {
            String ammoId = gunType.acceptedAmmo[0];
            if (ammoId != null) {
                ItemAmmo itemAmmo = ModularWarfare.ammoTypes.get(ammoId);
                if (itemAmmo != null && itemAmmo.type != null && itemAmmo.type.subAmmo != null && itemAmmo.type.subAmmo.length > 0) {
                    String subId = itemAmmo.type.subAmmo[0];
                    if (subId != null) {
                        ItemBullet itemBullet = ModularWarfare.bulletTypes.get(subId);
                        if (itemBullet != null && itemBullet.type != null) {
                            return itemBullet.type;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 按武器注册名解析其参考 {@link BulletType}（无栈、仅类型默认顺序）；无法解析返回 null。
     */
    @Nullable
    public static BulletType resolveReferenceBulletForWeapon(String weaponInternalName) {
        ItemGun gun = ModularWarfare.gunTypes.get(weaponInternalName);
        if (gun == null) {
            return null;
        }
        return resolveReferenceBullet(null, gun.type);
    }

    /**
     * 若为 Launcher / Thrower 且能解析参考子弹，则将子弹统计合并进 {@code target}，并写入 {@code referenceBullet}、{@code scaledDirectDamage}。
     */
    public static void mergeLauncherThrowerBulletStats(HashMap<String, Object> target, GunType gunType, @Nullable ItemStack stackWeapon) {
        if (gunType.weaponType != WeaponType.Launcher && gunType.weaponType != WeaponType.Thrower) {
            return;
        }
        BulletType ref = resolveReferenceBullet(stackWeapon, gunType);
        if (ref == null) {
            return;
        }
        appendBulletTypeStats(target, ref, true, true);
        putLauncherWeaponMergeLegacyAliases(target, ref);
        target.put("referenceBullet", ref.internalName);
        target.put("scaledDirectDamage", gunType.gunDamage * ref.bulletDamageFactor);
    }

    /**
     * 写入与早期版本一致的 {@code bullet*} 键，便于在完整武器 {@link WeaponStatsEvent} Map 中稳定读取。
     */
    private static void putLauncherWeaponMergeLegacyAliases(HashMap<String, Object> target, BulletType bt) {
        target.put("bulletImpactDamage", bt.impactDamage);
        target.put("bulletProjectileVelocity", bt.projectileVelocity);
        target.put("bulletExplosionDamage", bt.explosionDamage);
        target.put("bulletExplosionRange", bt.explosionRange);
        target.put("bulletExplosionKnockback", bt.explosionKnockback);
        target.put("bulletGravity", bt.gravity);
        target.put("bulletIsExplosion", bt.isExplosion);
        target.put("bulletIsSmoke", bt.isSmoke);
        target.put("bulletDamageWorld", bt.damageWorld);
        target.put("bulletAllowBlockDrops", bt.allowBlockDrops);
        target.put("bulletCausesFire", bt.causesFire);
        target.put("bulletShooterVulnerable", bt.shooterVulnerable);
        target.put("bulletIsFireDamage", bt.isFireDamage);
        target.put("bulletIsAbsoluteDamage", bt.isAbsoluteDamage);
        target.put("bulletIsBypassesArmorDamage", bt.isBypassesArmorDamage);
        target.put("bulletIsExplosionDamage", bt.isExplosionDamage);
        target.put("bulletIsMagicDamage", bt.isMagicDamage);
        target.put("bulletIsSlug", bt.isSlug);
    }

    /**
     * 仅子弹相关条目（不含枪械其它字段）；非发射器/投掷或无法解析时返回空 Map。
     */
    public static HashMap<String, Object> buildLauncherThrowerBulletStatsMap(GunType gunType, @Nullable ItemStack stackWeapon) {
        HashMap<String, Object> map = new HashMap<>();
        mergeLauncherThrowerBulletStats(map, gunType, stackWeapon);
        return map;
    }

    // -------------------------------------------------------------------------
    // 写入 Map（供事件与其它合并逻辑复用）
    // -------------------------------------------------------------------------

    /**
     * 将 {@link BulletType} 的可序列化统计字段写入 {@code target}（不写入 referenceBullet / scaledDirectDamage）。
     *
     * @param omitIdentityFields      为 true 时省略 {@code internalName}、{@code displayName}、{@code maxStackSize}、{@code toolipScript}（合并进武器 Map 时使用）。
     * @param omitBulletPropertyDetail 为 true 时仍写入 {@code bulletPropertyHitTypes}、{@code bulletPropertyCount}，但不写入 {@code bulletPropertiesDetail} 嵌套表。
     */
    public static void appendBulletTypeStats(HashMap<String, Object> target, BulletType bt, boolean omitIdentityFields, boolean omitBulletPropertyDetail) {
        if (bt == null) {
            return;
        }

        if (!omitIdentityFields) {
            target.put("internalName", bt.internalName);
            target.put("displayName", bt.displayName);
            target.put("maxStackSize", bt.maxStackSize);
            target.put("toolipScript", bt.toolipScript);
        }

        target.put("bulletDamageFactor", bt.bulletDamageFactor);
        target.put("bulletAccuracyFactor", bt.bulletAccuracyFactor);
        target.put("bulletPenetrateFactor", bt.bulletPenetrateFactor);
        target.put("bulletBlockPenetrateFactor", bt.bulletBlockPenetrateFactor);
        target.put("isSlug", bt.isSlug);

        target.put("isFireDamage", bt.isFireDamage);
        target.put("isAbsoluteDamage", bt.isAbsoluteDamage);
        target.put("isBypassesArmorDamage", bt.isBypassesArmorDamage);
        target.put("isExplosionDamage", bt.isExplosionDamage);
        target.put("isMagicDamage", bt.isMagicDamage);

        target.put("renderBulletModel", bt.renderBulletModel);
        target.put("shellModelFileName", bt.shellModelFileName);
        target.put("shellSound", bt.shellSound);

        target.put("impactDamage", bt.impactDamage);
        target.put("projectileVelocity", bt.projectileVelocity);
        target.put("explosionDamage", bt.explosionDamage);
        target.put("explosionRange", bt.explosionRange);
        target.put("explosionKnockback", bt.explosionKnockback);
        target.put("gravity", bt.gravity);
        target.put("isSmoke", bt.isSmoke);
        target.put("isExplosion", bt.isExplosion);
        target.put("damageWorld", bt.damageWorld);
        target.put("allowBlockDrops", bt.allowBlockDrops);
        target.put("causesFire", bt.causesFire);
        target.put("shooterVulnerable", bt.shooterVulnerable);

        target.put("isDynamicBullet", bt.isDynamicBullet);
        target.put("sameTextureAsGun", bt.sameTextureAsGun);
        target.put("customExplosionModel", bt.customExplosionModel);
        target.put("customExplosionTexture", bt.customExplosionTexture);
        target.put("trailModel", bt.trailModel);
        target.put("trailTex", bt.trailTex);
        target.put("trailGlow", bt.trailGlow);
        target.put("particleMaxAge", bt.particleMaxAge);

        appendBulletPropertiesStats(target, bt, omitBulletPropertyDetail);
    }

    /**
     * 完整快照（含身份字段与按命中部位拆分的 {@code bulletPropertiesDetail}）。
     */
    public static void appendBulletTypeStats(HashMap<String, Object> target, BulletType bt) {
        appendBulletTypeStats(target, bt, false, false);
    }

    private static void appendBulletPropertiesStats(HashMap<String, Object> target, BulletType bt, boolean omitDetail) {
        Map<String, BulletProperty> props = bt.bulletProperties;
        if (props == null || props.isEmpty()) {
            target.put("bulletPropertyHitTypes", Collections.emptyList());
            target.put("bulletPropertiesDetail", Collections.emptyMap());
            target.put("bulletPropertyCount", 0);
            return;
        }
        List<String> keys = new ArrayList<>(props.keySet());
        Collections.sort(keys);
        target.put("bulletPropertyHitTypes", keys);
        target.put("bulletPropertyCount", keys.size());

        if (omitDetail) {
            target.put("bulletPropertiesDetail", Collections.emptyMap());
            return;
        }

        HashMap<String, HashMap<String, Object>> detail = new HashMap<>();
        for (String hitType : keys) {
            BulletProperty p = props.get(hitType);
            if (p == null) {
                continue;
            }
            HashMap<String, Object> row = new HashMap<>();
            row.put("bulletDamageFactor", p.bulletDamageFactor);
            row.put("fireLevel", p.fireLevel);
            row.put("explosionLevel", p.explosionLevel);
            row.put("explosionBroken", p.explosionBroken);
            row.put("explosionOnBlock", p.explosionOnBlock);
            row.put("knockLevel", p.knockLevel);
            row.put("knockVerticalLevel", p.knockVerticalLevel);
            row.put("banShield", p.banShield);
            row.put("potionEffectCount", p.potionEffects == null ? 0 : p.potionEffects.length);
            row.put("potionEffects", summarizePotionEffects(p.potionEffects));
            detail.put(hitType, row);
        }
        target.put("bulletPropertiesDetail", detail);
    }

    private static List<HashMap<String, Object>> summarizePotionEffects(@Nullable PotionEntry[] entries) {
        if (entries == null || entries.length == 0) {
            return Collections.emptyList();
        }
        List<HashMap<String, Object>> list = new ArrayList<>();
        for (PotionEntry e : entries) {
            if (e == null) {
                continue;
            }
            HashMap<String, Object> one = new HashMap<>();
            one.put("potionEffect", e.potionEffect != null ? e.potionEffect.name() : null);
            one.put("duration", e.duration);
            one.put("level", e.level);
            list.add(one);
        }
        return list;
    }
}
