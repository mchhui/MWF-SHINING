package com.modularwarfare.client.handler;

import com.modularwarfare.ModConfig;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.animations.AnimStateMachine;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.utility.DevGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RenderGuiHandler {

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.getType() != ElementType.EXPERIENCE) return;

        if (!ModConfig.INSTANCE.dev_mode) return;

        if (Minecraft.getMinecraft().player != null) {
            EntityPlayerSP entityPlayer = Minecraft.getMinecraft().player;
            if (entityPlayer.getHeldItemMainhand() != null && entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun) {
                ItemGun itemGun = (ItemGun) entityPlayer.getHeldItemMainhand().getItem();
                GunType gunType = itemGun.type;
                
                if (gunType.animationType == WeaponAnimationType.ENHANCED) {
                    EnhancedStateMachine enhancedAnim = ClientRenderHooks.getEnhancedAnimMachine(entityPlayer);
                    new DevGui(Minecraft.getMinecraft(), entityPlayer.getHeldItemMainhand(), itemGun, null, null);
                } else {
                    AnimStateMachine anim = ClientRenderHooks.getAnimMachine(entityPlayer);
                    new DevGui(Minecraft.getMinecraft(), entityPlayer.getHeldItemMainhand(), itemGun, ClientProxy.gunStaticRenderer, anim);
                }
            }
        }
    }
}