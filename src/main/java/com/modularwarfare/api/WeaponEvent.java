package com.modularwarfare.api;

import com.modularwarfare.common.guns.ItemGun;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

public class WeaponEvent extends Event {

    private final EntityLivingBase entityLivingBase;
    private final ItemStack stackWeapon;
    private final ItemGun itemWeapon;

    public WeaponEvent(EntityLivingBase entityLivingBase, ItemStack stackWeapon, ItemGun itemWeapon) {
        this.entityLivingBase = entityLivingBase;
        this.stackWeapon = stackWeapon;
        this.itemWeapon = itemWeapon;
    }

    public EntityLivingBase getWeaponUser() {
        return entityLivingBase;
    }

    public ItemStack getWeaponStack() {
        return stackWeapon;
    }

    public ItemGun getWeaponItem() {
        return itemWeapon;
    }

}
