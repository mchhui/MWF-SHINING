package com.modularwarfare.api;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.ItemGun;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.HashMap;

/**
 * 武器属性工具类
 */
public class WeaponStats {

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
        
        WeaponStatsEvent event = new WeaponStatsEvent(null, null, itemGun);
        return event.getStats();
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
        WeaponStatsEvent event = new WeaponStatsEvent(player, heldItem, itemGun);
        return event.getStats();
    }
} 