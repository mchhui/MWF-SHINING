package com.modularwarfare.api;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class WeaponEnhancedReloadEvent {

    @Cancelable
    public static class Unload extends Event {
        public final EntityPlayerMP player;
        public final ItemStack gunStack;

        public Unload(EntityPlayerMP player, ItemStack gunStack) {
            this.player = player;
            this.gunStack = gunStack;
        }


    }

    public static class SearchAmmo extends Event {

        public final EntityPlayerMP player;
        public final ItemStack gunStack;
        public ItemStack ammo;

        public SearchAmmo(EntityPlayerMP player, ItemStack gunStack, ItemStack ammo) {
            this.player = player;
            this.gunStack = gunStack;
            this.ammo = ammo;
        }

        public void setAmmo(ItemStack ammo) {
            this.ammo = ammo;
        }

    }

    public static class SearchBullet extends Event {
        public final EntityPlayerMP player;
        public final ItemStack gunStack;
        public ItemStack bullet;
        public int count;

        public SearchBullet(EntityPlayerMP player, ItemStack gunStack, ItemStack bullet, int count) {
            this.player = player;
            this.gunStack = gunStack;
            this.bullet = bullet;
            this.count = count;
        }


    }

    public static class ReloadStopFirst extends Event {
        public final EntityPlayerMP player;
        public final ItemStack gunStack;
        public final ItemStack ammo;
        public final boolean isCurrentAmmo;

        public boolean unload;

        public ReloadStopFirst(EntityPlayerMP player, ItemStack gunStack, ItemStack ammo, boolean isCurrentAmmo,
                               boolean unload) {
            this.player = player;
            this.gunStack = gunStack;
            this.ammo = ammo;
            this.isCurrentAmmo = isCurrentAmmo;
            this.unload = unload;
        }

    }

    public static class ReloadStopSecond extends Event {
        public final EntityPlayerMP player;
        public final ItemStack gunStack;
        public final ItemStack ammo;
        public final boolean isCurrentAmmo;

        public boolean result;

        public ReloadStopSecond(EntityPlayerMP player, ItemStack gunStack, ItemStack ammo, boolean isCurrentAmmo,
                                boolean result) {
            this.player = player;
            this.gunStack = gunStack;
            this.ammo = ammo;
            this.isCurrentAmmo = isCurrentAmmo;
            this.result = result;
        }

    }
}
