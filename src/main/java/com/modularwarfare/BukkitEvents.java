package com.modularwarfare;

import com.modularwarfare.api.EntityHeadShotEvent;
import com.modularwarfare.api.GunHitEntityEvent;
import com.modularwarfare.api.WeaponAttachmentEvent;
import com.modularwarfare.api.WeaponHitEvent;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import static com.modularwarfare.NMSHelper.*;

/**
 * 开发环境可用-Dmwf.banbukkit=true禁用
 * */
public class BukkitEvents {
    public static class BukkitGunHitEntityEvent extends Event {
        public static final HandlerList handlerList = new HandlerList();

        public org.bukkit.entity.Entity shooter;
        public org.bukkit.entity.Entity victim;
        public final String gunId;
        public final String hitbox;
        public final double hitX;
        public final double hitY;
        public final double hitZ;
        public final boolean isHeadshot;
        public float damage;
        public boolean isCanceled = false;

        public BukkitGunHitEntityEvent(org.bukkit.entity.Entity shooter, org.bukkit.entity.Entity victim, String gunId, String hitbox, double hitX, double hitY, double hitZ, float damage, boolean isHeadshot) {
            this.shooter = shooter;
            this.victim = victim;
            this.gunId = gunId;
            this.hitbox = hitbox;
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitZ = hitZ;
            this.damage = damage;
            this.isHeadshot = isHeadshot;
        }

        public void setCanceled(boolean isCanceled) {
            this.isCanceled = isCanceled;
        }

        public boolean isCanceled() {
            return isCanceled;
        }

        @Override
        public HandlerList getHandlers() {
            return handlerList;
        }

        public static HandlerList getHandlerList() {
            return handlerList;
        }
    }

    public static class BukkitEntityHeadShotEvent extends Event {
        public static final HandlerList handlerList = new HandlerList();

        public org.bukkit.entity.Entity victim;
        public org.bukkit.entity.Entity shooter;

        public BukkitEntityHeadShotEvent(org.bukkit.entity.Entity victim, org.bukkit.entity.Entity shooter) {
            this.victim = victim;
            this.shooter = shooter;
        }

        @Override
        public HandlerList getHandlers() {
            // TODO Auto-generated method stub
            return handlerList;
        }

        public static HandlerList getHandlerList() {
            return handlerList;
        }
    }

    public static class BukkitWeaponAttachmentEvent extends Event {
        public static final HandlerList handlerList = new HandlerList();
        public final org.bukkit.entity.Player player;
        public final boolean isUnload;
        public final boolean isUnloadAll;
        public final String unloadAttachmentType;
        public final ItemStack gun;
        public ItemStack loadAttach;
        public boolean isCanceled = false;

        public BukkitWeaponAttachmentEvent(org.bukkit.entity.Player player, boolean isUnload, boolean isUnloadAll, String unloadAttachmentType, ItemStack gun, ItemStack loadAttach) {
            this.player = player;
            this.isUnload = isUnload;
            this.isUnloadAll = isUnloadAll;
            this.unloadAttachmentType = unloadAttachmentType;
            this.gun = gun;
            this.loadAttach = loadAttach;
        }

        public void setCanceled(boolean isCanceled) {
            this.isCanceled = isCanceled;
        }

        public boolean isCanceled() {
            return isCanceled;
        }

        @Override
        public HandlerList getHandlers() {
            // TODO Auto-generated method stub
            return handlerList;
        }

        public static HandlerList getHandlerList() {
            return handlerList;
        }
    }

    @SubscribeEvent
    public static void onGunHitEntity(GunHitEntityEvent event) {
        Entity shooter = toBukkitEntity(event.shooter);
        Entity victim = toBukkitEntity(event.victim);
        boolean isHeadshot = event.hitbox.contains("head");
        BukkitGunHitEntityEvent bukkitEvent = new BukkitGunHitEntityEvent(shooter, victim, event.gunId, event.hitbox, event.hitX, event.hitY, event.hitZ, event.damage, isHeadshot);
        Bukkit.getPluginManager().callEvent(bukkitEvent);
        event.damage = bukkitEvent.damage;
        if (bukkitEvent.isCanceled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHeadshot(EntityHeadShotEvent event) {
        Entity shooter = toBukkitEntity(event.getShooter());
        Entity victim = toBukkitEntity(event.getVictim());
        Bukkit.getPluginManager().callEvent(new BukkitEntityHeadShotEvent(victim, shooter));
    }

    @SubscribeEvent
    public static void onWeaponAttachment(WeaponAttachmentEvent event) {
        Player player = toBukkitPlayer(event.player);
        boolean isUnload = false;
        boolean isUnloadAll = false;
        String unloadAttachmentType = null;
        ItemStack gun = toBukkitStack(event.gun);
        ItemStack loadAttach = null;
        if (event instanceof WeaponAttachmentEvent.Unload) {
            isUnload = true;
            isUnloadAll = ((WeaponAttachmentEvent.Unload)event).unloadAll;
        }
        if (event instanceof WeaponAttachmentEvent.Load) {
            loadAttach = toBukkitStack(((WeaponAttachmentEvent.Load)event).attach);
        }
        BukkitWeaponAttachmentEvent bukkitEvent = new BukkitWeaponAttachmentEvent(player, isUnload, isUnloadAll, unloadAttachmentType, gun, loadAttach);
        Bukkit.getPluginManager().callEvent(bukkitEvent);
        if (event instanceof WeaponAttachmentEvent.Load) {
            ((WeaponAttachmentEvent.Load)event).attach = toForgeStack(bukkitEvent.loadAttach);
        }
        if (bukkitEvent.isCanceled()) {
            event.setCanceled(true);
        }
    }
}
