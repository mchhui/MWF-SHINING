package com.modularwarfare.api;

import java.util.ArrayList;

import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.manager.FireManager;
import com.modularwarfare.common.guns.manager.GunKickManager;
import com.modularwarfare.common.network.PacketGunFire;
import com.modularwarfare.utility.raycast.hits.BulletHit;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-side bridge for skill actions that must use the native player fire result. */
@SideOnly(Side.CLIENT)
public final class ClientWeaponFireAPI {
    private ClientWeaponFireAPI() {}

    public static ArrayList<PacketGunFire.Hit> collectHitSuggestions(EntityPlayer player, ItemGun gun) {
        ArrayList<PacketGunFire.Hit> result = new ArrayList<>();
        if (player == null || gun == null) return result;
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || held.getItem() != gun) return result;
        GunKickManager.AimingData aiming = GunKickManager.getAimingData(player);
        aiming.updateForced(player, gun);
        for (BulletHit hit : aiming.rayTraceList) {
            if (hit == null || hit.rayTraceResult == null || hit.rayTraceResult.hitVec == null) continue;
            PacketGunFire.Hit suggestion = new PacketGunFire.Hit();
            suggestion.victimEntityId = hit.rayTraceResult.entityHit == null ? -1 : hit.rayTraceResult.entityHit.getEntityId();
            suggestion.hitboxType = FireManager.getHitBoxName(hit);
            suggestion.remainingPenetrate = hit.remainingPenetrate;
            suggestion.remainingBlockPenetrate = hit.remainingBlockPenetrate;
            suggestion.distance = hit.distance;
            suggestion.hitX = hit.rayTraceResult.hitVec.x;
            suggestion.hitY = hit.rayTraceResult.hitVec.y;
            suggestion.hitZ = hit.rayTraceResult.hitVec.z;
            suggestion.facing = hit.rayTraceResult.sideHit;
            result.add(suggestion);
            if (result.size() >= 64) break;
        }
        return result;
    }

}
