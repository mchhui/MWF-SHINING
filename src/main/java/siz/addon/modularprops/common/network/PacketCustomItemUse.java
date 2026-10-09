package siz.addon.modularprops.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import siz.addon.modularprops.ModularProps;
import siz.addon.modularprops.common.custom.CustomItemType;
import siz.addon.modularprops.common.custom.ItemCustom;

import java.util.HashMap;
import java.util.UUID;

/**
 * 自定义道具使用网络包
 * 用于通知服务器执行USE指令和消耗道具
 * 
 * 安全机制：
 * 1. 指令内容完全由服务器端配置文件决定，客户端无法篡改
 * 2. 服务器端验证玩家是否真的持有该道具
 * 3. 冷却时间检查，防止频繁发包
 * 4. 每个玩家的USE状态跟踪，防止重复执行
 */
public class PacketCustomItemUse implements IMessage {
    
    private boolean executeCommand; // 是否执行指令
    private boolean consumeItem;    // 是否标记需要消耗道具
    
    // 服务器端安全验证数据
    private static class UseRecord {
        long lastUseTime = 0;           // 上次使用时间
        boolean commandExecuted = false; // 指令是否已执行
        ItemStack lastStack = ItemStack.EMPTY; // 上次使用的物品
    }
    
    // 每个玩家的使用记录（服务器端）
    private static final HashMap<UUID, UseRecord> playerUseRecords = new HashMap<>();
    
    // 最小使用间隔（毫秒）- 防止频繁发包
    private static final long MIN_USE_INTERVAL = 100;
    
    public PacketCustomItemUse() {
        // 无参构造函数（网络包需要）
    }
    
    public PacketCustomItemUse(boolean executeCommand, boolean consumeItem) {
        this.executeCommand = executeCommand;
        this.consumeItem = consumeItem;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        this.executeCommand = buf.readBoolean();
        this.consumeItem = buf.readBoolean();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(executeCommand);
        buf.writeBoolean(consumeItem);
    }
    
    public static class Handler implements IMessageHandler<PacketCustomItemUse, IMessage> {
        @Override
        public IMessage onMessage(PacketCustomItemUse message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack mainHand = player.getHeldItemMainhand();
                UUID playerId = player.getUniqueID();
                
                // === 安全验证 1：检查是否持有正确的道具 ===
                if (!(mainHand.getItem() instanceof ItemCustom)) {
                    ModularProps.LOGGER.warn("Player " + player.getName() + " tried to use custom item but not holding one!");
                    return;
                }
                
                ItemCustom item = (ItemCustom) mainHand.getItem();
                CustomItemType itemType = item.type;
                
                if (itemType == null || itemType.consumableConfig == null) {
                    return;
                }
                
                // 获取或创建使用记录
                UseRecord record = playerUseRecords.computeIfAbsent(playerId, k -> new UseRecord());
                long currentTime = System.currentTimeMillis();
                
                // === 安全验证 2：冷却时间检查 ===
                if (currentTime - record.lastUseTime < MIN_USE_INTERVAL) {
                    ModularProps.LOGGER.warn("Player " + player.getName() + " used item too frequently! Possible exploit attempt.");
                    return;
                }
                
                // === 安全验证 3：检查道具配置是否有指令 ===
                if (message.executeCommand) {
                    if (itemType.consumableConfig.useCommand == null || 
                        itemType.consumableConfig.useCommand.isEmpty()) {
                        ModularProps.LOGGER.warn("Player " + player.getName() + " tried to execute command but item has no commands configured!");
                        return;
                    }
                    
                    // === 安全验证 4：防止重复执行指令 ===
                    if (record.commandExecuted && record.lastStack == mainHand) {
                        ModularProps.LOGGER.warn("Player " + player.getName() + " tried to execute command twice for the same item!");
                        return;
                    }
                    
                    // 执行指令列表（服务器端）
                    for (String commandTemplate : itemType.consumableConfig.useCommand) {
                        if (commandTemplate == null || commandTemplate.isEmpty()) {
                            continue;
                        }
                        
                        // 解析执行者标签
                        String executorType = "console"; // 默认控制台执行
                        String actualCommand = commandTemplate;
                        
                        if (commandTemplate.startsWith("[player]")) {
                            executorType = "player";
                            actualCommand = commandTemplate.substring(8);
                        } else if (commandTemplate.startsWith("[op]")) {
                            executorType = "op";
                            actualCommand = commandTemplate.substring(4);
                        } else if (commandTemplate.startsWith("[console]")) {
                            executorType = "console";
                            actualCommand = commandTemplate.substring(9);
                        }
                        
                        // 替换占位符
                        String command = actualCommand
                            .replace("%player%", player.getName())
                            .replace("@s", "@p[name=" + player.getName() + "]");
                        
                        // 根据执行者类型执行指令
                        if (player.getServer() != null && player.getServer().getCommandManager() != null) {
                            switch (executorType) {
                                case "player":
                                    // 以玩家身份执行（受权限限制）
                                    player.getServer().getCommandManager().executeCommand(player, command);
                                    break;
                                    
                                case "op":
                                    // 临时授予OP权限执行
                                    boolean wasOp = player.getServer().getPlayerList().canSendCommands(player.getGameProfile());
                                    if (!wasOp) {
                                        player.getServer().getPlayerList().addOp(player.getGameProfile());
                                    }
                                    try {
                                        player.getServer().getCommandManager().executeCommand(player, command);
                                    } finally {
                                        if (!wasOp) {
                                            player.getServer().getPlayerList().removeOp(player.getGameProfile());
                                        }
                                    }
                                    break;
                                    
                                case "console":
                                default:
                                    // 以控制台身份执行（不受权限限制）
                                    player.getServer().getCommandManager().executeCommand(
                                        player.getServer(), 
                                        command
                                    );
                                    break;
                            }
                        }
                    }
                    
                    // 标记指令已执行
                    record.commandExecuted = true;
                    record.lastStack = mainHand.copy();
                    record.lastUseTime = currentTime;
                }
                
                // === 消耗道具（服务器端）===
                if (message.consumeItem) {
                    // === 安全验证 5：检查配置是否允许消耗 ===
                    if (!itemType.consumableConfig.consumeOnUse) {
                        ModularProps.LOGGER.warn("Player " + player.getName() + " tried to consume item but config doesn't allow it!");
                        return;
                    }
                    
                    // === 安全验证 6：只有指令已执行才能消耗 ===
                    if (!record.commandExecuted) {
                        ModularProps.LOGGER.warn("Player " + player.getName() + " tried to consume item before executing command!");
                        return;
                    }
                    
                    // 消耗道具
                    if (!player.capabilities.isCreativeMode) {
                        mainHand.shrink(1);
                        if (mainHand.getCount() <= 0) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemStack.EMPTY);
                        }
                    }
                    
                    // 清理使用记录
                    playerUseRecords.remove(playerId);
                }
            });
            
            return null;
        }
    }
}

