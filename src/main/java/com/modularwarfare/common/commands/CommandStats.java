package com.modularwarfare.common.commands;

import com.modularwarfare.api.WeaponStats;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.HashMap;

public class CommandStats extends CommandBase {
    public int getRequiredPermissionLevel() {
        return 0;
    }

    public String getName() {
        return "mw-stats";
    }

    public String getUsage(ICommandSender sender) {
        return "/mw-stats <internalName/hand>";
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "the command can only be used by players"));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: " + getUsage(sender)));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        HashMap<String, Object> stats;

        if (args[0].equalsIgnoreCase("hand")) {
            stats = WeaponStats.getHeldWeaponStats(player);
            System.out.println("stats: " + stats);
            if (stats == null) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not holding a weapon"));
                return;
            }
        } else {
            stats = WeaponStats.getWeaponStats(args[0]);
            if (stats == null) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Cannot find the weapon: " + args[0]));
                return;
            }
        }

        // 发送枪械信息
        sender.sendMessage(new TextComponentString("§6========== Weapon Information =========="));
        
        // 基础属性
        sender.sendMessage(new TextComponentString("§eBasic Attributes:"));
        sendStatInfo(sender, stats, "damage", "Basic Damage");
        sendStatInfo(sender, stats, "fireRate", "Fire Rate (RPM)");
        sendStatInfo(sender, stats, "reloadTime", "Reload Time (ticks)");
        sendStatInfo(sender, stats, "maxAmmo", "Ammo Capacity");
        sendStatInfo(sender, stats, "maxRange", "Max Range");
        sendStatInfo(sender, stats, "effectiveRange", "Effective Range");
        sendStatInfo(sender, stats, "bulletSpread", "Bullet Spread");
        sendStatInfo(sender, stats, "moveSpeedModifier", "Move Speed Modifier");
        sendStatInfo(sender, stats, "gunDamageHeadshotBonus", "Headshot Damage Bonus");

        // 后坐力属性
        sender.sendMessage(new TextComponentString("§eRecoil Attributes:"));
        sendStatInfo(sender, stats, "recoilPitch", "Vertical Recoil");
        sendStatInfo(sender, stats, "recoilYaw", "Horizontal Recoil");
        sendStatInfo(sender, stats, "recoilAimReducer", "Aim Recoil Reducer");
        sendStatInfo(sender, stats, "antiRecoilFactor", "Anti-Recoil Factor");

        // 精准度属性
        sender.sendMessage(new TextComponentString("§eAccuracy Attributes:"));
        sendStatInfo(sender, stats, "accuracyAimFactor", "Aim Accuracy");
        sendStatInfo(sender, stats, "accuracySneakFactor", "Sneak Accuracy");
        sendStatInfo(sender, stats, "accuracyCrawlFactor", "Crawl Accuracy");
        sendStatInfo(sender, stats, "accuracyMoveOffset", "Move Accuracy Offset");
        sendStatInfo(sender, stats, "accuracySprintOffset", "Sprint Accuracy Offset");

        // 穿透属性
        sender.sendMessage(new TextComponentString("§ePenetration Attributes:"));
        sendStatInfo(sender, stats, "gunPenetrateSize", "Penetration Distance");
        sendStatInfo(sender, stats, "gunMaxPenetrateBlockResistance", "Max Block Penetration Resistance");
        sendStatInfo(sender, stats, "gunPenetrateBlocksResistance", "Continuous Penetration Resistance");
        
        // 其他属性
        sender.sendMessage(new TextComponentString("§eOther Attributes:"));
        sendStatInfo(sender, stats, "weaponType", "Weapon Type");
    }

    private void sendStatInfo(ICommandSender sender, HashMap<String, Object> stats, String key, String displayName) {
        Object value = stats.get(key);
        if (value != null) {
            sender.sendMessage(new TextComponentString(String.format("§7%s: §f%s", displayName, value.toString())));
        }
    }
} 