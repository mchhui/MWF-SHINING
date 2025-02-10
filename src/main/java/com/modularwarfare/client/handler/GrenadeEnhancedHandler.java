package com.modularwarfare.client.handler;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGrenade;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.client.input.KeyType;
import com.modularwarfare.common.guns.WeaponSoundType;
import com.modularwarfare.common.network.PacketGrenadeThrow;
import com.modularwarfare.common.network.PacketGrenadeConsume;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID, value = Side.CLIENT)
public class GrenadeEnhancedHandler {
    public static boolean isHolding = false;
    public static boolean isThrowLow = false;
    public static boolean useKeyLock = false;
    public static long startHoldTime;
    public static ItemStack holdingStack;
    private static boolean rKeyLock = false;
    public static boolean isConsumed = false;
    public static int lastSlot = -1;

    private static Field pressedField;

    static {
        try {
            pressedField = KeyBinding.class.getDeclaredField("pressed");
            pressedField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void resetKeyBinding(KeyBinding keyBinding) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), false);
    }

    private static boolean isTimerStarted(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("timerStarted")) {
            return stack.getTagCompound().getBoolean("timerStarted");
        }
        return false;
    }

    private static void setTimerStarted(ItemStack stack, boolean started) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setBoolean("timerStarted", started);
    }

    private static long getTimerStartTime(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("timerStartTime")) {
            return stack.getTagCompound().getLong("timerStartTime");
        }
        return 0;
    }

    private static void setTimerStartTime(ItemStack stack, long time) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setLong("timerStartTime", time);
    }

    @SubscribeEvent
    public static void onMouseInput(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }

        ItemStack stack = mc.player.getHeldItemMainhand();
        if (stack.getItem() instanceof ItemGrenade) {
            GrenadeType type = ((ItemGrenade) stack.getItem()).type;
            if (type.animationType == WeaponAnimationType.ENHANCED) {
                if (event.getButton() == 0) {
                    GameSettings gs = mc.gameSettings;
                    resetKeyBinding(gs.keyBindAttack);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTick(RenderTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }

        AnimationType controllerState = AnimationController.getClientController().getPlayingAnimation();
        boolean isDraw = controllerState == AnimationType.DRAW;

        if (isDraw) {
            return;
        }
        
        int currentSlot = mc.player.inventory.currentItem;
        if (lastSlot != currentSlot) {
            if (holdingStack != null && holdingStack.getItem() instanceof ItemGrenade) {
                ItemStack lastSlotStack = mc.player.inventory.getStackInSlot(lastSlot);
                if (!lastSlotStack.isEmpty() && lastSlotStack.getItem() instanceof ItemGrenade
                        && isTimerStarted(lastSlotStack)) {
                    GrenadeType type = ((ItemGrenade) lastSlotStack.getItem()).type;
                    float remainingTime = type.fuseTime
                            - (System.currentTimeMillis() - getTimerStartTime(lastSlotStack)) / 1000f;
                    ModularWarfare.NETWORK.sendToServer(new PacketGrenadeThrow(false, remainingTime, 0));
                    setTimerStarted(lastSlotStack, false);
                    ModularWarfare.NETWORK.sendToServer(new PacketGrenadeConsume());
                }
                if (!lastSlotStack.isEmpty() && lastSlotStack.getItem() instanceof ItemGrenade
                        && isConsumed) {
                    ModularWarfare.NETWORK.sendToServer(new PacketGrenadeConsume());
                }
                resetGrenadeState();
            }
            lastSlot = currentSlot;
        }
        
        ItemStack stack = mc.player.getHeldItemMainhand();
        if (!isValidGrenadeStack(stack)) {
            resetGrenadeState();
            return;
        }

        GrenadeType type = ((ItemGrenade) stack.getItem()).type;
        if (type == null || type.animationType != WeaponAnimationType.ENHANCED) {
            return;
        }

        EnhancedStateMachine machine = ClientRenderHooks.getEnhancedAnimMachine(mc.player);
        if (machine == null) {
            return;
        }

        updateKeyStates(mc);

        handleThrowingState(mc, stack, type, machine);
        handleGrenadeTimer(mc, stack, type);
    }

    private static boolean isValidGrenadeStack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemGrenade;
    }

    private static void resetGrenadeState() {
        isHolding = false;
        isConsumed = false;
        holdingStack = null;
    }

    private static void updateKeyStates(Minecraft mc) {
        if (!Mouse.isButtonDown(mc.gameSettings.keyBindUseItem.getKeyCode() + 100)) {
            useKeyLock = false;
        }

        if (!Keyboard.isKeyDown(KeyType.GunReload.keyCode)) {
            rKeyLock = false;
        }

        if (!useKeyLock && Mouse.isButtonDown(mc.gameSettings.keyBindUseItem.getKeyCode() + 100)) {
            useKeyLock = true;
            isThrowLow = !isThrowLow;
        }
    }

    private static void handleThrowingState(Minecraft mc, ItemStack stack, GrenadeType type, EnhancedStateMachine machine) {
        boolean isThrowingTemp = Mouse.isButtonDown(mc.gameSettings.keyBindAttack.getKeyCode() + 100);

        if (isThrowingTemp && !isHolding) {
            if (!machine.throwing) {
                setTimerStarted(stack, false);
                isConsumed = false;
                if (type.enhancedModel != null) {
                    machine.triggerThrow(AnimationController.getClientController(), mc.player,
                            (ModelEnhancedGrenade) type.enhancedModel);
                }
                startHoldTime = System.currentTimeMillis();
                isHolding = true;
                holdingStack = stack.copy();
                type.playClientSound(mc.player, WeaponSoundType.GrenadePreThrow);
            }
        } else if (!isThrowingTemp && isHolding) {
            if (!machine.throwing) return;

            if (machine.throwingPhase == EnhancedStateMachine.Phase.FIRST && 
                machine.controller != null && 
                machine.controller.GRENADE_THROW >= 1.0 && 
                !isConsumed) {
                handleFirstPhase(mc, stack, type, machine);
            }
        }
        handleThrowProcess(mc, stack, type, machine);
        handleTimerStart(mc, stack, type, machine);
    }

    private static void handleFirstPhase(Minecraft mc, ItemStack stack, GrenadeType type, EnhancedStateMachine machine) {
        if (!isValidGrenadeStack(stack)) {
            resetGrenadeState();
            machine.throwing = false;
            return;
        }

        float remainingTime = isTimerStarted(stack)
                ? type.fuseTime - (System.currentTimeMillis() - getTimerStartTime(stack)) / 1000f
                : -1;
        ModularWarfare.NETWORK.sendToServer(new PacketGrenadeThrow(isThrowLow, remainingTime, 1.0f));

        isConsumed = true;
        isHolding = false;
        setTimerStarted(stack, false);

        type.playClientSound(mc.player, WeaponSoundType.GrenadeThrowFirst);

        machine.throwingPhase = EnhancedStateMachine.Phase.SECOND;
        if (machine.controller != null) {
            machine.controller.GRENADE_THROW = 0;
        }
    }

    private static void handleThrowProcess(Minecraft mc, ItemStack stack, GrenadeType type, EnhancedStateMachine machine) {
        if (machine.throwingPhase == EnhancedStateMachine.Phase.SECOND && 
                machine.controller != null && 
                machine.controller.GRENADE_THROW == 0) {
            type.playClientSound(mc.player, WeaponSoundType.GrenadeThrowSecond);
            machine.continueThrow(AnimationController.getClientController(), mc.player, (ModelEnhancedGrenade) type.enhancedModel);
        } else if (machine.throwingPhase == EnhancedStateMachine.Phase.POST && 
                machine.controller != null &&
                isConsumed) {
            double throwProgress = machine.controller.GRENADE_THROW;
            
            if (throwProgress >= 0.9 || (throwProgress == 0 && machine.throwing)) {
                handlePostPhase(mc, stack, type, machine);
            }
        }
    }

    private static void handlePostPhase(Minecraft mc, ItemStack stack, GrenadeType type, EnhancedStateMachine machine) {
        type.playClientSound(mc.player, WeaponSoundType.GrenadePostThrow);
        if (isValidGrenadeStack(stack)) {
            ModularWarfare.NETWORK.sendToServer(new PacketGrenadeConsume());
            if (!mc.player.capabilities.isCreativeMode) {
                stack.shrink(1);
                if (stack.getCount() <= 0) {
                    mc.player.inventory.setInventorySlotContents(mc.player.inventory.currentItem, ItemStack.EMPTY);
                }
            }
        }
        resetGrenadeState();
        machine.throwing = false;
        machine.throwingPhase = EnhancedStateMachine.Phase.PRE;
    }

    private static void handleTimerStart(Minecraft mc, ItemStack stack, GrenadeType type, EnhancedStateMachine machine) {
        if (!isTimerStarted(stack) && !rKeyLock && Keyboard.isKeyDown(KeyType.GunReload.keyCode)) {
            if (machine.throwing && machine.throwingPhase != EnhancedStateMachine.Phase.PRE) {
                rKeyLock = true;
                setTimerStarted(stack, true);
                setTimerStartTime(stack, System.currentTimeMillis());
                type.playClientSound(mc.player, WeaponSoundType.GrenadeTimerStart);
            }
        }
    }

    private static void handleGrenadeTimer(Minecraft mc, ItemStack stack, GrenadeType type) {
        if (isTimerStarted(stack)) {
            float elapsedTime = (System.currentTimeMillis() - getTimerStartTime(stack)) / 1000f;
            if (elapsedTime >= type.fuseTime) {
                ModularWarfare.NETWORK.sendToServer(new PacketGrenadeThrow(false, 0.01f, 0));
                setTimerStarted(stack, false);
                ModularWarfare.NETWORK.sendToServer(new PacketGrenadeConsume());
                resetGrenadeState();
            }
        }
    }
}
