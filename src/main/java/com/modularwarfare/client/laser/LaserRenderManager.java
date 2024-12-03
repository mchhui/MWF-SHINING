package com.modularwarfare.client.laser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;

import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.common.guns.ItemGun;

public class LaserRenderManager {
    private static LaserRenderManager INSTANCE = new LaserRenderManager();
    private List<LaserDotInfo> laserDots = new ArrayList<>();
    private HashMap<UUID, Boolean> laserStates = new HashMap<>();
    
    public static LaserRenderManager getInstance() {
        return INSTANCE;
    }
    
    public static class LaserDotInfo {
        public float[] color;
        public float alpha;
        public float dotSize;
        public double maxDistance;
        public boolean visible;
        public UUID playerId;
        
        public LaserDotInfo(float[] color, float alpha, float dotSize, double maxDistance, boolean visible, UUID playerId) {
            this.color = color;
            this.alpha = alpha;
            this.dotSize = dotSize;
            this.maxDistance = maxDistance;
            this.visible = visible;
            this.playerId = playerId;
        }
    }
    
    public void setLaserState(UUID playerId, boolean enabled) {
        laserStates.put(playerId, enabled);
    }
    
    public boolean getLaserState(UUID playerId) {
        return laserStates.getOrDefault(playerId, false);
    }
    
    public void addLaserDot(float[] color, float alpha, float dotSize, double maxDistance, boolean visible, UUID playerId, boolean applySprint, float collideFrontDistance, AnimationType currentAction) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if(player != null && player.getUniqueID().equals(playerId)) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if(heldItem.getItem() instanceof ItemGun) {
                boolean laserEnabled = ((ItemGun)heldItem.getItem()).getLaserEnabled(heldItem);
                if(!laserEnabled) {
                    return;
                }
            }
        }
        if(getLaserState(playerId)) {
            boolean isInAction = currentAction == AnimationType.PRE_RELOAD
                || currentAction == AnimationType.RELOAD_FIRST
                || currentAction == AnimationType.RELOAD_SECOND
                || currentAction == AnimationType.POST_RELOAD
                || currentAction == AnimationType.PRE_LOAD
                || currentAction == AnimationType.LOAD
                || currentAction == AnimationType.POST_LOAD
                || currentAction == AnimationType.PRE_UNLOAD
                || currentAction == AnimationType.UNLOAD
                || currentAction == AnimationType.POST_UNLOAD
                || currentAction == AnimationType.PRE_RELOAD_EMPTY
                || currentAction == AnimationType.RELOAD_FIRST_EMPTY
                || currentAction == AnimationType.RELOAD_SECOND_EMPTY
                || currentAction == AnimationType.POST_RELOAD_EMPTY
                || currentAction == AnimationType.RELOAD_FIRST_QUICKLY
                || currentAction == AnimationType.RELOAD_SECOND_QUICKLY
                || currentAction == AnimationType.DRAW
                || currentAction == AnimationType.DRAW_EMPTY
                || currentAction == AnimationType.TAKEDOWN
                || currentAction == AnimationType.TAKEDOWN_EMPTY
                || currentAction == AnimationType.INSPECT
                || currentAction == AnimationType.INSPECT_EMPTY;
    
            if(!applySprint && collideFrontDistance <= 0.2f && !isInAction) {
                laserDots.add(new LaserDotInfo(color, alpha, dotSize, maxDistance, visible, playerId));
            }
        }
    }
    
    public void clearLaserDots() {
        laserDots.clear();
    }
    
    public List<LaserDotInfo> getLaserDots() {
        return laserDots;
    }
    
    public void toggleLaserState(UUID playerId) {
        boolean currentState = getLaserState(playerId);
        setLaserState(playerId, !currentState);
    }
}