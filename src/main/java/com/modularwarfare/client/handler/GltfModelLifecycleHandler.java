package com.modularwarfare.client.handler;

import java.util.HashSet;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.client.model.ModelCustomArmor;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.common.guns.AttachmentType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAttachment;
import com.modularwarfare.common.guns.ItemGun;
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
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID, value = Side.CLIENT)
public class GltfModelLifecycleHandler {

    private static final HashSet<ResourceLocation> pinnedThisTick = new HashSet<>();
    private static final HashSet<ResourceLocation> pinnedLastTick = new HashSet<>();
    private static int logTicker;

    private static Class<?> modularPropsItemCustomClass;
    private static Class<?> modularPropsItemBlockCustomClass;
    private static Field modularPropsItemCustomTypeField;
    private static Field modularPropsItemBlockCustomTypeField;
    private static boolean modularPropsClassesResolved;

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
        considerWornArmorArms(player, GltfLoadPriority.HIGH, true);

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
        if (model != null) {
            model.ensureRequested(priority);
            if (hardPin) {
                pinnedThisTick.add(model.getModelLocation());
            }
        }
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemGun) {
            for (AttachmentPresetEnum slot : AttachmentPresetEnum.values()) {
                ItemStack att = GunType.getAttachment(stack, slot);
                if (att == null || !(att.getItem() instanceof ItemAttachment)) {
                    continue;
                }
                AttachmentType attType = ((ItemAttachment) att.getItem()).type;
                if (attType == null || !(attType.model instanceof ModelAttachment)) {
                    continue;
                }
                ModelAttachment attModel = (ModelAttachment) attType.model;
                if (!attModel.isGltf() || attModel.enhancedModel == null) {
                    continue;
                }
                attModel.enhancedModel.ensureRequested(priority);
                if (hardPin) {
                    pinnedThisTick.add(attModel.enhancedModel.getModelLocation());
                }
            }
        }
    }

    private static void considerWornArmorArms(EntityPlayer player, GltfLoadPriority priority, boolean hardPin) {
        EntityEquipmentSlot[] armorSlots = new EntityEquipmentSlot[] {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET
        };
        for (EntityEquipmentSlot slot : armorSlots) {
            considerArmorArm(player.getItemStackFromSlot(slot), priority, hardPin);
        }
        if (player.hasCapability(CapabilityExtra.CAPABILITY, null)) {
            IExtraItemHandler extra = player.getCapability(CapabilityExtra.CAPABILITY, null);
            if (extra != null) {
                for (int i = 0; i < extra.getSlots(); i++) {
                    considerArmorArm(extra.getStackInSlot(i), priority, hardPin);
                }
            }
        }
    }

    private static void considerArmorArm(ItemStack stack, GltfLoadPriority priority, boolean hardPin) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Object biped = null;
        if (stack.getItem() instanceof ItemMWArmor) {
            biped = ((ItemMWArmor) stack.getItem()).type != null
                ? ((ItemMWArmor) stack.getItem()).type.bipedModel : null;
        } else if (stack.getItem() instanceof ItemSpecialArmor) {
            biped = ((ItemSpecialArmor) stack.getItem()).type != null
                ? ((ItemSpecialArmor) stack.getItem()).type.bipedModel : null;
        }
        if (!(biped instanceof ModelCustomArmor)) {
            return;
        }
        EnhancedModel arm = ((ModelCustomArmor) biped).enhancedArmModel;
        if (arm == null) {
            return;
        }
        arm.ensureRequested(priority);
        if (hardPin) {
            pinnedThisTick.add(arm.getModelLocation());
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
        } else if (stack.getItem() instanceof ItemAttachment) {
            AttachmentType type = ((ItemAttachment) stack.getItem()).type;
            if (type != null && type.model instanceof ModelAttachment) {
                ModelAttachment attModel = (ModelAttachment) type.model;
                if (attModel.isGltf()) {
                    return attModel.enhancedModel;
                }
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
        } else {
            return modelFromModularProps(stack.getItem());
        }
        return null;
    }

    private static void resolveModularPropsClasses() {
        if (modularPropsClassesResolved) {
            return;
        }
        modularPropsClassesResolved = true;
        try {
            modularPropsItemCustomClass = Class.forName("siz.addon.modularprops.common.custom.ItemCustom");
            modularPropsItemCustomTypeField = modularPropsItemCustomClass.getField("type");
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            modularPropsItemBlockCustomClass = Class.forName("siz.addon.modularprops.common.custom.ItemBlockCustom");
            modularPropsItemBlockCustomTypeField = modularPropsItemBlockCustomClass.getField("type");
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static EnhancedModel modelFromModularProps(Item item) {
        if (item == null || !ClientProxy.modularPropsLoaded) {
            return null;
        }
        resolveModularPropsClasses();
        try {
            Object typeObj = null;
            if (modularPropsItemCustomClass != null && modularPropsItemCustomTypeField != null
                && modularPropsItemCustomClass.isInstance(item)) {
                typeObj = modularPropsItemCustomTypeField.get(item);
            } else if (modularPropsItemBlockCustomClass != null && modularPropsItemBlockCustomTypeField != null
                && modularPropsItemBlockCustomClass.isInstance(item)) {
                typeObj = modularPropsItemBlockCustomTypeField.get(item);
            }
            if (typeObj instanceof BaseType) {
                return ((BaseType) typeObj).enhancedModel;
            }
        } catch (ReflectiveOperationException ignored) {
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

    @SubscribeEvent
    public static void onResourceReload(net.minecraftforge.client.event.TextureStitchEvent.Post event) {
        if (event.getMap() != Minecraft.getMinecraft().getTextureMapBlocks()) {
            return;
        }
        GltfModelManager.get().clearFailedLoads();
        GltfModelManager.devLog("[GltfLazy] Cleared FAILED GLTF loads on resource reload");
    }
}
