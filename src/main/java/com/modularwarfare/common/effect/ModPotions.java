package com.modularwarfare.common.effect;

import com.modularwarfare.ModularWarfare;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID)
public class ModPotions {
    
    // 实例引用
    public static StunPotion STUN_POTION;
    
    /**
     * 注册所有自定义药水效果
     */
    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        // 创建眩晕药水效果
        STUN_POTION = new StunPotion();
        
        // 注册到游戏
        event.getRegistry().register(STUN_POTION);
        
        ModularWarfare.LOGGER.info("Registered custom potion effects");
    }
    
    /**
     * 注册药水类型（药水瓶）- 这里不需要，因为我们只通过爆炸应用效果
     */
    @SubscribeEvent
    public static void registerPotionTypes(RegistryEvent.Register<PotionType> event) {
        // 这里暂时不需要注册药水类型
    }
    
    /**
     * 清理逻辑
     */
    public static void onWorldTick() {
        // 定期清理已死亡的实体
        StunPotion.cleanupDeadEntities();
    }
} 