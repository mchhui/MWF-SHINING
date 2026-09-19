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
        return "/mw-play player <name speed allowReload allowFire | startFrame endFrame speed allowReload allowFire | stop>";
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 5 && args.length != 6 && args.length != 2)
            throw new net.minecraft.command.WrongUsageException(getUsage(sender));
        EntityPlayerMP player = getPlayer(server, sender, args[0]);
        boolean accepted;
        if (args.length == 2 && "stop".equalsIgnoreCase(args[1])) {
            accepted = com.modularwarfare.api.WeaponVisualAPI.stopAnimation(player);
        } else if (args.length == 6) {
            accepted = com.modularwarfare.api.WeaponVisualAPI.playAnimation(player,
                    parseDouble(args[1], 0), parseDouble(args[2], 0), (float) (parseDouble(args[3], 0) * 60),
                    parseBoolean(args[4]), parseBoolean(args[5]));
        } else if (args.length == 5) {
            accepted = com.modularwarfare.api.WeaponVisualAPI.playAnimation(player, args[1],
                    (float) parseDouble(args[2], 0), parseBoolean(args[3]), parseBoolean(args[4]));
        } else throw new net.minecraft.command.WrongUsageException(getUsage(sender));
        if (!accepted) throw new CommandException("Invalid animation parameters or held weapon is not enhanced");
    }

}
