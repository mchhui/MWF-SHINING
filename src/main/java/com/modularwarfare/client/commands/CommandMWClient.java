package com.modularwarfare.client.commands;

import com.modularwarfare.ModularWarfare;
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
        // TODO Auto-generated method stub
        return "/mw-client";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1) {
            if (args[0].equals("md5")) {
                for (int i = 0; i < ModularWarfare.contentPackHashList.size(); i++) {
                    sender.sendMessage(new TextComponentString(ModularWarfare.contentPackHashList.get(i)));
                }
                if (ModularWarfare.contentPackHashList.size() == 0) {
                    sender.sendMessage(new TextComponentString("There is not any content pack."));
                }
                return;
            }
        }
        sender.sendMessage(new TextComponentString("/mw-client md5 | Get the md5 of the content pack"));
    }

}
