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
        return "/mw-client md5 | /mw-client debugnode <on|off|bone>";
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
            // Unified: debugnode / debugmarkers (alias)
            if ("debugnode".equalsIgnoreCase(args[0]) || "debugmarkers".equalsIgnoreCase(args[0])) {
                if (args.length < 2) {
                    sendDebugStatus(sender);
                    return;
                }
                String sub = args[1].trim();
                if ("off".equalsIgnoreCase(sub) || "clear".equalsIgnoreCase(sub) || "false".equalsIgnoreCase(sub)
                        || "0".equals(sub)) {
                    RenderGunEnhanced.debugMarkerNodes = false;
                    RenderGunEnhanced.debugGunNodeName = null;
                    sender.sendMessage(new TextComponentString(
                            "Node debug OFF (markers + custom bone + trail rays)."));
                    return;
                }
                if ("on".equalsIgnoreCase(sub) || "markers".equalsIgnoreCase(sub) || "true".equalsIgnoreCase(sub)
                        || "1".equals(sub)) {
                    RenderGunEnhanced.debugMarkerNodes = true;
                    sender.sendMessage(new TextComponentString(
                            "Node debug ON: flashModel + mwf_scope_point + trail rays."));
                    if (RenderGunEnhanced.debugGunNodeName != null) {
                        sender.sendMessage(new TextComponentString(
                                "Custom bone still set: " + RenderGunEnhanced.debugGunNodeName));
                    }
                    return;
                }
                RenderGunEnhanced.debugMarkerNodes = true;
                RenderGunEnhanced.debugGunNodeName = sub;
                sender.sendMessage(new TextComponentString(
                        "Node debug ON: markers + trail rays + custom bone \"" + sub + "\"."));
                sender.sendMessage(new TextComponentString("Use /mw-client debugnode off to clear."));
                return;
            }
        }
        sender.sendMessage(new TextComponentString("/mw-client md5 - content pack hashes"));
        sender.sendMessage(new TextComponentString(
                "/mw-client debugnode <on|off|bone> - markers + trail rays; bone marks that gun node"));
        sender.sendMessage(new TextComponentString(
                "  on = flashModel + mwf_scope_point + trail rays (default off)"));
                sender.sendMessage(new TextComponentString(
                        "  trail rays: yellow=node forward, red=packet eye→hit, lime=corrected origin→hit"));
    }

    private static void sendDebugStatus(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(
                "Node debug: " + (RenderGunEnhanced.debugMarkerNodes ? "ON" : "OFF")
                        + (RenderGunEnhanced.debugGunNodeName != null
                                ? (", bone=" + RenderGunEnhanced.debugGunNodeName)
                                : "")));
        sender.sendMessage(new TextComponentString("Usage: /mw-client debugnode <on|off|bone>"));
    }

}
