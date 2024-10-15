package com.modularwarfare;

import com.modularwarfare.api.EntityHeadShotEvent;
import com.modularwarfare.api.WeaponAttachmentEvent;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class BukkitHelper {
    public static ScriptEngine scriptEngine = new NashornScriptEngineFactory().getScriptEngine();
    static {
        try {
            // var Bukkit=Java.type("org.bukkit.Bukkit");
            // var BukkitEntityHeadShotEvent=Java.type("com.modularwarfare.BukkitHelper.BukkitEntityHeadShotEvent");
            // var BukkitWeaponAttachmentEvent=Java.type("com.modularwarfare.BukkitHelper.BukkitWeaponAttachmentEvent");
            // var CraftEntity=Java.type("org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity");
            // var CraftItemStack=Java.type("org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack");
            // function toBukkitEntity(entity){
            // return CraftEntity.getEntity(Bukkit.getServer(),entity);
            // }
            // function toBukkitItemstack(stack){
            // return CraftItemStack.asBukkitCopy(stack);
            // }
            // function toForgeItemstack(stack){
            // return CraftItemStack.asNMSCopy(stack);
            // }
            scriptEngine.eval("var Bukkit=Java.type(\"org.bukkit.Bukkit\");\r\n"
                + "var BukkitEntityHeadShotEvent=Java.type(\"com.modularwarfare.BukkitHelper.BukkitEntityHeadShotEvent\");\r\n"
                + "var BukkitWeaponAttachmentEvent=Java.type(\"com.modularwarfare.BukkitHelper.BukkitWeaponAttachmentEvent\");\r\n"
                + "var CraftEntity=Java.type(\"org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity\");\r\n"
                + "var CraftItemStack=Java.type(\"org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack\");\r\n"
                + "function toBukkitEntity(entity){\r\n"
                + "    return CraftEntity.getEntity(Bukkit.getServer(),entity);\r\n" + "}\r\n"
                + "function toBukkitItemstack(stack){\r\n" + "    return CraftItemStack.asBukkitCopy(stack);\r\n"
                + "}\r\n" + "function toForgeItemstack(stack){\r\n" + "    return CraftItemStack.asNMSCopy(stack);\r\n"
                + "}");
        } catch (ScriptException e) {
            throw new RuntimeException();
        }
    }

    @SubscribeEvent
    public static void onEntityHeadShot(EntityHeadShotEvent event) {
        scriptEngine.put("event", event);
        try {
            scriptEngine.eval(
                "    var bukkitEvent=new BukkitEntityHeadShotEvent(toBukkitEntity(event.getVictim()),toBukkitEntity(event.getShooter()));\r\n"
                + "    Bukkit.getPluginManager().callEvent(bukkitEvent);");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        // var bukkitEvent=new
        // BukkitEntityHeadShotEvent(toBukkitEntity(event.getVictim()),toBukkitEntity(event.getShooter()));
        // Bukkit.getPluginManager().callEvent(bukkitEvent);
    }

    @SubscribeEvent
    public static void onAttachmentLoad(WeaponAttachmentEvent.Load event) {
        scriptEngine.put("event", event);
        try {
            scriptEngine.eval(
                "       var bukkitEvent=new BukkitWeaponAttachmentEvent(toBukkitEntity(event.player),false,false,null,toBukkitItemstack(event.gun),toBukkitItemstack(event.attach));\r\n"
                + "       Bukkit.getPluginManager().callEvent(bukkitEvent);\r\n"
                + "       if(bukkitEvent.isCanceled){\r\n" + "                event.setCanceled(true);\r\n"
                + "       }\r\n" + "       event.attach=toForgeItemstack(bukkitEvent.loadAttach);");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        // var bukkitEvent=new
        // BukkitWeaponAttachmentEvent(toBukkitEntity(event.player),false,false,null,toBukkitItemstack(event.gun),toBukkitItemstack(event.attach));
        // Bukkit.getPluginManager().callEvent(bukkitEvent);
        // if(bukkitEvent.isCanceled){
        // event.setCanceled(true);
        // }
        // event.attach=toForgeItemstack(bukkitEvent.loadAttach);
    }

    @SubscribeEvent
    public static void onAttachmentUnload(WeaponAttachmentEvent.Unload event) {
        scriptEngine.put("event", event);
        try {
            scriptEngine.eval(
                "var bukkitEvent=new BukkitWeaponAttachmentEvent(toBukkitEntity(event.player),true,event.unloadAll,event.type,toBukkitItemstack(event.gun),null);\r\n"
                + "Bukkit.getPluginManager().callEvent(bukkitEvent);\r\n" + "if(bukkitEvent.isCanceled){\r\n"
                + "    event.setCanceled(true);\r\n" + "}");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        // var bukkitEvent=new
        // BukkitWeaponAttachmentEvent(toBukkitEntity(event.player),true,event.unloadAll,event.type,toBukkitItemstack(event.gun),null);
        // Bukkit.getPluginManager().callEvent(bukkitEvent);
        // if(bukkitEvent.isCanceled){
        // event.setCanceled(true);
        // }
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

    @Cancelable
    public static class BukkitWeaponAttachmentEvent extends Event {
        public static final HandlerList handlerList = new HandlerList();

        public org.bukkit.entity.Player player;
        public final boolean isUnload;
        public final boolean isUnloadAll;
        public final String unloadAttachmentType;
        public final ItemStack gun;
        public ItemStack loadAttach;
        public boolean isCanceled = false;

        public BukkitWeaponAttachmentEvent(org.bukkit.entity.Player player, boolean isUnload, boolean isUnloadAll,
            String unloadAttachmentType, ItemStack gun, ItemStack loadAttach) {
            this.player = player;
            this.isUnload = isUnload;
            this.isUnloadAll = isUnloadAll;
            this.unloadAttachmentType = unloadAttachmentType;
            this.gun = gun;
            this.loadAttach = loadAttach;
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
}
