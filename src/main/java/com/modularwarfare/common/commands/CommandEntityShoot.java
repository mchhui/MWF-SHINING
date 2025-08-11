package com.modularwarfare.common.commands;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.EntityShootingAPI;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.UUID;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;

/**
 * 生物射击测试命令
 * 用于测试EntityShootingAPI的功能
 */
public class CommandEntityShoot extends CommandBase {

    @Override
    public String getName() {
        return "entity-shoot";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/entity-shoot <target> <shotCount> [useHeldWeapon] [weaponName] [ammoName] [magazineName]\n" +
               "/entity-shoot target <shooter> <target> <shotCount> <maxDistance> [useHeldWeapon] [weaponName] [ammoName] [magazineName]\n" +
               "/entity-shoot delayed <shooter> <target> <shotCount> <maxDistance> <delayTicks> <offsetX> <offsetY> <offsetZ> [useHeldWeapon] [weaponName] [ammoName] [magazineName]\n" +
               "/entity-shoot delayed-coord <shooter> <targetX> <targetY> <targetZ> <shotCount> <maxDistance> <delayTicks> <offsetX> <offsetY> <offsetZ> [useHeldWeapon] [weaponName] [ammoName] [magazineName]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // 需要OP权限
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: " + getUsage(sender)));
            return;
        }

        try {
            // 检查命令类型
            if (args[0].equalsIgnoreCase("target")) {
                executeTargetShootCommand(sender, args);
            } else if (args[0].equalsIgnoreCase("delayed")) {
                executeDelayedShootCommand(sender, args);
            } else if (args[0].equalsIgnoreCase("delayed-coord")) {
                executeDelayedCoordShootCommand(sender, args);
            } else {
                executeNormalShootCommand(sender, args);
            }

        } catch (Exception e) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Error executing command: " + e.getMessage()));
            ModularWarfare.LOGGER.error("EntityShoot命令执行错误", e);
        }
    }

    /**
     * 执行普通射击命令
     */
    private void executeNormalShootCommand(ICommandSender sender, String[] args) throws CommandException {
        // 解析参数
        String targetArg = args[0];
        int shotCount = parseInt(args[1]);
        
        boolean useHeldWeapon = true;
        String weaponName = null;
        String ammoName = null;
        String magazineName = null;
        
        if (args.length > 2) {
            useHeldWeapon = parseBoolean(args[2]);
        }
        
        if (args.length > 3) {
            weaponName = args[3];
        }
        
        if (args.length > 4) {
            ammoName = args[4];
        }
        
        if (args.length > 5) {
            magazineName = args[5];
        }

        // 查找目标实体
        EntityLivingBase targetEntity = findTargetEntity(sender, targetArg);

        if (targetEntity == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Target entity not found: " + targetArg));
            return;
        }

        // 执行射击
        boolean success = EntityShootingAPI.shootEntity(targetEntity, shotCount, useHeldWeapon, weaponName, ammoName, magazineName);
        
        if (success) {
            // 只有玩家执行时才显示成功信息
            if (sender instanceof EntityPlayer) {
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Successfully made entity " + targetEntity.getName() + " shoot " + shotCount + " times"));
                
                // 显示详细信息
                if (useHeldWeapon) {
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Using held weapon"));
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Using specified weapon: " + weaponName));
                }
                
                // 检查射击冷却
                long cooldown = EntityShootingAPI.getEntityShootCooldown(targetEntity);
                if (cooldown > 0) {
                    sender.sendMessage(new TextComponentString(TextFormatting.BLUE + "Shooting cooldown: " + cooldown + "ms"));
                }
            }
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to make entity " + targetEntity.getName() + " shoot"));
            
            // 检查原因
            if (!EntityShootingAPI.canEntityShoot(targetEntity, useHeldWeapon)) {
                if (useHeldWeapon) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Reason: Entity cannot shoot (may not have weapon or ammo)"));
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Reason: Entity is invalid or dead"));
                }
            }
        }
    }

    /**
     * 执行带目标射击命令
     */
    private void executeTargetShootCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 5) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Target shooting usage: /entity-shoot target <shooter> <target> <shotCount> <maxDistance> [useHeldWeapon] [weaponName] [ammoName] [magazineName]"));
            return;
        }

        // 解析参数
        String shooterArg = args[1];
        String targetArg = args[2];
        int shotCount = parseInt(args[3]);
        double maxDistance = parseDouble(args[4]);
        
        boolean useHeldWeapon = true;
        String weaponName = null;
        String ammoName = null;
        String magazineName = null;
        
        if (args.length > 5) {
            useHeldWeapon = parseBoolean(args[5]);
        }
        
        if (args.length > 6) {
            weaponName = args[6];
        }
        
        if (args.length > 7) {
            ammoName = args[7];
        }
        
        if (args.length > 8) {
            magazineName = args[8];
        }

        // 查找射击实体
        EntityLivingBase shooterEntity = findTargetEntity(sender, shooterArg);
        if (shooterEntity == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Shooter entity not found: " + shooterArg));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Supported formats:"));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "- UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "- Player selectors: @p, @a, @r"));
            
            // 添加调试信息
            if (shooterArg.equals("@p") || shooterArg.equals("@a") || shooterArg.equals("@r")) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Debug info: No player entities found"));
                int playerCount = 0;
                for (World w : net.minecraftforge.common.DimensionManager.getWorlds()) {
                    for (Entity e : w.loadedEntityList) {
                        if (e instanceof EntityPlayer) {
                            playerCount++;
                        }
                    }
                }
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Current online players: " + playerCount));
            }
            return;
        }

        // 查找目标实体
        EntityLivingBase targetEntity = findTargetEntity(sender, targetArg);
        if (targetEntity == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Target entity not found: " + targetArg));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Supported formats:"));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "- UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "- Player selectors: @p, @a, @r"));
            
            // 添加调试信息
            if (targetArg.equals("@p") || targetArg.equals("@a") || targetArg.equals("@r")) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Debug info: No player entities found"));
                int playerCount = 0;
                for (World w : net.minecraftforge.common.DimensionManager.getWorlds()) {
                    for (Entity e : w.loadedEntityList) {
                        if (e instanceof EntityPlayer) {
                            playerCount++;
                        }
                    }
                }
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Current online players: " + playerCount));
            }
            return;
        }

        // 检查距离
        double distance = shooterEntity.getDistance(targetEntity);
        
        if (distance > maxDistance) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Target distance " + String.format("%.2f", distance) + " exceeds maximum shooting distance " + maxDistance));
            return;
        }

        // 执行带目标射击
        boolean success = EntityShootingAPI.shootEntityAtTarget(shooterEntity, targetEntity, shotCount, maxDistance, 
                                                              useHeldWeapon, weaponName, ammoName, magazineName);
        
        if (success) {
            // 只有玩家执行时才显示成功信息
            if (sender instanceof EntityPlayer) {
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Successfully made entity " + shooterEntity.getName() + " shoot at " + targetEntity.getName() + " " + shotCount + " times"));
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Distance: " + String.format("%.2f", distance) + " blocks, Max distance: " + maxDistance + " blocks"));
                
                // 显示详细信息
                if (useHeldWeapon) {
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Using held weapon"));
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Using specified weapon: " + weaponName));
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Ammo: " + ammoName));
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Magazine: " + magazineName));
                }
            }
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to make entity " + shooterEntity.getName() + " shoot at " + targetEntity.getName()));
            // 检查原因
            if (!EntityShootingAPI.canEntityShoot(shooterEntity, useHeldWeapon)) {
                if (useHeldWeapon) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Reason: Shooter entity cannot shoot (may not have weapon or ammo)"));
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Reason: Shooter entity is invalid or dead"));
                }
            }
        }
    }

    /**
     * 执行延迟射击命令
     */
    private void executeDelayedShootCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 9) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Delayed shooting usage: /entity-shoot delayed <shooter> <target> <shotCount> <maxDistance> <delayTicks> <offsetX> <offsetY> <offsetZ> [useHeldWeapon] [weaponName] [ammoName] [magazineName]"));
            return;
        }

        // 解析参数
        String shooterArg = args[1];
        String targetArg = args[2];
        int shotCount = parseInt(args[3]);
        double maxDistance = parseDouble(args[4]);
        int delayTicks = parseInt(args[5]);
        float offsetX = (float)parseDouble(args[6]);
        float offsetY = (float)parseDouble(args[7]);
        float offsetZ = (float)parseDouble(args[8]);
        
        boolean useHeldWeapon = true;
        String weaponName = null;
        String ammoName = null;
        String magazineName = null;
        
        if (args.length > 9) {
            useHeldWeapon = parseBoolean(args[9]);
        }
        
        if (args.length > 10) {
            weaponName = args[10];
        }
        
        if (args.length > 11) {
            ammoName = args[11];
        }
        
        if (args.length > 12) {
            magazineName = args[12];
        }

        // 查找射击实体
        EntityLivingBase shooterEntity = findTargetEntity(sender, shooterArg);
        if (shooterEntity == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Shooter entity not found: " + shooterArg));
            return;
        }

        // 查找目标实体
        EntityLivingBase targetEntity = findTargetEntity(sender, targetArg);
        if (targetEntity == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Target entity not found: " + targetArg));
            return;
        }

        // 检查距离
        double distance = shooterEntity.getDistance(targetEntity);
        if (distance > maxDistance) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Target distance " + String.format("%.2f", distance) + " exceeds maximum shooting distance " + maxDistance));
            return;
        }

        boolean success = EntityShootingAPI.delayedShootEntityAtTarget(shooterEntity, targetEntity, shotCount, maxDistance, 
                                                                       delayTicks, offsetX, offsetY, offsetZ,
                                                                       useHeldWeapon, weaponName, ammoName, magazineName);
        
        if (success) {
            // 只有玩家执行时才显示成功信息
            if (sender instanceof EntityPlayer) {
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Successfully made entity " + shooterEntity.getName() + " delayed shoot at " + targetEntity.getName() + " " + shotCount + " times"));
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Distance: " + String.format("%.2f", distance) + " blocks, Max distance: " + maxDistance + " blocks, Delay: " + delayTicks + " ticks"));
                
                if (useHeldWeapon) {
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Using held weapon"));
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Using specified weapon: " + weaponName));
                }
                
                long cooldown = EntityShootingAPI.getEntityShootCooldown(shooterEntity);
                if (cooldown > 0) {
                    sender.sendMessage(new TextComponentString(TextFormatting.BLUE + "Shooting cooldown: " + cooldown + "ms"));
                }
            }
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to make entity " + shooterEntity.getName() + " delayed shoot at " + targetEntity.getName()));
            
            if (!EntityShootingAPI.canEntityShoot(shooterEntity, useHeldWeapon)) {
                if (useHeldWeapon) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Reason: Shooter entity cannot shoot (may not have weapon or ammo)"));
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Reason: Shooter entity is invalid or dead"));
                }
            }
        }
    }

    /**
     * 执行延迟坐标射击命令
     */
    private void executeDelayedCoordShootCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 11) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Delayed coordinate shooting usage: /entity-shoot delayed-coord <shooter> <targetX> <targetY> <targetZ> <shotCount> <maxDistance> <delayTicks> <offsetX> <offsetY> <offsetZ> [useHeldWeapon] [weaponName] [ammoName] [magazineName]"));
            return;
        }

        // 解析参数
        String shooterArg = args[1];
        double targetX = parseDouble(args[2]);
        double targetY = parseDouble(args[3]);
        double targetZ = parseDouble(args[4]);
        int shotCount = parseInt(args[5]);
        double maxDistance = parseDouble(args[6]);
        int delayTicks = parseInt(args[7]);
        float offsetX = (float)parseDouble(args[8]);
        float offsetY = (float)parseDouble(args[9]);
        float offsetZ = (float)parseDouble(args[10]);
        
        boolean useHeldWeapon = true;
        String weaponName = null;
        String ammoName = null;
        String magazineName = null;
        
        if (args.length > 11) {
            useHeldWeapon = parseBoolean(args[11]);
        }
        
        if (args.length > 12) {
            weaponName = args[12];
        }
        
        if (args.length > 13) {
            ammoName = args[13];
        }
        
        if (args.length > 14) {
            magazineName = args[14];
        }

        // 查找射击实体
        EntityLivingBase shooterEntity = findTargetEntity(sender, shooterArg);
        if (shooterEntity == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Shooter entity not found: " + shooterArg));
            return;
        }

        // 执行延迟坐标射击
        boolean success = EntityShootingAPI.delayedShootEntityAtCoordinates(shooterEntity, targetX, targetY, targetZ, shotCount, maxDistance, 
                                                                           delayTicks, offsetX, offsetY, offsetZ, useHeldWeapon, weaponName, ammoName, magazineName);
        
        if (success) {
            // 只有玩家执行时才显示成功信息
            if (sender instanceof EntityPlayer) {
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Successfully made entity " + shooterEntity.getName() + " delayed shoot at coordinates (" + String.format("%.2f", targetX) + ", " + String.format("%.2f", targetY) + ", " + String.format("%.2f", targetZ) + ") " + shotCount + " times"));
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Delay: " + delayTicks + " ticks, Offset: (" + String.format("%.2f", offsetX) + ", " + String.format("%.2f", offsetY) + ", " + String.format("%.2f", offsetZ) + ")"));
                
                // 显示详细信息
                if (useHeldWeapon) {
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Using held weapon"));
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Using specified weapon: " + weaponName));
                }
                
                // 检查射击冷却
                long cooldown = EntityShootingAPI.getEntityShootCooldown(shooterEntity);
                if (cooldown > 0) {
                    sender.sendMessage(new TextComponentString(TextFormatting.BLUE + "Shooting cooldown: " + cooldown + "ms"));
                }
            }
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to make entity " + shooterEntity.getName() + " delayed shoot at coordinates (" + String.format("%.2f", targetX) + ", " + String.format("%.2f", targetY) + ", " + String.format("%.2f", targetZ) + ")"));
            
            // 检查原因
            if (!EntityShootingAPI.canEntityShoot(shooterEntity, useHeldWeapon)) {
                if (useHeldWeapon) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Reason: Shooter entity cannot shoot (may not have weapon or ammo)"));
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Reason: Shooter entity is invalid or dead"));
                }
            }
        }
    }

    /**
     * 查找目标实体
     */
    private EntityLivingBase findTargetEntity(ICommandSender sender, String targetArg) {
        EntityLivingBase targetEntity = null;
        
        if (targetArg.equals("@p") || targetArg.equals("@a") || targetArg.equals("@r")) {
            // 玩家选择器 - 查找玩家
            // 在所有世界中查找玩家
            for (World w : net.minecraftforge.common.DimensionManager.getWorlds()) {
                for (Entity e : w.loadedEntityList) {
                    if (e instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) e;
                        
                        if (targetArg.equals("@p")) {
                            // 找到第一个玩家作为最近的玩家
                            targetEntity = player;
                            break;
                        } else if (targetArg.equals("@a")) {
                            // 找到第一个玩家
                            targetEntity = player;
                            break;
                        } else if (targetArg.equals("@r")) {
                            // 找到第一个玩家作为随机玩家
                            targetEntity = player;
                            break;
                        }
                    }
                }
                if (targetEntity != null) {
                    break;
                }
            }
            
            if (targetEntity == null) {
                ModularWarfare.LOGGER.warn("No player entities found for selector: {}", targetArg);
            }
        } else {
            // 尝试作为UUID处理（无前缀）
            try {
                UUID entityUUID = UUID.fromString(targetArg);
                
                // 查找UUID对应的实体
                for (World w : net.minecraftforge.common.DimensionManager.getWorlds()) {
                    for (Entity e : w.loadedEntityList) {
                        if (e instanceof EntityLivingBase && e.getUniqueID().equals(entityUUID)) {
                            targetEntity = (EntityLivingBase) e;
                            break;
                        }
                    }
                    if (targetEntity != null) {
                        break;
                    }
                }
                
                if (targetEntity == null) {
                    ModularWarfare.LOGGER.warn("Entity with UUID {} not found, please check if entity is in loaded worlds", targetArg);
                }
            } catch (IllegalArgumentException e) {
                // 不是有效的UUID格式
                ModularWarfare.LOGGER.warn("Invalid UUID format: {}", targetArg);
            }
        }
        
        return targetEntity;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        // 根据命令类型返回不同的用户名索引
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("target")) {
                // target <shooter> <target> <shotCount> <maxDistance> [useHeldWeapon] [weaponName] [ammoName] [magazineName]
                return index == 1 || index == 2; // shooter 和 target 参数
            } else if (args[0].equalsIgnoreCase("delayed")) {
                // delayed <shooter> <target> <shotCount> <maxDistance> <delayTicks> <offsetX> <offsetY> <offsetZ> [useHeldWeapon] [weaponName] [ammoName] [magazineName]
                return index == 1 || index == 2; // shooter 和 target 参数
            } else if (args[0].equalsIgnoreCase("delayed-coord")) {
                // delayed-coord <shooter> <targetX> <targetY> <targetZ> <shotCount> <maxDistance> <delayTicks> <offsetX> <offsetY> <offsetZ> [useHeldWeapon] [weaponName] [ammoName] [magazineName]
                return index == 1; // 只有 shooter 参数
            } else {
                // 普通射击命令: <target> <shotCount> [useHeldWeapon] [weaponName] [ammoName] [magazineName]
                return index == 0; // target 参数
            }
        }
        return false;
    }
} 