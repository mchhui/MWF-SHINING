package com.modularwarfare.api;

import com.modularwarfare.common.guns.AttachmentPresetEnum;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class WeaponAttachmentEvent extends Event {
    public final EntityPlayer player;
    public final ItemStack gun;

    public WeaponAttachmentEvent(EntityPlayer player, ItemStack gun) {
        this.player = player;
        this.gun = gun;
    }

    @Cancelable
    public static class Load extends WeaponAttachmentEvent {
        public ItemStack attach;

        public Load(EntityPlayer player, ItemStack gun, ItemStack attach) {
            super(player, gun);
            this.attach = attach;
        }

    }
    
    @Cancelable
    public static class Unload extends WeaponAttachmentEvent {
        public final boolean unloadAll;
        public final AttachmentPresetEnum type;
        public Unload(EntityPlayer player, ItemStack gun, AttachmentPresetEnum type,boolean unloadAll) {
            super(player, gun);
            this.type=type;
            this.unloadAll=unloadAll;
        }

    }
}
