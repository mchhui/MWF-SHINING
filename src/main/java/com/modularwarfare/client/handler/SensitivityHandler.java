package com.modularwarfare.client.handler;

import com.modularwarfare.ModularWarfare;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 统一管理游戏中各种因素对鼠标灵敏度的影响
 * 通过乘法系统实现，各个效果提供自己的乘数，最终一起应用
 */
@SideOnly(Side.CLIENT)
public class SensitivityHandler {

    private static final Logger LOGGER = LogManager.getLogger(ModularWarfare.MOD_ID);
    private static SensitivityHandler INSTANCE;
    
    // 玩家原始的灵敏度设置
    private float originalSensitivity = 0.5f;
    
    // 存储各种来源的灵敏度乘数
    private Map<String, Float> sensitivityMultipliers = new HashMap<>();
    
    // 是否已初始化
    private boolean initialized = false;
    
    // 是否需要更新灵敏度
    private boolean needsUpdate = false;
    
    // 常量定义
    public static final String SCOPE_EFFECT = "scope";
    public static final String STUN_EFFECT = "stun";
    
    /**
     * 获取单例实例
     */
    public static SensitivityHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SensitivityHandler();
        }
        return INSTANCE;
    }
    
    /**
     * 私有构造函数，注册事件处理器
     */
    private SensitivityHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * 设置指定来源的灵敏度乘数
     * @param source 来源标识符
     * @param multiplier 乘数值（1.0f为不变，小于1.0f降低灵敏度，大于1.0f提高灵敏度）
     */
    public void setSensitivityMultiplier(String source, float multiplier) {
        if (source == null || source.isEmpty()) {
            return;
        }
        
        // 检查是否需要初始化
        checkInitialization();
        
        // 如果乘数为1.0f，则移除该来源的影响
        if (Math.abs(multiplier - 1.0f) < 0.001f) {
            if (sensitivityMultipliers.containsKey(source)) {
                sensitivityMultipliers.remove(source);
                needsUpdate = true;
            }
            return;
        }
        
        // 更新乘数
        Float oldValue = sensitivityMultipliers.get(source);
        if (oldValue == null || Math.abs(oldValue - multiplier) > 0.001f) {
            sensitivityMultipliers.put(source, multiplier);
            needsUpdate = true;
        }
    }
    
    /**
     * 移除指定来源的灵敏度乘数
     * @param source 来源标识符
     */
    public void removeSensitivityMultiplier(String source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        
        if (sensitivityMultipliers.containsKey(source)) {
            sensitivityMultipliers.remove(source);
            needsUpdate = true;
        }
    }
    
    /**
     * 清除所有灵敏度乘数
     */
    public void clearAllMultipliers() {
        if (!sensitivityMultipliers.isEmpty()) {
            sensitivityMultipliers.clear();
            needsUpdate = true;
        }
    }
    
    /**
     * 获取当前综合灵敏度乘数
     * @return 所有乘数相乘的结果
     */
    public float getCombinedMultiplier() {
        if (sensitivityMultipliers.isEmpty()) {
            return 1.0f;
        }
        
        float combined = 1.0f;
        for (float multiplier : sensitivityMultipliers.values()) {
            combined *= multiplier;
        }
        return combined;
    }
    
    /**
     * 应用当前的灵敏度设置
     */
    public void applyCurrentSensitivity() {
        if (!initialized) {
            return;
        }
        
        float multiplier = getCombinedMultiplier();
        float newSensitivity = originalSensitivity * multiplier;
        
        // 确保灵敏度不会小于最小值
        newSensitivity = Math.max(0.05f, newSensitivity);
        
        // 设置新的灵敏度值
        Minecraft.getMinecraft().gameSettings.mouseSensitivity = newSensitivity;
    }
    
    /**
     * 检查并初始化
     */
    private void checkInitialization() {
        if (!initialized && Minecraft.getMinecraft().player != null) {
            originalSensitivity = Minecraft.getMinecraft().gameSettings.mouseSensitivity;
            initialized = true;
        }
    }
    
    /**
     * 重置为原始灵敏度
     */
    public void resetToOriginal() {
        if (initialized) {
            Minecraft.getMinecraft().gameSettings.mouseSensitivity = originalSensitivity;
            sensitivityMultipliers.clear();
        }
    }
    
    /**
     * 获取原始灵敏度值
     */
    public float getOriginalSensitivity() {
        checkInitialization();
        return originalSensitivity;
    }
    
    /**
     * 更新原始灵敏度（当玩家主动修改设置时）
     */
    public void updateOriginalSensitivity(float newValue) {
        // 只有当没有任何效果影响时才更新原始值
        if (sensitivityMultipliers.isEmpty()) {
            originalSensitivity = newValue;
        }
    }
    
    /**
     * 客户端tick事件处理
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 检查初始化
            if (!initialized && Minecraft.getMinecraft().player != null) {
                checkInitialization();
            }
            
            // 如果需要更新并且已初始化，则应用新的灵敏度
            if (needsUpdate && initialized) {
                applyCurrentSensitivity();
                needsUpdate = false;
            }
            
            // 玩家退出时重置
            if (initialized && Minecraft.getMinecraft().player == null) {
                resetToOriginal();
                initialized = false;
            }
            
            // 检测玩家是否手动修改了灵敏度设置
            if (initialized && sensitivityMultipliers.isEmpty() && 
                    Math.abs(Minecraft.getMinecraft().gameSettings.mouseSensitivity - originalSensitivity) > 0.001f) {
                updateOriginalSensitivity(Minecraft.getMinecraft().gameSettings.mouseSensitivity);
            }
        }
    }
} 