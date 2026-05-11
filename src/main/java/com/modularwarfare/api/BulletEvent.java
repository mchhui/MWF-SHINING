package com.modularwarfare.api;

import com.modularwarfare.common.guns.ItemBullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 与子弹物品相关的事件基类（供 {@link BulletStatsEvent} 等扩展）。
 */
public class BulletEvent extends Event {

    private final EntityLivingBase entityLivingBase;
    private final ItemStack stackBullet;
    private final ItemBullet itemBullet;

    public BulletEvent(EntityLivingBase entityLivingBase, ItemStack stackBullet, ItemBullet itemBullet) {
        this.entityLivingBase = entityLivingBase;
        this.stackBullet = stackBullet;
        this.itemBullet = itemBullet;
    }

    public EntityLivingBase getBulletHolder() {
        return entityLivingBase;
    }

    public ItemStack getBulletStack() {
        return stackBullet;
    }

    public ItemBullet getBulletItem() {
        return itemBullet;
    }
}
