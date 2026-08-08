package com.modularwarfare.client.handler;

import java.util.HashSet;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.melee.ItemMelee;
import com.modularwarfare.common.melee.MeleeType;
import com.modularwarfare.common.type.BaseType;

import mchhui.hegltf.GltfCpuScheduler;
import mchhui.hegltf.GltfGpuUploadScheduler;
import mchhui.hegltf.GltfLoadPriority;
import mchhui.hegltf.GltfModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID, value = Side.CLIENT)
public class GltfModelLifecycleHandler {

    private static final HashSet<ResourceLocation> pinnedThisTick = new HashSet<>();
    private static final HashSet<ResourceLocation> pinnedLastTick = new HashSet<>();
    private static int logTicker;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (!GltfModelManager.isLazyEnabled()) {
            return;
        }

        pinnedThisTick.clear();
        EntityPlayer player = mc.player;

        considerHeld(player.getHeldItemMainhand(), GltfLoadPriority.HIGH, true);
        considerHeld(player.getHeldItemOffhand(), GltfLoadPriority.HIGH, true);

        if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.gltf != null && ModConfig.INSTANCE.gltf.prefetchHotbar) {
            int ttl = Math.max(1000, ModConfig.INSTANCE.gltf.hotbarSoftPinMs);
            int selected = player.inventory.currentItem;
            for (int i = 0; i < 9; i++) {
                if (i == selected) {
                    continue;
                }
                ItemStack stack = player.inventory.getStackInSlot(i);
                EnhancedModel model = modelFromStack(stack);
                if (model != null) {
                    ResourceLocation loc = model.getModelLocation();
                    model.ensureRequested(GltfLoadPriority.LOW);
                    GltfModelManager.get().softPin(loc, ttl);
                }
            }
        }

        for (ResourceLocation loc : pinnedThisTick) {
            if (!pinnedLastTick.contains(loc)) {
                GltfModelManager.get().pin(loc);
            }
        }
        for (ResourceLocation loc : pinnedLastTick) {
            if (!pinnedThisTick.contains(loc)) {
                GltfModelManager.get().unpin(loc);
            }
        }
        pinnedLastTick.clear();
        pinnedLastTick.addAll(pinnedThisTick);

        GltfModelManager.get().tickIdleUnload();

        if (GltfModelManager.isDevLog() && ++logTicker >= 200) {
            logTicker = 0;
            GltfModelManager.devLog(
                "[GltfLazy] cached={} ready={} cpuQ={} gpuQ={} frameTasks={} frameWeight={}",
                GltfModelManager.get().cachedCount(), GltfModelManager.get().readyCount(),
                GltfCpuScheduler.queuedCount(), GltfGpuUploadScheduler.queuedCount(),
                GltfGpuUploadScheduler.lastFrameTasks(), GltfGpuUploadScheduler.lastFrameWeight());
        }
    }

    private static void considerHeld(ItemStack stack, GltfLoadPriority priority, boolean hardPin) {
        EnhancedModel model = modelFromStack(stack);
        if (model == null) {
            return;
        }
        model.ensureRequested(priority);
        if (hardPin) {
            pinnedThisTick.add(model.getModelLocation());
        }
    }

    public static EnhancedModel modelFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof ItemGun) {
            GunType type = ((ItemGun) stack.getItem()).type;
            if (type != null && type.enhancedModel != null) {
                return type.enhancedModel;
            }
        } else if (stack.getItem() instanceof ItemGrenade) {
            GrenadeType type = ((ItemGrenade) stack.getItem()).type;
            if (type != null && type.enhancedModel != null) {
                return type.enhancedModel;
            }
        } else if (stack.getItem() instanceof ItemMelee) {
            MeleeType type = ((ItemMelee) stack.getItem()).type;
            if (type != null && type.enhancedModel != null) {
                return type.enhancedModel;
            }
        }
        return null;
    }

    public static void prefetch(BaseType type, GltfLoadPriority priority) {
        if (type == null || type.enhancedModel == null) {
            return;
        }
        type.enhancedModel.ensureRequested(priority != null ? priority : GltfLoadPriority.NORMAL);
    }

    @SubscribeEvent
    public static void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        pinnedThisTick.clear();
        pinnedLastTick.clear();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            mc.addScheduledTask(() -> {
                GltfModelManager.get().clearAll();
                GltfModelManager.devLog("[GltfLazy] Cleared GLTF cache on disconnect (client thread)");
            });
        } else {
            GltfModelManager.get().clearAllCpuOnly();
        }
    }
}
