package com.modularwarfare.common.effect;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.handler.SensitivityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StunPotion extends Potion {
    private static final Logger LOGGER = LogManager.getLogger(ModularWarfare.MOD_ID);
    
    /**
     * 保存被移除的AI任务映射
     */
    private static Map<EntityLiving, Map<Integer, Boolean>> disabledAITasks = new HashMap<>();
    private static Map<EntityLiving, Map<Integer, Boolean>> disabledTargetTasks = new HashMap<>();
    
    /**
     * 存储玩家眩晕效果的级别
     */
    private static Map<EntityPlayer, Integer> playerStunLevels = new HashMap<>();
    
    /**
     * 注册事件处理器，确保在所有情况下都能恢复灵敏度
     */
    static {
        MinecraftForge.EVENT_BUS.register(StunPotion.class);
    }
    
    public StunPotion() {
        // 参数: 是否有害效果, 颜色
        super(true, 0xE0E0E0); // 使用浅灰色作为药水颜色，设置为有害效果
        
        // 设置药水名称和图标
        setPotionName("effect.stun");
        setRegistryName(ModularWarfare.MOD_ID, "stun");
        setIconIndex(0, 0); // 使用原版药水图标位置
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        // 每tick都应用效果
        return true;
    }
    
    @Override
    public void performEffect(EntityLivingBase entity, int amplifier) {
        // 区分客户端和服务端效果
        if (entity.world.isRemote) {
            clientSidePerformEffect(entity, amplifier);
        } else {
            serverSidePerformEffect(entity, amplifier);
        }
    }
    
    @SideOnly(Side.CLIENT)
    private void clientSidePerformEffect(EntityLivingBase entity, int amplifier) {
        Minecraft mc = Minecraft.getMinecraft();
        
        // 客户端只处理玩家的鼠标灵敏度
        if (entity instanceof EntityPlayer && entity == mc.player) {
            EntityPlayer player = (EntityPlayer) entity;
            
            // 保存当前玩家的眩晕级别
            playerStunLevels.put(player, amplifier);
            
            // 根据效果强度计算灵敏度乘数
            float reductionFactor = Math.min(0.9f, 0.5f + (amplifier * 0.2f));
            float multiplier = 1.0f - reductionFactor;
            
            // 设置灵敏度乘数
            SensitivityHandler.getInstance().setSensitivityMultiplier(SensitivityHandler.STUN_EFFECT, multiplier);
        }
    }
    
    private void serverSidePerformEffect(EntityLivingBase entity, int amplifier) {
        // 服务端处理实体AI禁锢
        if (entity instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) entity;
            
            // 首次应用效果时，禁用AI任务
            disableAITasks(living);
        }
    }
    
    @Override
    public void removeAttributesModifiersFromEntity(EntityLivingBase entity, net.minecraft.entity.ai.attributes.AbstractAttributeMap attributeMap, int amplifier) {
        super.removeAttributesModifiersFromEntity(entity, attributeMap, amplifier);
        
        // 效果结束时，恢复实体状态
        if (entity.world.isRemote) {
            removeClientSideEffect(entity);
        } else {
            removeServerSideEffect(entity);
        }
    }
    
    /**
     * 确保玩家灵敏度在所有情况下都能恢复
     * 
     * @param entity 实体
     */
    @SideOnly(Side.CLIENT)
    private void removeClientSideEffect(EntityLivingBase entity) {
        Minecraft mc = Minecraft.getMinecraft();
        
        // 恢复玩家的鼠标灵敏度
        if (entity instanceof EntityPlayer && entity == mc.player) {
            EntityPlayer player = (EntityPlayer) entity;
            
            // 移除灵敏度乘数
            SensitivityHandler.getInstance().removeSensitivityMultiplier(SensitivityHandler.STUN_EFFECT);
            
            // 清除玩家眩晕级别记录
            playerStunLevels.remove(player);
        }
    }
    
    private void removeServerSideEffect(EntityLivingBase entity) {
        if (entity instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) entity;
            
            // 恢复AI任务
            restoreAITasks(living);
        }
    }
    
    private void disableAITasks(EntityLiving entity) {
        // 存储当前AI任务状态（如果尚未存储）
        if (!disabledAITasks.containsKey(entity)) {
            Map<Integer, Boolean> aiTasksMap = new HashMap<>();
            Map<Integer, Boolean> targetTasksMap = new HashMap<>();
            
            // 保存并禁用所有AI任务
            for (int i = 0; i < entity.tasks.taskEntries.size(); i++) {
                net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry task = 
                    (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry) entity.tasks.taskEntries.toArray()[i];
                aiTasksMap.put(i, task.using);
                task.using = false;
            }
            
            // 保存并禁用所有目标任务
            for (int i = 0; i < entity.targetTasks.taskEntries.size(); i++) {
                net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry task = 
                    (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry) entity.targetTasks.taskEntries.toArray()[i];
                targetTasksMap.put(i, task.using);
                task.using = false;
            }
            
            disabledAITasks.put(entity, aiTasksMap);
            disabledTargetTasks.put(entity, targetTasksMap);
        }
    }
    
    private void restoreAITasks(EntityLiving entity) {
        // 恢复AI任务（如果已存储）
        if (disabledAITasks.containsKey(entity)) {
            Map<Integer, Boolean> aiTasksMap = disabledAITasks.get(entity);
            
            // 恢复所有AI任务
            for (int i = 0; i < entity.tasks.taskEntries.size() && i < aiTasksMap.size(); i++) {
                net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry task = 
                    (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry) entity.tasks.taskEntries.toArray()[i];
                task.using = aiTasksMap.getOrDefault(i, true);
            }
            
            disabledAITasks.remove(entity);
        }
        
        // 恢复目标任务（如果已存储）
        if (disabledTargetTasks.containsKey(entity)) {
            Map<Integer, Boolean> targetTasksMap = disabledTargetTasks.get(entity);
            
            // 恢复所有目标任务
            for (int i = 0; i < entity.targetTasks.taskEntries.size() && i < targetTasksMap.size(); i++) {
                net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry task = 
                    (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry) entity.targetTasks.taskEntries.toArray()[i];
                task.using = targetTasksMap.getOrDefault(i, true);
            }
            
            disabledTargetTasks.remove(entity);
        }
    }
    
    /**
     * 监听玩家退出世界事件，恢复灵敏度
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (Minecraft.getMinecraft().player != null && event.player == Minecraft.getMinecraft().player) {
            // 恢复所有保存的灵敏度
            for (Map.Entry<EntityPlayer, Integer> entry : playerStunLevels.entrySet()) {
                SensitivityHandler.getInstance().removeSensitivityMultiplier(SensitivityHandler.STUN_EFFECT);
            }
            playerStunLevels.clear();
        }
    }
    
    /**
     * 客户端tick事件，用于检查并恢复任何未正确清除的灵敏度
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getMinecraft().player != null) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            
            // 检查玩家是否仍有眩晕效果
            boolean hasStunEffect = player.getActivePotionEffect(ModPotions.STUN_POTION) != null;
            
            // 如果玩家不再有眩晕效果但仍在映射中，则移除影响
            if (!hasStunEffect && playerStunLevels.containsKey(player)) {
                SensitivityHandler.getInstance().removeSensitivityMultiplier(SensitivityHandler.STUN_EFFECT);
                playerStunLevels.remove(player);
            }
        }
    }
    
    /**
     * 清理死亡实体的引用，防止内存泄漏
     */
    public static void cleanupDeadEntities() {
        // 清理死亡或无效实体的AI映射
        Iterator<EntityLiving> aiIterator = disabledAITasks.keySet().iterator();
        while (aiIterator.hasNext()) {
            EntityLiving entity = aiIterator.next();
            if (entity == null || entity.isDead) {
                aiIterator.remove();
            }
        }
        
        Iterator<EntityLiving> targetIterator = disabledTargetTasks.keySet().iterator();
        while (targetIterator.hasNext()) {
            EntityLiving entity = targetIterator.next();
            if (entity == null || entity.isDead) {
                targetIterator.remove();
            }
        }
        
        // 清理死亡或无效玩家的灵敏度映射
        Iterator<EntityPlayer> playerIterator = playerStunLevels.keySet().iterator();
        while (playerIterator.hasNext()) {
            EntityPlayer player = playerIterator.next();
            if (player == null || player.isDead) {
                // 如果在客户端，恢复灵敏度
                if (Minecraft.getMinecraft().player != null && player == Minecraft.getMinecraft().player) {
                    SensitivityHandler.getInstance().removeSensitivityMultiplier(SensitivityHandler.STUN_EFFECT);
                }
                playerIterator.remove();
            }
        }
    }
} 