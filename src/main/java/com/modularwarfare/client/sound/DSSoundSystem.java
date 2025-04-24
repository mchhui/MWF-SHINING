package com.modularwarfare.client.sound;

import com.modularwarfare.client.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.orecruncher.dsurround.client.sound.SoundEngine;
import org.orecruncher.dsurround.client.sound.SoundEffect;
import org.orecruncher.dsurround.client.sound.ISoundInstance;

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
        
        Vec3d soundPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        // 使用Builder创建音效
        ISoundInstance soundInstance = new SoundEffect.Builder(sound.getSoundName(), SoundCategory.PLAYERS)
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .createSoundAt(pos);
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
        //System.out.println("DSplayPosSound: " + sound.getSoundName());
    }
    
    /**
     * 播放绑定到实体音效
     */
    public static void playSound(Entity entity, SoundEvent sound, float volume, float pitch) {
        if (!ClientProxy.dsSurroundLoaded) return;
        
        // 使用Builder创建音效
        ISoundInstance soundInstance = new SoundEffect.Builder(sound.getSoundName(), SoundCategory.PLAYERS)
            .setVolume(volume)
            .setPitch(pitch)
            .build()
            .createSoundNear(entity);
            
        // 使用DS引擎播放音效
        SoundEngine.instance().playSound(soundInstance);
        //System.out.println("DSplayEnitySound: " + sound.getSoundName());
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
        //System.out.println("DSplaySelfSound: " + sound.getSoundName());
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