package com.modularwarfare.api;

import com.modularwarfare.common.guns.ItemBullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import java.util.HashMap;

/**
 * 子弹属性快照（与 {@link WeaponStatsEvent} 用法类似：构造后即得到 {@link #getStats()}）。
 *
 * <p>键集合由 {@link BulletStats#appendBulletTypeStats(HashMap, BulletType)} 定义（含身份、弹道/爆炸、渲染与 {@code bulletPropertiesDetail} 等）。</p>
 */
public class BulletStatsEvent extends BulletEvent {

    private final HashMap<String, Object> stats;

    public BulletStatsEvent(EntityLivingBase holder, ItemStack stackBullet, ItemBullet itemBullet) {
        super(holder, stackBullet, itemBullet);
        this.stats = BulletStats.buildBulletStatsMap(itemBullet);
    }

    public HashMap<String, Object> getStats() {
        return stats;
    }

    public Object getStat(String key) {
        return stats.get(key);
    }
}
