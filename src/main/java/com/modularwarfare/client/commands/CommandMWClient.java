package com.modularwarfare.client.commands;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandMWClient extends CommandBase {

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getName() {
        return "mw-client";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/mw-client md5 | /mw-client debugnode <node|off>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 1) {
            if (args[0].equals("md5")) {
                for (int i = 0; i < ModularWarfare.contentPackHashList.size(); i++) {
                    sender.sendMessage(new TextComponentString(ModularWarfare.contentPackHashList.get(i)));
                }
                if (ModularWarfare.contentPackHashList.size() == 0) {
                    sender.sendMessage(new TextComponentString("No content packs."));
                }
                return;
            }
            if (args.length >= 2 && "debugnode".equalsIgnoreCase(args[0])) {
                String sub = args[1];
                if ("off".equalsIgnoreCase(sub) || "clear".equalsIgnoreCase(sub)) {
                    RenderGunEnhanced.debugGunNodeName = null;
                    sender.sendMessage(new TextComponentString("Gun node debug off."));
                    return;
                }
                String name = sub.trim();
                RenderGunEnhanced.debugGunNodeName = name;
                sender.sendMessage(new TextComponentString("Gun node: " + name));
                sender.sendMessage(new TextComponentString("Marks that bone in 1P/3P; use off to clear."));
                return;
            }
        }
        sender.sendMessage(new TextComponentString("/mw-client md5 - content pack hashes"));
        sender.sendMessage(new TextComponentString("/mw-client debugnode <name> - gun bone overlay; debugnode off"));
    }

}
