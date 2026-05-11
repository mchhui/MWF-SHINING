package com.modularwarfare.api;

import com.modularwarfare.common.guns.ItemGun;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.HashMap;

/**
 * 武器属性获取事件；统计内容由 {@link WeaponStats#buildWeaponStatsMap(ItemGun, ItemStack)} 生成。
 */
public class WeaponStatsEvent extends WeaponEvent {

    private final HashMap<String, Object> stats;

    public WeaponStatsEvent(EntityPlayer entityPlayer, ItemStack stackWeapon, ItemGun itemWeapon) {
        super(entityPlayer, stackWeapon, itemWeapon);
        this.stats = WeaponStats.buildWeaponStatsMap(itemWeapon, stackWeapon);
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