package com.modularwarfare.client.sound;

import com.modularwarfare.client.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.orecruncher.dsurround.client.sound.SoundEngine;
import org.orecruncher.dsurround.client.sound.SoundEffect;
import org.orecruncher.dsurround.client.sound.ISoundInstance;
import org.orecruncher.dsurround.client.sound.SoundBuilder;

/**
 * Dynamic Surroundings音效系统集成工具类
 */
public class DSSoundSystem {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    /**
     * 播放3D位置音效
     */
    public static void playSound(BlockPos pos, SoundEvent sound, float volume, float pitch) {
        if (!ClientProxy.dsSurroundLoaded) return;
        
        // 使用Builder创建音效
        ISoundInstance soundInstance = new SoundEffect.Builder(sound.getSoundName(), SoundCategory.PLAYERS)
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .createSoundAt(pos)
            .setAttenuationType(ISoundInstance.AttenuationType.NONE);
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
    }

    /**
     * 播放3D位置音效(线性衰减)
     */
    public static void playSoundLinear(BlockPos pos, SoundEvent sound, float volume, float pitch) {
        if (!ClientProxy.dsSurroundLoaded) return;
        
        // 使用Builder创建音效
        ISoundInstance soundInstance = new SoundEffect.Builder(sound.getSoundName(), SoundCategory.PLAYERS)
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .createSoundAt(pos)
            .setAttenuationType(ISoundInstance.AttenuationType.LINEAR);
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
    }
    
    /**
     * 播放绑定到实体音效
     */
    public static void playEntitySound(Entity entity, SoundEvent sound, float volume, float pitch) {
        if (!ClientProxy.dsSurroundLoaded) return;
        
        // 使用Builder创建音效
        ISoundInstance soundInstance = new SoundEffect.Builder(sound.getSoundName(), SoundCategory.PLAYERS)
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .createSoundNear(entity)
            .setAttenuationType(ISoundInstance.AttenuationType.NONE);
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
    }

    /**
     * 播放仅玩家自己可听见的音效
     */
    public static void playSelfSound(SoundEvent sound, float volume, float pitch) {
        if (!ClientProxy.dsSurroundLoaded) return;
        
        // 创建无衰减的音效实例
        ISoundInstance soundInstance = new SoundEffect.Builder(sound.getSoundName(), SoundCategory.MASTER)
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .createTrackingSound(mc.player, false);  // 使用tracking sound并绑定到玩家
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
    }

    /**
     * 播放3D位置音效（简单高效版本）
     */
    public static void playSoundSimple(BlockPos pos, SoundEvent sound, float volume, float pitch) {
        if (!ClientProxy.dsSurroundLoaded) return;
        
        // 使用SoundBuilder直接创建音效 - 简单高效
        ISoundInstance soundInstance = SoundBuilder.builder(sound, SoundCategory.PLAYERS)
            .setPosition(pos)
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .setAttenuationType(ISoundInstance.AttenuationType.NONE);
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
    }

    /**
     * 播放3D位置音效(线性衰减)（简单高效版本）
     */
    public static void playSoundLinearSimple(BlockPos pos, SoundEvent sound, float volume, float pitch) {
        if (!ClientProxy.dsSurroundLoaded) return;
        
        // 使用SoundBuilder直接创建音效 - 简单高效
        ISoundInstance soundInstance = SoundBuilder.builder(sound, SoundCategory.PLAYERS)
            .setPosition(pos)
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .setAttenuationType(ISoundInstance.AttenuationType.LINEAR);
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
    }

    /**
     * 播放绑定到实体音效（简单高效版本）
     */
    public static void playEntitySoundSimple(Entity entity, SoundEvent sound, float volume, float pitch) {
        if (!ClientProxy.dsSurroundLoaded) return;
        
        // 使用SoundBuilder直接创建音效 - 简单高效
        // 获取实体位置并添加随机偏移（模拟createSoundNear的效果）
        final float posX = (float) (entity.posX + (Math.random() - 0.5) * 16);
        final float posY = (float) (entity.posY + entity.getEyeHeight() + (Math.random() - 0.5) * 16);
        final float posZ = (float) (entity.posZ + (Math.random() - 0.5) * 16);
        
        ISoundInstance soundInstance = SoundBuilder.builder(sound, SoundCategory.PLAYERS)
            .setPosition(posX, posY, posZ)
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .setAttenuationType(ISoundInstance.AttenuationType.NONE);
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
    }

    /**
     * 播放仅玩家自己可听见的音效（简单高效版本）
     */
    public static void playSelfSoundSimple(SoundEvent sound, float volume, float pitch) {
        if (!ClientProxy.dsSurroundLoaded) return;
        
        // 使用SoundBuilder直接创建无衰减音效 - 简单高效
        ISoundInstance soundInstance = SoundBuilder.builder(sound, SoundCategory.MASTER)
            .setPosition(0, 0, 0)  // 位置设为原点，因为使用NONE衰减
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .setAttenuationType(ISoundInstance.AttenuationType.NONE);
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
    }
    
    /**
     * 停止所有音效
     */
    public static void stopAllSounds() {
        if (!ClientProxy.dsSurroundLoaded) return;
        SoundEngine.instance().stopAllSounds();
    }
    
    /**
     * 停止特定音效
     */
    public static void stopSound(ISoundInstance sound) {
        if (!ClientProxy.dsSurroundLoaded) return;
        SoundEngine.instance().stopSound(sound);
    }
} 