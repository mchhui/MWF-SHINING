 package com.modularwarfare.client.handler;

import org.lwjgl.input.Mouse;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGrenade;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.WeaponAnimationType;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID, value = Side.CLIENT)
public class GrenadeEnhancedHandler {
    public static boolean isHolding=false;
    public static boolean isThrowLow=false;
    public static boolean useKeyLock=false;
    public static long startHoldTime;
    public static ItemStack holdingStack;
    
    @SubscribeEvent
     public static void onTick(RenderTickEvent event) {
         if(event.phase!=Phase.END) {
             return;
         }
         if(Minecraft.getMinecraft().player==null) {
             return;
         }
         boolean isThrowingTemp=false;
         if(!Mouse.isButtonDown(Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode()+100)) {
             useKeyLock=false;
         }
         ItemStack stack=Minecraft.getMinecraft().player.getHeldItemMainhand();
         if(stack.getItem() instanceof ItemGrenade) {
             GrenadeType type=((ItemGrenade)stack.getItem()).type;
             if(type.animationType==WeaponAnimationType.ENHANCED) {
                 if(Mouse.isButtonDown(Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode()+100)) {
                     isThrowingTemp=true;
                 }
                 if(isThrowingTemp&&!isHolding) {
                     ClientRenderHooks.getEnhancedAnimMachine(Minecraft.getMinecraft().player).triggerThrow(AnimationController.getClientController(), Minecraft.getMinecraft().player, (ModelEnhancedGrenade)type.enhancedModel);
                     isThrowLow=false;
                     startHoldTime=System.currentTimeMillis();
                 }
                 if(isHolding) {
                     if(!useKeyLock&&Mouse.isButtonDown(Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode()+100)) {
                         useKeyLock=true;
                         isThrowLow=!isThrowLow;
                     }
                 }
             }
         }
         isHolding=isThrowingTemp;
     }
}
