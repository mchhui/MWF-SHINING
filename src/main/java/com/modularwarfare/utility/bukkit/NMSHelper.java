package com.modularwarfare.utility.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.minecraft.entity.player.EntityPlayer;
/**
 * 该类的NMS风格引用由NMSHelperTransformer转化成Forge风格引用
 * 映射表需要手动维护
 * */
public class NMSHelper {
    public static Entity toBukkitEntity(net.minecraft.entity.Entity entity) {
        return CraftEntity.getEntity((CraftServer)Bukkit.getServer(), (net.minecraft.server.v1_12_R1.Entity)(Object)entity);
    }

    public static Player toBukkitPlayer(EntityPlayer player) {
        return (Player)CraftEntity.getEntity((CraftServer)Bukkit.getServer(), (net.minecraft.server.v1_12_R1.Entity)(Object)player);
    }

    public static ItemStack toBukkitStack(net.minecraft.item.ItemStack stack) {
        return CraftItemStack.asBukkitCopy((net.minecraft.server.v1_12_R1.ItemStack)(Object)stack);
    }

    public static net.minecraft.item.ItemStack toForgeStack(ItemStack stack) {
        return (net.minecraft.item.ItemStack)(Object)CraftItemStack.asNMSCopy(stack);
    }
}
