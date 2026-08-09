package com.modularwarfare.client.handler;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.HandleKeyEvent;
import com.modularwarfare.client.ClientEventHandler;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.animations.AnimStateMachine;
import com.modularwarfare.client.fpp.basic.renderers.RenderGunStatic;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.gui.GuiGunModify;
import com.modularwarfare.client.gui.hud.GunTransformHUD;
import com.modularwarfare.client.input.KeyEntry;
import com.modularwarfare.client.input.KeyType;
import com.modularwarfare.client.laser.LaserRenderManager;
import com.modularwarfare.client.flashlight.FlashlightRenderManager;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.client.model.ModelCustomArmor;
import com.modularwarfare.common.armor.ArmorType;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.melee.ItemMelee;
import com.modularwarfare.common.network.PacketGunReload;
import com.modularwarfare.common.network.PacketGunSwitchMode;
import com.modularwarfare.common.network.PacketGunUnloadAttachment;
import com.modularwarfare.common.network.PacketLaserToggle;
import com.modularwarfare.common.network.PacketFlashlightToggle;
import com.modularwarfare.utility.MWSound;
import com.modularwarfare.utility.script.ScriptHost;
import com.teamderpy.shouldersurfing.client.ShoulderInstance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.UUID;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID, value = Side.CLIENT)
public final class KeyInputHandler {

    private static final ArrayList<KeyEntry> keyBinds = new ArrayList<>();
    public static KeyBinding jetpackFire;

    public static void registerKeys() {
        keyBinds.clear();

        keyBinds.add(new KeyEntry(KeyType.GunReload));
        keyBinds.add(new KeyEntry(KeyType.ClientReload));
        keyBinds.add(new KeyEntry(KeyType.FireMode));
        keyBinds.add(new KeyEntry(KeyType.Inspect));
        keyBinds.add(new KeyEntry(KeyType.GunUnload));
        keyBinds.add(new KeyEntry(KeyType.AddAttachment));
        keyBinds.add(new KeyEntry(KeyType.Flashlight));
        keyBinds.add(new KeyEntry(KeyType.LaserToggle));
        keyBinds.add(new KeyEntry(KeyType.GunTransform));

        keyBinds.add(new KeyEntry(KeyType.QuickViewToggle));

        if (ModularWarfare.DEV_ENV) {
            keyBinds.add(new KeyEntry(KeyType.DebugMode));
        }

        for (KeyEntry keyEntry : keyBinds) {
            ClientRegistry.registerKeyBinding(keyEntry.keyBinding);
        }
        jetpackFire = new KeyBinding(KeyType.Jetpack.displayName, KeyType.Jetpack.keyCode, "ModularWarfare");
        ClientRegistry.registerKeyBinding(jetpackFire);
    }

    @SubscribeEvent
    static void onKeyInput(InputEvent.KeyInputEvent event) {
        for (KeyEntry keyEntry : keyBinds) {
            if (keyEntry.keyBinding.isKeyDown()) {
                handleKeyInput(keyEntry.keyType);
                break;
            } else if(keyEntry.keyType == KeyType.GunTransform && !keyEntry.keyBinding.isKeyDown() && GunTransformHUD.isVisible()) {
                GunTransformHUD.onKeyReleased();
            }
        }
    }

    private static void handleKeyInput(KeyType keyType) {
        if (Minecraft.getMinecraft().player != null) {
            EntityPlayerSP entityPlayer = Minecraft.getMinecraft().player;
            HandleKeyEvent event = new HandleKeyEvent(keyType);
            MinecraftForge.EVENT_BUS.post(event);

            switch (keyType) {
                // F9 Reloads Models /// SHIFT + F9 Reloads Textures & Icons
                case ClientReload:

                    ModularWarfare.loadConfig();
                    ScriptHost.INSTANCE.reset();

                    if(ClientProxy.gunEnhancedRenderer != null) {
                        ClientProxy.gunEnhancedRenderer.resetModels();
                    }
                    if(ClientProxy.grenadeEnhancedRenderer != null) {
                        ClientProxy.grenadeEnhancedRenderer.resetModels();
                    }
                    com.modularwarfare.client.fpp.enhanced.renderers.RenderMelee.controller = null;

                    ItemStack mainHand = entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                    if (mainHand.getItem() instanceof ItemGun) {
                        final GunType gunType = ((ItemGun)mainHand.getItem()).type;
                        for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                            ItemStack itemStack = GunType.getAttachment(mainHand, attachment);
                            if (itemStack != null && itemStack.getItem() != Items.AIR
                                    && itemStack.getItem() instanceof ItemAttachment) {
                                AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                                if (attachmentType != null && attachmentType.hasModel()) {
                                    attachmentType.reloadModel();
                                    forceReloadAttachmentGltf(attachmentType);
                                }
                            }
                        }
                        reloadTypeModel(gunType);
                    } else if (mainHand.getItem() instanceof ItemGrenade) {
                        reloadTypeModel(((ItemGrenade)mainHand.getItem()).type);
                    } else if (mainHand.getItem() instanceof ItemMelee) {
                        reloadTypeModel(((ItemMelee)mainHand.getItem()).type);
                    }

                    reloadWornArmorModels(entityPlayer);

                    if (entityPlayer.isSneaking()) {
                        ModularWarfare.PROXY.reloadModels(true);
                    }
                    break;
                case FireMode:
                    if(!entityPlayer.isSpectator()) {
                        if (entityPlayer.getHeldItemMainhand() != null && entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun) {
                            ItemGun itemGun = (ItemGun) entityPlayer.getHeldItemMainhand().getItem();
                            GunType gunType = itemGun.type;
                            PacketGunSwitchMode.switchClient(entityPlayer);
                            ModularWarfare.NETWORK.sendToServer(new PacketGunSwitchMode());
                            ModularWarfare.PROXY.onModeChangeAnimation(entityPlayer, gunType.internalName);
                        }
                    }
                    break;
                case Inspect:
                    if(!entityPlayer.isSpectator()) {
                        if (entityPlayer.getHeldItemMainhand() != null) {
                            if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun) {
                                if(AnimationController.getController(entityPlayer, null)!=null) {
                                    AnimationController.getController(entityPlayer, null).INSPECT=0;
                                }
                            } else if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGrenade) {
                                ItemGrenade itemGrenade = (ItemGrenade) entityPlayer.getHeldItemMainhand().getItem();
                                if (itemGrenade.type.animationType == WeaponAnimationType.ENHANCED) {
                                    // 检查是否可以进行视检
                                    if (!GrenadeEnhancedHandler.isHolding && 
                                        !GrenadeEnhancedHandler.isTimerStarted(entityPlayer.getHeldItemMainhand()) && 
                                        !ClientRenderHooks.getEnhancedAnimMachine(entityPlayer).throwing) {
                                        if (AnimationController.getController(entityPlayer, null) != null) {
                                            AnimationController.getController(entityPlayer, null).INSPECT = 0;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                case GunReload:
                    ItemStack reloadStack = entityPlayer.getHeldItemMainhand();
                    if (reloadStack != null && (reloadStack.getItem() instanceof ItemGun || reloadStack.getItem() instanceof ItemAmmo)) {
                        if (AnimationController.getController(entityPlayer, null) == null
                                || AnimationController.getController(entityPlayer, null).isCouldReload()) {
                            ModularWarfare.NETWORK.sendToServer(new PacketGunReload());
                        }
                    }
                    break;

                case GunUnload:
                    ItemStack unloadStack = entityPlayer.getHeldItemMainhand();
                    if (ClientRenderHooks.getAnimMachine(entityPlayer).attachmentMode) {
                        ModularWarfare.NETWORK.sendToServer(new PacketGunUnloadAttachment(ClientProxy.attachmentUI.selectedAttachEnum.getName(), false));
                    } else {
                        if (unloadStack != null && (unloadStack.getItem() instanceof ItemGun || unloadStack.getItem() instanceof ItemAmmo)) {
                            if (AnimationController.getController(entityPlayer, null) == null
                                || AnimationController.getController(entityPlayer, null).isCouldReload()) {
                                ModularWarfare.NETWORK.sendToServer(new PacketGunReload(true));
                            }
                        }
                    }
                    break;

                case DebugMode:
                    if (entityPlayer.isSneaking()) {
                        ModularWarfare.loadContentPacks(true);
                        //ModularWarfare.PROXY.reloadModels(true);
                    }
                    break;

                case AddAttachment:
                    if(!entityPlayer.isSpectator() && ClientEventHandler.serverAllowGunModifyGui) {
                        if (entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND) != null && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
                            if (entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem() instanceof ItemGun) {
                                if (((ItemGun)entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem()).type.animationType == WeaponAnimationType.BASIC) {
                                    AnimStateMachine stateMachine = ClientRenderHooks.getAnimMachine(entityPlayer);
                                    ModularWarfare.PROXY.playSound(new MWSound(entityPlayer.getPosition(), "attachment.open", 1f, 1f));
                                    Minecraft.getMinecraft().displayGuiScreen(new GuiGunModify());
                                } else if (((ItemGun)entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem()).type.animationType == WeaponAnimationType.ENHANCED) {
                                    if ((ClientRenderHooks.currentGun != -1
                                    && ClientRenderHooks.wannaSlot == -1
                                    && AnimationController.getClientController() != null
                                    && (AnimationController.getClientController().getPlayingAnimation() == AnimationType.DEFAULT
                                    || AnimationController.getClientController().getPlayingAnimation() == AnimationType.DEFAULT_EMPTY))) {
                                        ModularWarfare.PROXY.playSound(new MWSound(entityPlayer.getPosition(), "attachment.open", 1f, 1f));
                                        Minecraft.getMinecraft().displayGuiScreen(new GuiGunModify());
                                        }
                                    }
                                }
                            }
                        }
                    break;

                case Flashlight:
                    if(!entityPlayer.isSpectator()) {
                        if (entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem() instanceof ItemGun) {
                            final ItemStack gunStack = entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                            if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Flashlight) != null) {
                                final ItemAttachment itemAttachment = (ItemAttachment) GunType.getAttachment(gunStack, AttachmentPresetEnum.Flashlight).getItem();
                                if (itemAttachment != null) {
                                    boolean flashlightEnabled = !((ItemGun) gunStack.getItem()).getFlashlightEnabled(gunStack);
                                    ((ItemGun) gunStack.getItem()).setFlashlightEnabled(gunStack, flashlightEnabled);
                                    FlashlightRenderManager.getInstance().setFlashlightState(entityPlayer.getUniqueID(), flashlightEnabled);
                                    RenderGunStatic.isLightOn = flashlightEnabled;
                                    ModularWarfare.NETWORK.sendToServer(new PacketFlashlightToggle(flashlightEnabled));
                                    ModularWarfare.PROXY.playSound(new MWSound(entityPlayer.getPosition(), "attachment.apply", 1f, 1f));
                                }
                            }
                        }
                    }
                    break;

                case LaserToggle:
                    if(!entityPlayer.isSpectator()) {
                        if (entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem() instanceof ItemGun) {
                            final ItemStack gunStack = entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                            if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Laser) != null) {
                                final ItemAttachment itemAttachment = (ItemAttachment) GunType.getAttachment(gunStack, AttachmentPresetEnum.Laser).getItem();
                                if (itemAttachment != null) {
                                    boolean laserEnabled = !((ItemGun) gunStack.getItem()).getLaserEnabled(gunStack);
                                    ((ItemGun) gunStack.getItem()).setLaserEnabled(gunStack, laserEnabled);
                                    LaserRenderManager.getInstance().toggleLaserState(entityPlayer.getUniqueID());
                                    ModularWarfare.NETWORK.sendToServer(new PacketLaserToggle(laserEnabled));
                                    ModularWarfare.PROXY.playSound(new MWSound(entityPlayer.getPosition(), "attachment.apply", 1f, 1f));
                                }
                            }
                        }
                    }
                    break;

                case QuickViewToggle:
                    if(!entityPlayer.isSpectator()) {
                        if(ClientProxy.shoulderSurfingLoaded) {
                            if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
                                Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
                                ShoulderInstance.getInstance().setShoulderSurfing(false);
                            } else {
                                Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
                                ShoulderInstance.getInstance().setShoulderSurfing(true);
                            }
                        } else {
                            if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
                                Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
                            } else {
                                Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
                            }
                        }
                        Minecraft.getMinecraft().renderGlobal.setDisplayListEntitiesDirty();
                    }
                    break;

                case GunTransform:
                    handleGunTransform(entityPlayer);
                    break;

                    // Deprecated
//                case Backpack:
//                    if (!ModConfig.INSTANCE.general.customInventory) {
//                        if (!entityPlayer.isCreative()) {
//                            ModularWarfare.NETWORK.sendToServer(new PacketOpenGui(0));
//                        }
//                    }
//                    break;

                default:
                    ModularWarfare.LOGGER.warn("Default case called on handleKeyInput for " + keyType.toString());
                    break;
            }
        }
    }

    private static void reloadTypeModel(com.modularwarfare.common.type.BaseType type) {
        if (type == null) {
            return;
        }
        type.reloadModel();
        if (type.enhancedModel != null) {
            type.enhancedModel.forceReload();
        }
    }

    private static void forceReloadAttachmentGltf(AttachmentType attachmentType) {
        if (attachmentType == null || !(attachmentType.model instanceof ModelAttachment)) {
            return;
        }
        EnhancedModel gltf = ((ModelAttachment) attachmentType.model).enhancedModel;
        if (gltf != null) {
            gltf.forceReload();
        }
    }

    private static void reloadWornArmorModels(EntityPlayerSP player) {
        EntityEquipmentSlot[] armorSlots = new EntityEquipmentSlot[] {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET
        };
        for (EntityEquipmentSlot slot : armorSlots) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemMWArmor) {
                reloadArmorType(((ItemMWArmor) stack.getItem()).type);
            }
        }
        if (player.hasCapability(CapabilityExtra.CAPABILITY, null)) {
            IExtraItemHandler extra = player.getCapability(CapabilityExtra.CAPABILITY, null);
            if (extra != null) {
                for (int i = 0; i < extra.getSlots(); i++) {
                    ItemStack stack = extra.getStackInSlot(i);
                    if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemSpecialArmor) {
                        reloadArmorType(((ItemSpecialArmor) stack.getItem()).type);
                    }
                }
            }
        }
    }

    private static void reloadArmorType(ArmorType armorType) {
        if (armorType == null) {
            return;
        }
        if (armorType.bipedModel instanceof ModelCustomArmor) {
            EnhancedModel oldArm = ((ModelCustomArmor) armorType.bipedModel).enhancedArmModel;
            if (oldArm != null) {
                mchhui.hegltf.GltfModelManager.get().forceUnload(oldArm.getModelLocation());
            }
        }
        armorType.reloadModel();
        if (armorType.bipedModel instanceof ModelCustomArmor) {
            EnhancedModel arm = ((ModelCustomArmor) armorType.bipedModel).enhancedArmModel;
            if (arm != null) {
                arm.forceReload();
            }
        }
    }

    private static void handleGunTransform(EntityPlayerSP entityPlayer) {
        if(entityPlayer == null || entityPlayer.isSpectator()) return;

        ItemStack heldItem = entityPlayer.getHeldItemMainhand();
        if(heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemGun)) return;

        ItemGun itemGun = (ItemGun) heldItem.getItem();
        GunType gunType = itemGun.type;

        if(gunType.transformations != null && !gunType.transformations.isEmpty()) {
            if(entityPlayer.isSneaking()) {
                ItemGun.switchToLastTransformState(heldItem);
                GunTransformManager.transformGun(entityPlayer, gunType.transformations.get(ItemGun.getTransformState(heldItem)),UUID.randomUUID());
            } else if(!GunTransformHUD.isVisible()) {
                GunTransformHUD.setVisible(true);
            }
        }
    }

    private KeyInputHandler() {
    }
}