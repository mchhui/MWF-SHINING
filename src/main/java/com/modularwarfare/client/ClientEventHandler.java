package com.modularwarfare.client;

import org.lwjgl.input.Keyboard;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.AnimationUtils;
import com.modularwarfare.client.handler.KeyInputHandler;
import com.modularwarfare.client.hud.FlashSystem;
import com.modularwarfare.client.model.InstantBulletRenderer;
import com.modularwarfare.common.backpacks.BackpackType;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;
import com.modularwarfare.common.hitbox.playerdata.PlayerDataHandler;
import com.modularwarfare.common.init.ModSounds;
import com.modularwarfare.common.network.PacketBackpackElytraStart;
import com.modularwarfare.common.network.PacketBackpackJet;
import com.modularwarfare.common.network.PacketOpenGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClientEventHandler {

    public static float cemeraBobbing=0f;
    public boolean lastJump;
    public static boolean isJetFly=false;
    public static float jetPower=0;
    public static int jetFireTime=0;
    public static int jetCoolTime=0;
    
    @SubscribeEvent
    public void onInput(InputUpdateEvent event) {
        EntityPlayerSP player=Minecraft.getMinecraft().player;
        if (event.getMovementInput().jump && !lastJump && !player.onGround && player.motionY < 0.0D && !player.isElytraFlying() && !player.capabilities.isFlying)
        {
            ModularWarfare.NETWORK.sendToServer(new PacketBackpackElytraStart());
        }
        boolean isElytra=false;
        boolean sneakToHover=false;
        if (player.hasCapability(CapabilityExtra.CAPABILITY, null)) {
            final IExtraItemHandler extraSlots = player.getCapability(CapabilityExtra.CAPABILITY, null);
            final ItemStack itemstackBackpack = extraSlots.getStackInSlot(0);

            if (!itemstackBackpack.isEmpty()) {
                if (itemstackBackpack.getItem() instanceof ItemBackpack) {
                    BackpackType backpack= ((ItemBackpack)itemstackBackpack.getItem()).type;
                    if (backpack.isElytra) {
                        isElytra=true;
                    }
                    if(backpack.jetSneakHover) {
                        sneakToHover=true;
                    }
                }
            }
        }
        if (event.getMovementInput().jump) {
            if (!lastJump && !player.onGround) {
                isJetFly = true;
            }
            if(isElytra) {
                isJetFly=false;
            }
        }else if(sneakToHover&&event.getMovementInput().sneak){
            if (!lastJump && !player.onGround) {
                isJetFly = true;
            }
        }else {
            isJetFly=false;
        }
        lastJump=event.getMovementInput().jump;
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void cemeraSetup(CameraSetup event) {
        GlStateManager.rotate((float) cemeraBobbing * 5, 0, 0, 1);
    }
    
    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event) {
        InstantBulletRenderer.RenderAllTrails(event.getPartialTicks());
    }
    
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        if(event.phase==Phase.START&&event.side==Side.CLIENT) {
            if(event.player==Minecraft.getMinecraft().player) {
                boolean isJetFly=false;
                BackpackType backpack=null;
                if (event.player.hasCapability(CapabilityExtra.CAPABILITY, null)) {
                    final IExtraItemHandler extraSlots = event.player.getCapability(CapabilityExtra.CAPABILITY, null);
                    final ItemStack itemstackBackpack = extraSlots.getStackInSlot(0);

                    if (!itemstackBackpack.isEmpty()) {
                        if (itemstackBackpack.getItem() instanceof ItemBackpack) {
                            backpack= ((ItemBackpack)itemstackBackpack.getItem()).type;
                            backpack.isJet=true;
                            backpack.isElytra=true;
                            if (backpack.isJet) {
                                isJetFly=this.isJetFly||Keyboard.isKeyDown(KeyInputHandler.jetpackFire.getKeyCode());
                            }
                        }
                    }
                }
                jetCoolTime--;
                if(jetCoolTime<0) {
                    jetCoolTime=0;
                }
                if (isJetFly && event.player.isElytraFlying()&&!event.player.isSneaking()) {
                    double speed = event.player.motionX * event.player.motionX
                        + event.player.motionY * event.player.motionY + event.player.motionZ * event.player.motionZ;
                    speed = Math.sqrt(speed);
                    if (jetFireTime == 0 && jetCoolTime == 0) {
                        jetFireTime = 1;
                        ModularWarfare.NETWORK.sendToServer(new PacketBackpackJet(true));
                    } else if(jetCoolTime==0){
                        jetFireTime++;
                        if (jetFireTime > backpack.jetElytraBoostDuration) {
                            jetCoolTime = backpack.jetElytraBoostCoolTime;
                            jetFireTime=0;
                        }
                        Vec3d vec = event.player.getLookVec();
                        vec = vec.scale(Math.max(speed, backpack.jetElytraBoost));
                        event.player.motionX = vec.x;
                        event.player.motionY = vec.y;
                        event.player.motionZ = vec.z;
                        AnimationUtils.isJet.put(event.player.getName(), System.currentTimeMillis()+100);
                    }
                }
                if(isJetFly&&!event.player.isElytraFlying()) {
                    jetPower+=backpack.jetWorkForce;
                    if(jetPower>backpack.jetMaxForce) {
                        jetPower=backpack.jetMaxForce;
                    }
                    if(event.player.isSneaking()) {
                        jetPower=0;
                    }
                    if(event.player.motionY<jetPower) {
                        event.player.motionY=jetPower;
                    }
                    ModularWarfare.NETWORK.sendToServer(new PacketBackpackJet());
                    AnimationUtils.isJet.put(event.player.getName(), System.currentTimeMillis()+100);
                }else {
                    jetPower=backpack.jetIdleForce;
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiLaunch(GuiOpenEvent event) {
        if (ModConfig.INSTANCE.general.customInventory) {
            if (event.getGui() != null && event.getGui().getClass() == GuiInventory.class) {
                final EntityPlayer player = Minecraft.getMinecraft().player;
                if (!player.isCreative()) {
                    event.setCanceled(true);
                    ModularWarfare.NETWORK.sendToServer(new PacketOpenGui(0));
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if(player == null) return;
        PlayerDataHandler.clientSideData.clear();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (ModConfig.INSTANCE.walks_sounds.walk_sounds && !Loader.isModLoaded("dsurround")) {
            float soundVolume = ModConfig.INSTANCE.walks_sounds.volume;
            Minecraft mc = Minecraft.getMinecraft();
            final ResourceLocation currentSound = event.getSound().getSoundLocation();

            if (FlashSystem.flashValue > 0 && !event.getSound().getSoundLocation().toString().contains("stun") && !event.getSound().getSoundLocation().toString().contains("flashed")) {
                event.setResultSound(new PositionedSoundRecord(currentSound, event.getSound().getCategory(), 0.05f, 1.0f - ((float) FlashSystem.flashValue / 50), false, 0, ISound.AttenuationType.LINEAR, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()) {{
                }});
            }

            if (currentSound.toString().equals("minecraft:entity.generic.explode")) {
                event.setResultSound(null);
            }
            if (currentSound.toString().equals("minecraft:block.grass.step")) {
                if (mc.player.isSprinting()) {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_GRASS_SPRINT, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                } else {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_GRASS_WALK, SoundCategory.BLOCKS,  (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                }
            } else if (currentSound.toString().equals("minecraft:block.stone.step")) {
                if (mc.player.isSprinting()) {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_STONE_SPRINT, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                } else {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_STONE_WALK, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                }
            } else if (currentSound.toString().equals("minecraft:block.gravel.step")) {
                if (mc.player.isSprinting()) {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_GRAVEL_SPRINT, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                } else {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_GRAVEL_WALK, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                }
            } else if (currentSound.toString().equals("minecraft:block.metal.step")) {
                if (mc.player.isSprinting()) {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_METAL_SPRINT, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                } else {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_METAL_WALK, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                }
            } else if (currentSound.toString().equals("minecraft:block.wood.step")) {
                if (mc.player.isSprinting()) {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_WOOD_SPRINT, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                } else {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_WOOD_WALK, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                }
            } else if (currentSound.toString().equals("minecraft:block.sand.step")) {
                if (mc.player.isSprinting()) {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_SAND_SPRINT, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                } else {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_SAND_WALK, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                }
            } else if (currentSound.toString().equals("minecraft:block.snow.step")) {
                if (mc.player.isSprinting()) {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_SNOW_SPRINT, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                } else {
                    event.setResultSound(new PositionedSoundRecord(ModSounds.STEP_SNOW_WALK, SoundCategory.BLOCKS, (FlashSystem.flashValue > 0) ? soundVolume * 0.05f : soundVolume, (FlashSystem.flashValue > 0) ? 1.0f - ((float) FlashSystem.flashValue / 50) : 1.0f, event.getSound().getXPosF(), event.getSound().getYPosF(), event.getSound().getZPosF()));
                }
            }
        }
    }

}

