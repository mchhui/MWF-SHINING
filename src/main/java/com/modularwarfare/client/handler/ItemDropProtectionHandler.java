package com.modularwarfare.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.network.PacketGrenadeConsume;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID, value = Side.CLIENT)
public class ItemDropProtectionHandler {
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        
        if (player == null || mc.currentScreen != null) {
            return;
        }
        
        int dropKey = mc.gameSettings.keyBindDrop.getKeyCode();
        int eventKey = Keyboard.getEventKey();
        
        if (eventKey == dropKey && Keyboard.getEventKeyState()) {
            ItemStack mainHand = player.getHeldItemMainhand();
            
            if (mainHand.isEmpty()) {
                return;
            }
            
            if (mainHand.getItem() instanceof ItemGrenade) {
                if (GrenadeEnhancedHandler.isConsumed) {
                    ModularWarfare.NETWORK.sendToServer(new PacketGrenadeConsume());
                    ModularWarfare.LOGGER.info("Drop key detected - forced consume grenade");
                    
                    GrenadeEnhancedHandler.isConsumed = false;
                    GrenadeEnhancedHandler.holdingStack = null;
                    GrenadeEnhancedHandler.isHolding = false;
                }
            }
            

            try {
                Class<?> itemCustomClass = Class.forName("siz.addon.modularprops.common.custom.ItemCustom");
                if (itemCustomClass.isInstance(mainHand.getItem())) {

                    handleCustomItemDrop(player, mainHand);
                }
            } catch (ClassNotFoundException e) {

            }
        }
    }
    

    private static void handleCustomItemDrop(EntityPlayerSP player, ItemStack stack) {
        try {
            Class<?> handlerClass = Class.forName("siz.addon.modularprops.common.handler.CustomItemEventHandler");
            java.lang.reflect.Method method = handlerClass.getDeclaredMethod("handleDropProtection", 
                java.util.UUID.class, ItemStack.class);
            method.setAccessible(true);
            method.invoke(null, player.getUniqueID(), stack);
        } catch (Exception e) {

        }
    }
}

