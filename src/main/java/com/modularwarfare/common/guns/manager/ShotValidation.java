package com.modularwarfare.common.guns.manager;

import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.guns.WeaponFireMode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

//@SideOnly(Side.SERVER)
public class ShotValidation {

    public static boolean isValidShoot(final long clientFireTickDelay, final float recoilPitch, final float recoilYaw, final float recoilAimReducer, final float bulletSpread, GunType type) {
        return (clientFireTickDelay == type.fireTickDelay) && (type.recoilPitch == recoilPitch) && (type.recoilYaw == recoilYaw) && (type.recoilAimReducer == recoilAimReducer) && (type.bulletSpread == bulletSpread);
    }

    public static boolean verifShot(EntityPlayer entityPlayer, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode, final int clientFireTickDelay, final float recoilPitch, final float recoilYaw, final float recoilAimReducer, final float bulletSpread) {
        GunType gunType = itemGun.type;
        if (entityPlayer.isSpectator()) {
            return false;
        }
        // Can fire checks
        if (isValidShoot(clientFireTickDelay, recoilPitch, recoilYaw, recoilAimReducer, bulletSpread, itemGun.type)) {
            if (itemGun.type.animationType == WeaponAnimationType.BASIC) {
                if (ItemGun.isServerReloading(entityPlayer)) {
                    return false;
                }
            }
            return (itemGun.type.allowSprintFiring || !entityPlayer.isSprinting()) && itemGun.type.hasFireMode(fireMode);
        }
        return true;
    }

}
