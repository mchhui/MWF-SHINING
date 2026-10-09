package siz.addon.modularprops.common.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import com.modularwarfare.api.HandleKeyEvent;
import com.modularwarfare.client.input.KeyType;
import siz.addon.modularprops.client.fpp.enhanced.animation.CustomItemAnimationController;
import siz.addon.modularprops.client.fpp.enhanced.animation.CustomBlockAnimationController;
import siz.addon.modularprops.common.custom.CustomItemType;
import siz.addon.modularprops.common.custom.CustomBlockType;
import siz.addon.modularprops.common.custom.ItemCustom;
import siz.addon.modularprops.common.custom.ItemBlockCustom;
import siz.addon.modularprops.common.custom.BlockCustom;
import siz.addon.modularprops.common.network.PacketCustomItemUse;
import siz.addon.modularprops.ModularProps;
import com.modularwarfare.common.guns.WeaponSoundType;

import java.util.HashMap;
import java.util.UUID;

/**
 * 自定义道具和方块的事件处理器
 */
public class CustomItemEventHandler {
    
    // 存储每个玩家的动画控制器
    @SideOnly(Side.CLIENT)
    private static HashMap<UUID, CustomItemAnimationController> itemAnimationControllers = new HashMap<>();
    
    @SideOnly(Side.CLIENT)
    private static HashMap<UUID, CustomBlockAnimationController> blockAnimationControllers = new HashMap<>();
    
    // 存储 consumable 使用状态
    private static HashMap<UUID, ConsumableUseState> consumableUseStates = new HashMap<>();
    
    // 防护机制（参考手雷实现）
    @SideOnly(Side.CLIENT)
    private static ItemStack holdingStack = null; // 正在使用的道具副本
    @SideOnly(Side.CLIENT)
    private static int lastSlot = -1; // 上次的物品栏槽位
    
    // 动画更新时间跟踪（与 MWF 保持一致）
    private static long lastSyncTime = 0;
    private static final int SPS = 60; // 与 MWF 的 ClientTickHandler 保持一致
    
    private static class ConsumableUseState {
        ItemStack stack;
        EnumHand hand;
        long startTime;
        boolean cancelled = false;
        boolean commandExecuted = false; // 标记指令是否已执行
        boolean shouldConsume = false;   // 标记是否应该消耗（参考手雷的 isConsumed）
        float executePercent = 0.8f;     // 执行百分比
    }
    
    /**
     * 渲染 Tick 事件 - 更新动画（道具和方块）
     * 参考 ClientTickHandler 的实现，应该在 renderTick 中更新动画
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        
        ItemStack mainHand = player.getHeldItemMainhand();
        ItemStack offHand = player.getHeldItemOffhand();
        
        // === 防护机制：检测槽位切换或物品变化（参考手雷实现）===
        int currentSlot = player.inventory.currentItem;
        if (lastSlot != currentSlot) {
            ItemStack lastStack = lastSlot >= 0 ? player.inventory.getStackInSlot(lastSlot) : ItemStack.EMPTY;
            
            // 如果有正在使用的道具（holdingStack 不为空）
            if (holdingStack != null && holdingStack.getItem() instanceof ItemCustom) {
                UUID playerId = player.getUniqueID();
                ConsumableUseState state = consumableUseStates.get(playerId);
                
                // 关键检测：检查 lastStack 是否还是自定义道具且有待消耗状态
                if (!lastStack.isEmpty() && lastStack.getItem() instanceof ItemCustom && state != null) {
                    // 如果指令已执行且应该消耗，强制发送消耗包
                    if (state.commandExecuted && state.shouldConsume) {
                        ModularProps.NETWORK.sendToServer(new PacketCustomItemUse(false, true));
                        ModularProps.LOGGER.info("Slot/item changed - forced consume packet sent for item: " + 
                                               ((ItemCustom)holdingStack.getItem()).type.internalName);
                    }
                }
                
                // 重置状态（无论是否消耗）
                resetUseState(playerId);
            }
            lastSlot = currentSlot;
        }
        
        // 计算 stepTick，与 MWF 的 ClientTickHandler 保持一致
        long time = System.currentTimeMillis();
        if (time > lastSyncTime + 1000 / 144) {
            if (lastSyncTime > 0) {
                final float stepTick = (time - lastSyncTime) / (1000 / (float) SPS);
                
                // 处理主手道具
                if (mainHand.getItem() instanceof ItemCustom) {
                    ItemCustom item = (ItemCustom) mainHand.getItem();
                    CustomItemType itemType = item.type;
                    
                    if (itemType != null && itemType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                        CustomItemAnimationController controller = getOrCreateItemController(player.getUniqueID(), itemType);
                        if (controller != null) {
                            controller.onTickRender(stepTick, player);
                            
                            // 检查使用状态完成（支持所有有 consumableConfig 的道具）
                            if (itemType.consumableConfig != null) {
                                checkConsumableUseComplete(player, mainHand, EnumHand.MAIN_HAND, controller, itemType);
                            }
                        }
                    }
                }
                
                // 处理主手方块（第一人称动画）
                if (mainHand.getItem() instanceof ItemBlockCustom) {
                    ItemBlockCustom itemBlock = (ItemBlockCustom) mainHand.getItem();
                    BlockCustom block = (BlockCustom) itemBlock.getBlock();
                    CustomBlockType blockType = block.type;
                    
                    if (blockType != null && blockType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                        CustomBlockAnimationController controller = getOrCreateBlockController(player.getUniqueID(), blockType);
                        if (controller != null) {
                            controller.onTickRenderFirstPerson(stepTick, player);
                        }
                    }
                }
                
                // 处理副手道具
                if (offHand.getItem() instanceof ItemCustom) {
                    ItemCustom item = (ItemCustom) offHand.getItem();
                    CustomItemType itemType = item.type;
                    
                    if (itemType != null && itemType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                        CustomItemAnimationController controller = getOrCreateItemController(player.getUniqueID(), itemType);
                        if (controller != null) {
                            controller.onTickRender(stepTick, player);
                            
                            // 检查使用状态完成（支持所有有 consumableConfig 的道具）
                            if (itemType.consumableConfig != null) {
                                checkConsumableUseComplete(player, offHand, EnumHand.OFF_HAND, controller, itemType);
                            }
                        }
                    }
                }
            }
            lastSyncTime = time;
        }
    }
    
    /**
     * 检查 consumable 使用是否完成
     */
    @SideOnly(Side.CLIENT)
    private void checkConsumableUseComplete(EntityPlayerSP player, ItemStack stack, EnumHand hand, 
                                           CustomItemAnimationController controller, CustomItemType itemType) {
        UUID playerId = player.getUniqueID();
        ConsumableUseState state = consumableUseStates.get(playerId);
        
        if (state != null && state.stack == stack && state.hand == hand) {
            if (state.cancelled) {
                resetUseState(playerId);
                return;
            }
            
            // 检查是否达到执行百分比（只发送指令，不消耗）
            double useProgress = controller.USE;
            if (!state.commandExecuted && useProgress >= state.executePercent) {
                // 发送网络包到服务器执行指令（consumeItem=false，不在此时消耗）
                ModularProps.NETWORK.sendToServer(new PacketCustomItemUse(true, false));
                state.commandExecuted = true;
                
                ModularProps.LOGGER.info("Sent USE command packet to server for item: " + itemType.internalName + 
                                        " at progress: " + String.format("%.2f", useProgress * 100) + "%");
            }
            
            // 检查 USE 动画是否完全完成（参考手雷的 handlePostPhase）
            if (controller.isUseComplete() && !controller.isDrawing() && controller.ATTACK >= 1.0) {
                // 动画完全完成，现在才消耗道具
                if (state.shouldConsume) {
                    // 发送消耗网络包到服务器
                    ModularProps.NETWORK.sendToServer(new PacketCustomItemUse(false, true));
                    
                    // 客户端本地消耗（双端消耗，参考手雷）
                    if (!player.capabilities.isCreativeMode) {
                    stack.shrink(1);
                        if (stack.getCount() <= 0) {
                            if (hand == EnumHand.MAIN_HAND) {
                                player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemStack.EMPTY);
                            } else {
                                player.inventory.offHandInventory.set(0, ItemStack.EMPTY);
                            }
                        }
                        ModularProps.LOGGER.info("Client consumed item: " + itemType.internalName + " (animation complete)");
                    }
                }
                
                // 重置使用状态（包括 holdingStack）
                resetUseState(playerId);
            }
        }
    }
    
    /**
     * 重置使用状态（防护机制）
     */
    @SideOnly(Side.CLIENT)
    private void resetUseState(UUID playerId) {
                consumableUseStates.remove(playerId);
        holdingStack = null;
    }
    
    /**
     * 鼠标输入事件 - 处理攻击和使用
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        
        ItemStack mainHand = player.getHeldItemMainhand();
        ItemStack offHand = player.getHeldItemOffhand();
        
        // 处理主手道具
        if (mainHand.getItem() instanceof ItemCustom) {
            ItemCustom item = (ItemCustom) mainHand.getItem();
            CustomItemType itemType = item.type;
            
            if (itemType != null && itemType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                CustomItemAnimationController controller = getOrCreateItemController(player.getUniqueID(), itemType);
                if (controller != null) {
                    // 鼠标左键 - 攻击
                    if (Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown()) {
                        // 检查是否正在使用（打断逻辑）
                            UUID playerId = player.getUniqueID();
                            ConsumableUseState state = consumableUseStates.get(playerId);
                        boolean isInterrupting = false;
                        
                            if (state != null && state.stack == mainHand && state.hand == EnumHand.MAIN_HAND) {
                            // 正在使用，打断使用
                                state.cancelled = true;
                                controller.USE = 1.0; // 立即完成 USE 动画
                            isInterrupting = true;
                            }
                        
                        // 只有在不是打断的情况下才触发攻击动画
                        if (!isInterrupting) {
                            controller.triggerAttack();
                            // 音效由控制器自动播放
                        }
                    }
                    
                    // 鼠标右键 - 使用
                    if (Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown()) {
                        // 检查是否允许使用（usable 或有 consumableConfig）
                        boolean canUse = itemType.usable || 
                                       (itemType.consumableConfig != null);
                        
                        if (canUse && !controller.isUsing() && !controller.isDrawing() && controller.ATTACK >= 1.0) {
                            controller.triggerUse();
                            // 音效由控制器自动播放
                            
                            // 如果有 consumableConfig（不限于 CONSUMABLE 类型），记录使用状态
                            if (itemType.consumableConfig != null) {
                                ConsumableUseState state = new ConsumableUseState();
                                state.stack = mainHand;
                                state.hand = EnumHand.MAIN_HAND;
                                state.startTime = System.currentTimeMillis();
                                
                                // 获取执行百分比
                                if (itemType.enhancedModel != null && 
                                    itemType.enhancedModel.config instanceof siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig) {
                                    siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig config = 
                                        (siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig) itemType.enhancedModel.config;
                                    state.executePercent = config.useExecutePercent;
                                }
                                
                                // 判断是否需要消耗
                                state.shouldConsume = itemType.consumableConfig.consumeOnUse;
                                
                                consumableUseStates.put(player.getUniqueID(), state);
                                
                                // 保存正在使用的道具副本（防护机制）
                                holdingStack = mainHand.copy();
                            }
                        }
                    }
                }
            }
        }
        
        // 处理主手方块
        if (mainHand.getItem() instanceof ItemBlockCustom) {
            ItemBlockCustom itemBlock = (ItemBlockCustom) mainHand.getItem();
            BlockCustom block = (BlockCustom) itemBlock.getBlock();
            CustomBlockType blockType = block.type;
            
            if (blockType != null && blockType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                CustomBlockAnimationController controller = getOrCreateBlockController(player.getUniqueID(), blockType);
                if (controller != null) {
                    // 鼠标左键 - 攻击
                    if (Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown()) {
                        controller.triggerAttack();
                        playBlockAnimationSound(player, blockType, "attack");
                    }
                    
                    // 鼠标右键 - 使用
                    if (Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown()) {
                        if (!controller.isDrawing() && controller.ATTACK >= 1.0 && controller.PLACE >= 1.0) {
                            controller.triggerUse();
                            playBlockAnimationSound(player, blockType, "use");
                        }
                    }
                }
            }
        }
        
        // 处理副手道具（类似逻辑）
        if (offHand.getItem() instanceof ItemCustom) {
            ItemCustom item = (ItemCustom) offHand.getItem();
            CustomItemType itemType = item.type;
            
            if (itemType != null && itemType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                CustomItemAnimationController controller = getOrCreateItemController(player.getUniqueID(), itemType);
                if (controller != null) {
                    // 鼠标左键 - 攻击
                    if (Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown()) {
                        // 检查是否正在使用（打断逻辑）
                        UUID playerId = player.getUniqueID();
                        ConsumableUseState state = consumableUseStates.get(playerId);
                        boolean isInterrupting = false;
                        
                        if (state != null && state.stack == offHand && state.hand == EnumHand.OFF_HAND) {
                            // 正在使用，打断使用
                            state.cancelled = true;
                            controller.USE = 1.0; // 立即完成 USE 动画
                            isInterrupting = true;
                        }
                        
                        // 只有在不是打断的情况下才触发攻击动画
                        if (!isInterrupting) {
                        controller.triggerAttack();
                        // 音效由控制器自动播放
                        }
                    }
                    
                    // 鼠标右键 - 使用
                    if (Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown()) {
                        // 检查是否允许使用（usable 或有 consumableConfig）
                        boolean canUse = itemType.usable || 
                                       (itemType.consumableConfig != null);
                        
                        if (canUse && !controller.isUsing() && !controller.isDrawing() && controller.ATTACK >= 1.0) {
                            controller.triggerUse();
                            // 音效由控制器自动播放
                            
                            // 如果有 consumableConfig（不限于 CONSUMABLE 类型），记录使用状态
                            if (itemType.consumableConfig != null) {
                                ConsumableUseState state = new ConsumableUseState();
                                state.stack = offHand;
                                state.hand = EnumHand.OFF_HAND;
                                state.startTime = System.currentTimeMillis();
                                
                                // 获取执行百分比
                                if (itemType.enhancedModel != null && 
                                    itemType.enhancedModel.config instanceof siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig) {
                                    siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig config = 
                                        (siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig) itemType.enhancedModel.config;
                                    state.executePercent = config.useExecutePercent;
                                }
                                
                                // 判断是否需要消耗
                                state.shouldConsume = itemType.consumableConfig.consumeOnUse;
                                
                                consumableUseStates.put(player.getUniqueID(), state);
                                
                                // 保存正在使用的道具副本（防护机制）
                                holdingStack = offHand.copy();
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 方块放置事件 - 触发第一人称放置动画
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onPlayerInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() != net.minecraftforge.fml.relauncher.Side.CLIENT) {
            return;
        }
        
        EntityPlayerSP player = (EntityPlayerSP) event.getEntityPlayer();
        if (player == null) {
            return;
        }
        
        ItemStack heldItem = player.getHeldItem(event.getHand());
        if (heldItem.getItem() instanceof ItemBlockCustom) {
            ItemBlockCustom itemBlock = (ItemBlockCustom) heldItem.getItem();
            BlockCustom block = (BlockCustom) itemBlock.getBlock();
            CustomBlockType blockType = block.type;
            
            if (blockType != null && blockType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                CustomBlockAnimationController controller = getOrCreateBlockController(player.getUniqueID(), blockType);
                if (controller != null && !controller.isDrawing() && controller.ATTACK >= 1.0 && 
                    controller.USE >= 1.0 && controller.PLACE >= 1.0) {
                    // 触发第一人称放置动画
                    controller.triggerPlace();
                    playBlockAnimationSound(player, blockType, "place");
                }
            }
        }
    }
    
    /**
     * 监听 MWF 的按键事件 - 处理视检和方块放置
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onHandleKeyEvent(HandleKeyEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        
        ItemStack mainHand = player.getHeldItemMainhand();
        
        // 处理视检键（N键）
        if (event.keyType == KeyType.Inspect) {
            if (mainHand.getItem() instanceof ItemCustom) {
                ItemCustom item = (ItemCustom) mainHand.getItem();
                CustomItemType itemType = item.type;
                
                if (itemType != null && itemType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                    CustomItemAnimationController controller = getOrCreateItemController(player.getUniqueID(), itemType);
                    if (controller != null && !controller.isDrawing() && controller.ATTACK >= 1.0 && controller.USE >= 1.0) {
                        controller.triggerInspect();
                        playAnimationSound(player, itemType, "inspect");
                    }
                }
            } else if (mainHand.getItem() instanceof ItemBlockCustom) {
                // 方块的第一人称视检
                ItemBlockCustom itemBlock = (ItemBlockCustom) mainHand.getItem();
                BlockCustom block = (BlockCustom) itemBlock.getBlock();
                CustomBlockType blockType = block.type;
                
                if (blockType != null && blockType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                    CustomBlockAnimationController controller = getOrCreateBlockController(player.getUniqueID(), blockType);
                    if (controller != null && !controller.isDrawing() && controller.ATTACK >= 1.0 && 
                        controller.USE >= 1.0 && controller.PLACE >= 1.0) {
                        controller.triggerInspect();
                        playBlockAnimationSound(player, blockType, "inspect");
                    }
                }
            }
        }
        
        // 处理变形键（G键）- 用于方块放置动画（第一人称）
        // 注意：实际的方块放置由 Minecraft 的右键点击处理，这里只是触发动画
        // 真正的放置动画应该在 PlayerInteractEvent.RightClickBlock 中处理
    }
    
    /**
     * 播放动画音效
     */
    @SideOnly(Side.CLIENT)
    private void playAnimationSound(EntityPlayerSP player, CustomItemType itemType, String animationName) {
        if (itemType.weaponSoundMap == null) {
            return;
        }
        
        // 根据动画名称查找对应的音效类型
        WeaponSoundType soundType = null;
        switch (animationName) {
            case "draw":
                soundType = WeaponSoundType.Draw;
                break;
            case "attack":
                // 使用自定义音效类型，需要扩展 WeaponSoundType 或使用现有类型
                soundType = WeaponSoundType.ModeSwitch; // 临时使用
                break;
            case "use":
                soundType = WeaponSoundType.ModeSwitch; // 临时使用
                break;
            case "inspect":
                soundType = WeaponSoundType.Inspect;
                break;
        }
        
        if (soundType != null) {
            itemType.playClientSound(player, soundType);
        }
    }
    
    /**
     * 获取或创建道具动画控制器
     * 参考 AnimationController.getController() 的实现，检查 itemType 是否改变
     */
    @SideOnly(Side.CLIENT)
    private CustomItemAnimationController getOrCreateItemController(UUID playerId, CustomItemType itemType) {
        if (itemType.enhancedModel == null || itemType.enhancedModel.config == null) {
            return null;
        }
        
        CustomItemAnimationController controller = itemAnimationControllers.get(playerId);
        
        // 如果控制器不存在、配置改变、或道具类型改变，创建新的控制器
        if (controller == null || 
            controller.getCurrentAnimation() == null ||
            controller.getConfig() != itemType.enhancedModel.config ||
            controller.getItemType() != itemType) {
            
            controller = new CustomItemAnimationController(
                (siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig) itemType.enhancedModel.config,
                itemType
            );
            itemAnimationControllers.put(playerId, controller);
        }
        return controller;
    }
    
    /**
     * 静态方法：获取道具动画控制器（供渲染器使用）
     */
    @SideOnly(Side.CLIENT)
    public static CustomItemAnimationController getItemAnimationController(UUID playerId, CustomItemType itemType) {
        if (itemType.enhancedModel == null || itemType.enhancedModel.config == null) {
            return null;
        }
        return itemAnimationControllers.get(playerId);
    }
    
    /**
     * 获取或创建方块动画控制器
     * 参考 AnimationController.getController() 的实现，检查 blockType 是否改变
     */
    @SideOnly(Side.CLIENT)
    private CustomBlockAnimationController getOrCreateBlockController(UUID playerId, CustomBlockType blockType) {
        if (blockType.enhancedModel == null || blockType.enhancedModel.config == null) {
            return null;
        }
        
        CustomBlockAnimationController controller = blockAnimationControllers.get(playerId);
        
        // 如果控制器不存在、配置改变、或方块类型改变，创建新的控制器
        if (controller == null || 
            controller.getCurrentAnimation() == null || 
            controller.getConfig() != blockType.enhancedModel.config ||
            controller.getBlockType() != blockType) {
            
            controller = new CustomBlockAnimationController(
                (siz.addon.modularprops.client.fpp.enhanced.configs.CustomBlockEnhancedRenderConfig) blockType.enhancedModel.config,
                blockType // 传递 blockType 用于音效
            );
            blockAnimationControllers.put(playerId, controller);
        }
        return controller;
    }
    
    /**
     * 静态方法：获取方块动画控制器（供渲染器使用）
     */
    @SideOnly(Side.CLIENT)
    public static CustomBlockAnimationController getBlockAnimationController(UUID playerId, CustomBlockType blockType) {
        if (blockType.enhancedModel == null || blockType.enhancedModel.config == null) {
            return null;
        }
        return blockAnimationControllers.get(playerId);
    }
    
    /**
     * 播放方块动画音效
     */
    @SideOnly(Side.CLIENT)
    private void playBlockAnimationSound(EntityPlayerSP player, CustomBlockType blockType, String animationName) {
        if (blockType.weaponSoundMap == null) {
            return;
        }
        
        // 根据动画名称查找对应的音效类型
        WeaponSoundType soundType = null;
        switch (animationName) {
            case "draw":
                soundType = WeaponSoundType.Draw;
                break;
            case "attack":
                soundType = WeaponSoundType.ModeSwitch; // 临时使用
                break;
            case "use":
                soundType = WeaponSoundType.ModeSwitch; // 临时使用
                break;
            case "inspect":
                soundType = WeaponSoundType.Inspect;
                break;
            case "place":
                soundType = WeaponSoundType.ModeSwitch; // 临时使用
                break;
        }
        
        if (soundType != null) {
            blockType.playClientSound(player, soundType);
        }
    }
    
    /**
     * 玩家切换物品时重置动画控制器
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == Side.CLIENT) {
            EntityPlayer player = event.player;
            ItemStack mainHand = player.getHeldItemMainhand();
            
            // 如果切换了物品，清除旧的控制器
            if (!(mainHand.getItem() instanceof ItemCustom)) {
                itemAnimationControllers.remove(player.getUniqueID());
            }
        }
    }
    
    /**
     * 处理丢弃键防护（由核心模组的 ItemDropProtectionHandler 调用）
     * 当玩家按下丢弃键时，如果道具已执行指令且标记为消耗，则强制发送消耗包
     */
    public static void handleDropProtection(UUID playerId, ItemStack stack) {
        ConsumableUseState state = consumableUseStates.get(playerId);
        
        // 检查是否有待消耗状态
        if (state != null && state.commandExecuted && state.shouldConsume) {
            // 检查是否是同一个道具
            if (state.stack.getItem() == stack.getItem()) {
                // 强制发送消耗包到服务器
                ModularProps.NETWORK.sendToServer(new PacketCustomItemUse(false, true));
                
                ItemCustom item = (ItemCustom) stack.getItem();
                ModularProps.LOGGER.info("Drop key detected - forced consume custom item: " + item.type.internalName);
                
                // 重置状态
                consumableUseStates.remove(playerId);
                holdingStack = null;
            }
        }
    }
}

