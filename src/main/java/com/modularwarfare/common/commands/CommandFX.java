package com.modularwarfare.common.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import safx.SAPackets;
import safx.packets.PacketSpawnParticle;
import safx.packets.PacketSpawnParticleOnEntity;
import safx.util.EntityCondition;
import com.modularwarfare.ModularWarfare;

import java.util.UUID;

public class CommandFX extends CommandBase {
    
    public int getRequiredPermissionLevel() {
        return 2;
    }

    public String getName() {
        return "mw-fx";
    }

    public String getUsage(ICommandSender sender) {
        return "/mw-fx <effect_name> <x> <y> <z> [motionX] [motionY] [motionZ] [scale] [world] or /mw-fx <effect_name> entity <uuid> [offsetX] [offsetY] [offsetZ] [attachToHead] [scale] or /mw-fx <effect_name> box <uuid> <boxName> <duration> [scale] [enableSmoothing] [smoothingSubdivisions] or /mw-fx help";
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("§cUsage: " + getUsage(sender)));
            return;
        }
        
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            ModularWarfare.PROXY.reloadFX();
            sender.sendMessage(new TextComponentString("§aReloading SAFX. You need use Sneak + F3 to reload textures..."));
            return;
        }
        
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return;
        }
        
        if (args.length >= 3 && args[1].equalsIgnoreCase("entity")) {
            handleEntityEffect(server, sender, args);
            return;
        }
        
        if (args.length >= 4 && args[1].equalsIgnoreCase("box")) {
            handleBoxEffect(server, sender, args);
            return;
        }
        
        if (args.length < 4) {
            sender.sendMessage(new TextComponentString("§cUsage: " + getUsage(sender)));
            return;
        }

        handleLocationEffect(server, sender, args);
    }
    
    private void handleEntityEffect(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 3) {
            sender.sendMessage(new TextComponentString("§cUsage: /mw-fx <effect_name> entity <uuid> [offsetX] [offsetY] [offsetZ] [attachToHead] [scale]"));
            return;
        }
        
        String fxName = args[0];
        String uuidString = args[2];
        
        try {
            UUID entityUUID = UUID.fromString(uuidString);
            
            float offsetX = args.length > 3 ? Float.parseFloat(args[3]) : 0.0f;
            float offsetY = args.length > 4 ? Float.parseFloat(args[4]) : 0.0f;
            float offsetZ = args.length > 5 ? Float.parseFloat(args[5]) : 0.0f;
            boolean attachToHead = args.length > 6 ? Boolean.parseBoolean(args[6]) : false;
            float scale = args.length > 7 ? Float.parseFloat(args[7]) : 1.0f;
            
            Entity targetEntity = null;

            for (World world : net.minecraftforge.common.DimensionManager.getWorlds()) {
                for (Entity entity : world.loadedEntityList) {
                    if (entity.getUniqueID().equals(entityUUID)) {
                        targetEntity = entity;
                        break;
                    }
                }
                if (targetEntity != null) break;
            }
            
            if (targetEntity == null) {
                sender.sendMessage(new TextComponentString("§cEntity with UUID " + uuidString + " not found!"));
                return;
            }
            
            // 获取距离实体最近的玩家，使用其维度来发送特效包（对虚拟世界很重要）
            EntityPlayerMP nearestPlayer = null;
            EntityPlayer nearest = targetEntity.world.getClosestPlayer(targetEntity.posX, targetEntity.posY, targetEntity.posZ, 512, false);
            if (nearest instanceof EntityPlayerMP) {
                nearestPlayer = (EntityPlayerMP) nearest;
            }
            
            if (nearestPlayer == null) {
                sender.sendMessage(new TextComponentString("§cNo player nearby to render effect (need player within 512 blocks)!"));
                return;
            }
            
            SAPackets.sendEffectToAllAround(
                new PacketSpawnParticleOnEntity(fxName, targetEntity, offsetX, offsetY, offsetZ, attachToHead, EntityCondition.NONE, scale),
                targetEntity, 512.0
            );
            
            sender.sendMessage(new TextComponentString("§aAttached effect §e" + fxName + " §ato entity " + targetEntity.getName() + 
                " §ain world §e" + targetEntity.world.getWorldInfo().getWorldName() +
                " §a(dimension: " + nearestPlayer.dimension + ")" +
                " §awith offset: §e(" + formatFloat(offsetX) + ", " + formatFloat(offsetY) + ", " + formatFloat(offsetZ) + ") " +
                "§aattachToHead: §e" + attachToHead + " §ascale: §e" + scale));
            
        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponentString("§cInvalid number format in parameters!"));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(new TextComponentString("§cInvalid UUID format: " + uuidString));
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("§cError spawning effect on entity: " + e.getMessage()));
        }
    }
    
    private void handleBoxEffect(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 5) {
            sender.sendMessage(new TextComponentString("§cUsage: /mw-fx <effect_name> box <uuid> <boxName> <duration> [scale] [enableSmoothing] [smoothingSubdivisions]"));
            return;
        }
        
        String fxName = args[0];
        String uuidString = args[2];
        String boxName = args[3];
        
        try {
            UUID entityUUID = UUID.fromString(uuidString);
            long duration = Long.parseLong(args[4]);
            float scale = args.length > 5 ? Float.parseFloat(args[5]) : 1.0f;
            boolean enableSmoothing = args.length > 6 ? Boolean.parseBoolean(args[6]) : false;
            int smoothingSubdivisions = args.length > 7 ? Integer.parseInt(args[7]) : 3;
            
            Entity targetEntity = null;

            for (World world : net.minecraftforge.common.DimensionManager.getWorlds()) {
                for (Entity entity : world.loadedEntityList) {
                    if (entity.getUniqueID().equals(entityUUID)) {
                        targetEntity = entity;
                        break;
                    }
                }
                if (targetEntity != null) break;
            }
            
            if (targetEntity == null) {
                sender.sendMessage(new TextComponentString("§cEntity with UUID " + uuidString + " not found!"));
                return;
            }
            
            EntityPlayerMP nearestPlayer = null;
            EntityPlayer nearest = targetEntity.world.getClosestPlayer(targetEntity.posX, targetEntity.posY, targetEntity.posZ, 512, false);
            if (nearest instanceof EntityPlayerMP) {
                nearestPlayer = (EntityPlayerMP) nearest;
            }
            
            if (nearestPlayer == null) {
                sender.sendMessage(new TextComponentString("§cNo player nearby to render effect (need player within 512 blocks)!"));
                return;
            }
            
            safx.SagerFX.proxy.createFXOnBoxWithPacket(fxName, targetEntity, boxName, duration, scale, enableSmoothing, smoothingSubdivisions);
            
            String durationText = duration == 0 ? "permanent" : duration + "ms";
            String smoothingText = enableSmoothing ? ("§aenabled (subdivisions: §e" + smoothingSubdivisions + "§a)") : "§cdisabled";
            sender.sendMessage(new TextComponentString("§aAttached effect §e" + fxName + " §ato OBB box §e" + boxName + 
                " §aon entity " + targetEntity.getName() + 
                " §ain world §e" + targetEntity.world.getWorldInfo().getWorldName() +
                " §a(dimension: " + nearestPlayer.dimension + ")" +
                " §awith duration: §e" + durationText + " §ascale: §e" + scale + " §asmoothing: " + smoothingText));
            
        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponentString("§cInvalid number format in parameters!"));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(new TextComponentString("§cInvalid UUID format: " + uuidString));
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("§cError spawning effect on box: " + e.getMessage()));
        }
    }
    
    private void handleLocationEffect(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        try {
            String fxName = args[0];
            double posX = Double.parseDouble(args[1]);
            double posY = Double.parseDouble(args[2]);
            double posZ = Double.parseDouble(args[3]);

            String worldName = null;
            int worldNameIndex = -1;
            
            if (args.length == 5 && !isNumeric(args[4])) {
                worldName = args[4];
                worldNameIndex = 4;
            } else if (args.length == 6 && !isNumeric(args[5])) {
                worldName = args[5];
                worldNameIndex = 5;
            } else if (args.length == 8 && !isNumeric(args[7])) {
                worldName = args[7];
                worldNameIndex = 7;
            } else if (args.length == 9 && !isNumeric(args[8])) {
                worldName = args[8];
                worldNameIndex = 8;
            }
            
            World world;
            if (worldName != null) {
                world = getWorldByName(server, worldName);
                if (world == null) {
                    sender.sendMessage(new TextComponentString("§cWorld '" + worldName + "' not found!"));
                    return;
                }
            } else if (sender instanceof EntityPlayerMP) {
                world = ((EntityPlayerMP) sender).world;
            } else {
                world = server.getWorld(0);
            }

            int effectiveArgsLength = worldNameIndex != -1 ? args.length - 1 : args.length;

            if (effectiveArgsLength == 4) {
                SAPackets.sendEffectToAllAround(
                    new PacketSpawnParticle(fxName, posX, posY, posZ), 
                    world, posX, posY, posZ, 128.0
                );
                sender.sendMessage(new TextComponentString("§aSpawned effect §e" + fxName + " §aat (" + formatDouble(posX) + ", " + formatDouble(posY) + ", " + formatDouble(posZ) + ") §ain world §e" + world.getWorldInfo().getWorldName()));
            }
            else if (effectiveArgsLength == 5) {
                float scale = Float.parseFloat(args[4]);
                SAPackets.sendEffectToAllAround(
                    new PacketSpawnParticle(fxName, posX, posY, posZ, scale), 
                    world, posX, posY, posZ, 128.0
                );
                sender.sendMessage(new TextComponentString("§aSpawned effect §e" + fxName + " §aat (" + formatDouble(posX) + ", " + formatDouble(posY) + ", " + formatDouble(posZ) + ") §ain world §e" + world.getWorldInfo().getWorldName() + " §awith scale: §e" + scale));
            }
            else if (effectiveArgsLength == 7) {
                double motionX = Double.parseDouble(args[4]);
                double motionY = Double.parseDouble(args[5]);
                double motionZ = Double.parseDouble(args[6]);
                
                SAPackets.sendEffectToAllAround(
                    new PacketSpawnParticle(fxName, posX, posY, posZ, motionX, motionY, motionZ), 
                    world, posX, posY, posZ, 128.0
                );
                sender.sendMessage(new TextComponentString("§aSpawned effect §e" + fxName + " §aat (" + formatDouble(posX) + ", " + formatDouble(posY) + ", " + formatDouble(posZ) + ") §ain world §e" + world.getWorldInfo().getWorldName() + " §awith motion: §e(" + formatDouble(motionX) + ", " + formatDouble(motionY) + ", " + formatDouble(motionZ) + ")"));
            }
            else if (effectiveArgsLength == 8) {
                double motionX = Double.parseDouble(args[4]);
                double motionY = Double.parseDouble(args[5]);
                double motionZ = Double.parseDouble(args[6]);
                float scale = Float.parseFloat(args[7]);
                
                SAPackets.sendEffectToAllAround(
                    new PacketSpawnParticle(fxName, posX, posY, posZ, motionX, motionY, motionZ, scale), 
                    world, posX, posY, posZ, 128.0
                );
                sender.sendMessage(new TextComponentString("§aSpawned effect §e" + fxName + " §aat (" + formatDouble(posX) + ", " + formatDouble(posY) + ", " + formatDouble(posZ) + ") §ain world §e" + world.getWorldInfo().getWorldName() + " §awith motion: §e(" + formatDouble(motionX) + ", " + formatDouble(motionY) + ", " + formatDouble(motionZ) + ") §aand scale: §e" + scale));
            }
            else {
                sender.sendMessage(new TextComponentString("§cWrong number of parameters! Should be 4, 5, 7 or 8 parameters (plus optional world name)"));
                sender.sendMessage(new TextComponentString("§eUsage examples:"));
                sender.sendMessage(new TextComponentString("§e/mw-fx LargeExplosion 100 64 200"));
                sender.sendMessage(new TextComponentString("§e/mw-fx LargeExplosion 100 64 200 world_nether"));
                sender.sendMessage(new TextComponentString("§e/mw-fx LargeExplosion 100 64 200 2.0 world_the_end"));
                sender.sendMessage(new TextComponentString("§e/mw-fx SABulletTrail 100 64 200 0.1 0.5 0.1 world_nether"));
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponentString("§cInvalid number format! Please ensure coordinates, motion values and scale are valid numbers"));
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("§cError spawning effect: " + e.getMessage()));
        }
    }
    
    private World getWorldByName(MinecraftServer server, String worldName) {
        for (int i = 0; i < server.worlds.length; i++) {
            World world = server.worlds[i];
            if (world.getWorldInfo().getWorldName().equalsIgnoreCase(worldName)) {
                return world;
            }
        }
        return null;
    }
    
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void showHelp(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== ModularWarfare FX Command Help ==="));
        sender.sendMessage(new TextComponentString("§eAdmin Commands:"));
        sender.sendMessage(new TextComponentString("§a/mw-fx reload"));
        sender.sendMessage(new TextComponentString("  §7- Reload particles and textures (Client Side)"));
        sender.sendMessage(new TextComponentString("§eLocation-based effects:"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> <x> <y> <z> [world]"));
        sender.sendMessage(new TextComponentString("  §7- Spawn effect at specified location"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> <x> <y> <z> <scale> [world]"));
        sender.sendMessage(new TextComponentString("  §7- Spawn effect with custom scale"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> <x> <y> <z> <motionX> <motionY> <motionZ> [world]"));
        sender.sendMessage(new TextComponentString("  §7- Spawn effect with motion"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> <x> <y> <z> <motionX> <motionY> <motionZ> <scale> [world]"));
        sender.sendMessage(new TextComponentString("  §7- Spawn effect with motion and scale"));
        sender.sendMessage(new TextComponentString("§eEntity-attached effects:"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> entity <uuid>"));
        sender.sendMessage(new TextComponentString("  §7- Attach effect to entity by UUID"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> entity <uuid> <offsetX> <offsetY> <offsetZ>"));
        sender.sendMessage(new TextComponentString("  §7- Attach effect with position offset"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> entity <uuid> <offsetX> <offsetY> <offsetZ> <attachToHead> <scale>"));
        sender.sendMessage(new TextComponentString("  §7- Full entity effect with head attachment and scale"));
        sender.sendMessage(new TextComponentString("§eOBB Box-attached effects:"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> box <uuid> <boxName> <duration>"));
        sender.sendMessage(new TextComponentString("  §7- Attach effect to OBB collision box"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> box <uuid> <boxName> <duration> <scale>"));
        sender.sendMessage(new TextComponentString("  §7- Attach effect to OBB box with scale (duration in ms, 0=permanent)"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> box <uuid> <boxName> <duration> <scale> <enableSmoothing>"));
        sender.sendMessage(new TextComponentString("  §7- With trail smoothing (true/false) for curved effect"));
        sender.sendMessage(new TextComponentString("§a/mw-fx <effect_name> box <uuid> <boxName> <duration> <scale> <enableSmoothing> <subdivisions>"));
        sender.sendMessage(new TextComponentString("  §7- With custom subdivisions (1-10, default 3, higher=smoother+slower)"));
        sender.sendMessage(new TextComponentString("§eWorld Names:"));
        sender.sendMessage(new TextComponentString("  §7- §eworld §7(Overworld), §eworld_nether §7(Nether), §eworld_the_end §7(End)"));
        sender.sendMessage(new TextComponentString("  §7- Custom world names based on level name"));
        sender.sendMessage(new TextComponentString("§eAvailable Effect Examples:"));
        sender.sendMessage(new TextComponentString("  §7Explosions: §eLargeExplosion, smallExplosion, RocketExplosion"));
        sender.sendMessage(new TextComponentString("  §7Explosions: §eAdvExplosion, AdvExpSmall, GuidedMissileExplosion"));
        sender.sendMessage(new TextComponentString("  §7Fire: §eExplosionFire, FlamethrowerTrail, ExplosionFirePillar"));
        sender.sendMessage(new TextComponentString("  §7Smoke: §eBlackSmoke, SmokeGun, SmokeTank"));
        sender.sendMessage(new TextComponentString("  §7Sparks: §eOrangeSpark, BlueSparks, SparkTrail"));
        sender.sendMessage(new TextComponentString("  §7Trails: §eSABulletTrail, SAMissileTrail, SAGrenadeTrail"));
        sender.sendMessage(new TextComponentString("  §7Impacts: §eGunHit, LaserHit, PrismHit, PowerImpact"));
        sender.sendMessage(new TextComponentString("§eTip: Use ~ for current position coordinates"));
        sender.sendMessage(new TextComponentString("§eTip: Get entity UUID with F3+I while looking at entity"));
        sender.sendMessage(new TextComponentString("§eTip: Common OBB box names: head, body, leftArm, rightArm, leftLeg, rightLeg"));
        sender.sendMessage(new TextComponentString("§eTip: Duration 0 means permanent (until entity dies)"));
        sender.sendMessage(new TextComponentString("§eTip: If no world specified for location effects, uses current world or overworld"));
        sender.sendMessage(new TextComponentString("§eTip: Entity effects automatically use the entity's world"));
    }
    
    private String formatDouble(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.2f", value);
        }
    }
    
    private String formatFloat(float value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.2f", value);
        }
    }
} 