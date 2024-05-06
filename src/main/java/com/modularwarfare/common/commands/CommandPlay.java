package com.modularwarfare.common.commands;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.network.PacketBackpackJet;
import com.modularwarfare.common.network.PacketCustomAnimation;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandPlay extends CommandBase {
    public int getRequiredPermissionLevel() {
        return 4;
    }

    public String getName() {
        return "mw-play";
    }

    public String getUsage(ICommandSender sender) {
        return "/mw-play player startTime endTime speedFactor allowReload allowFire";
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player=getPlayer(server, sender, args[0]);
        double s=Double.valueOf(args[1]);
        double e=Double.valueOf(args[2]);
        float speedFactor=Float.valueOf(args[3]);
        boolean allowReload=Boolean.valueOf(args[4]);
        boolean allowFire=Boolean.valueOf(args[5]);
        ModularWarfare.NETWORK.sendTo(new PacketCustomAnimation(player.getUniqueID(), s,e, speedFactor, allowReload, allowFire), player);
    }

}