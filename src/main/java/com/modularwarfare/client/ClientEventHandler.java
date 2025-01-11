package com.modularwarfare.client;

import com.modularwarfare.client.fpp.basic.configs.AttachmentRenderConfig;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.utility.maths.MathUtils;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.SoundEvent;
import org.lwjgl.input.Keyboard;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.AnimationUtils;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.client.handler.KeyInputHandler;
import com.modularwarfare.client.hud.FlashSystem;
import com.modularwarfare.client.laser.LaserDotRenderer;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
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
    public static int mouseDX;
    public static int mouseDY;

    public static float cemeraBobbing=0f;
    public static float cemeraBobTarget=0f;
    public static float cemeraBobVelocity=0f;
    public static float cemeraBobDamping=0.945f;
    public static float cemeraBobSpringForce=0.45f;
    public static float cemeraBobOscillationSpeed=1.2f;
    public static float cemeraBobReturnForce=0.035f;
    public static long lastFrameTime = System.currentTimeMillis();
    public static float averageDeltaTime = 1.0f / 60.0f; // 新增：用于平滑帧率变化
    public static final float ALPHA = 0.2f; // 新增：平滑系数
    public boolean lastJump;
    public static boolean isJetFly=false;
    public static float jetPower=0;
    public static int jetFireTime=0;
    public static int jetCoolTime=0;
    public static boolean serverCustomInventory = false;
    public static boolean serverAllowGunModifyGui = false;
    
    @SubscribeEvent
    public void onInput(InputUpdateEvent event) {
        EntityPlayerSP player=Minecraft.getMinecraft().player;
        if (event.getMovementInput().jump && !lastJump && !player.onGround && player.motionY < 0.0D && !player.isElytraFlying() && !player.capabilities.isFlying)
        {
            ModularWarfare.NETWORK.sendToServer(new PacketBackpackElytraStart());
        }
        if (event.getMovementInput().jump && !lastJump && !player.onGround && player.isElytraFlying() && !player.capabilities.isFlying)
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
    
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        mouseDX=event.getDx();
        mouseDY=event.getDy();


        if(event.getDwheel()!=0)
        {
            ItemStack sightItem = GunType.getAttachment(Minecraft.getMinecraft().player.getHeldItemMainhand(), AttachmentPresetEnum.Sight);

            if (ClientRenderHooks.isAimingScope && sightItem!=null && sightItem.getItem() instanceof ItemAttachment) {
                ItemAttachment itemAttachment = ((ItemAttachment) sightItem.getItem());
                AttachmentRenderConfig cfg = ((ModelAttachment) itemAttachment.baseType.model)    .config;
                AttachmentRenderConfig.Sight sight = cfg.sight;

                SoundEvent se = null;

                if(sight.fovZoomStage!=null){
                    int dump = sight.fovZoomStageIndex;

                    sight.fovZoomStageIndex+=event.getDwheel()>0?1:-1;
                    if(sight.fovZoomStageIndex<0)                                  sight.fovZoomStageIndex=sight.fovZoomStage.length-1;
                    else if(sight.fovZoomStageIndex>=sight.fovZoomStage.length-1)  sight.fovZoomStageIndex=0;

                    sight.fovZoom = sight.fovZoomStage[sight.fovZoomStageIndex];

                    se = (dump == sight.fovZoomStageIndex) ? null : itemAttachment.type.getSound(Minecraft.getMinecraft().player, WeaponSoundType.AttFOVChange);

                    event.setCanceled(true);
                }else if(sight.fovZoomMax>1F) {
                    float dump = sight.fovZoom;
                    sight.fovZoom = MathUtils.clamp(
                            sight.fovZoom + event.getDwheel()/500F,
                            sight.fovZoomMin,
                            sight.fovZoomMax
                    );

                    se =    (dump==sight.fovZoom) ? null : itemAttachment.type.getSound(Minecraft.getMinecraft().player, WeaponSoundType.Att_FOVStage);

                    event.setCanceled(true);
                }

                if(se!=null)
                {
                    PositionedSoundRecord sound = PositionedSoundRecord.getRecord(se, 1F, 1F);
                    Minecraft.getMinecraft().getSoundHandler().playSound(sound);
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void cemeraSetup(CameraSetup event) {
        if (Minecraft.getMinecraft().player != null) {
            if (Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() instanceof ItemGun) {
                ItemGun gun = (ItemGun)Minecraft.getMinecraft().player.getHeldItemMainhand().getItem();
                if (gun.type.enhancedModel != null) {
                    GlStateManager.rotate(90, 0, 1, 0);
                    GlStateManager.rotate((float)Math.toDegrees(RenderGunEnhanced.mwf_camera_rot.angle),
                        (float)-RenderGunEnhanced.mwf_camera_rot.x, (float)-RenderGunEnhanced.mwf_camera_rot.y,
                        (float)-RenderGunEnhanced.mwf_camera_rot.z);
                    GlStateManager.translate(RenderGunEnhanced.mwf_camera_pos.x / 10,
                        RenderGunEnhanced.mwf_camera_pos.y / 10, RenderGunEnhanced.mwf_camera_pos.z / 10);
                    GlStateManager.rotate(90, 0, -1, 0);
                    if (RenderGunEnhanced.mwf_camera_pos.x != 0 || RenderGunEnhanced.mwf_camera_pos.y != 0
                        || RenderGunEnhanced.mwf_camera_pos.z != 0 || RenderGunEnhanced.mwf_camera_rot.angle != 0) {
                        Minecraft.getMinecraft().renderGlobal.setDisplayListEntitiesDirty();
                    }
                }
            }
        }

        // 计算实际的帧间时间
        long currentTime = System.currentTimeMillis();
        float actualDeltaTime = (currentTime - lastFrameTime) / 1000.0f;
        
        // 如果时间步长过大，说明可能发生了卡顿
        if (actualDeltaTime > 0.1f) {
            lastFrameTime = currentTime;
            return;
        }
        
        // 使用指数移动平均来平滑帧率变化
        if (actualDeltaTime > 0) {
            averageDeltaTime = averageDeltaTime * (1 - ALPHA) + actualDeltaTime * ALPHA;
        }
        
        // 将平均帧率限制在合理范围内（30fps到240fps之间）
        float clampedDeltaTime = Math.min(Math.max(averageDeltaTime, 1.0f/240.0f), 1.0f/30.0f);
        
        // 根据实际帧率调整模拟步长
        float simulationSteps = Math.round(clampedDeltaTime / (1.0f/120.0f));
        float deltaTime = clampedDeltaTime / Math.max(simulationSteps, 1);
        
        lastFrameTime = currentTime;

        float springForce, returnForce, distanceToTarget, oscillation, totalForce, movement;
        
        // 进行多次物理更新以保持稳定性
        for(int i = 0; i < simulationSteps; i++) {
            // 计算弹簧力
            springForce = (cemeraBobTarget - cemeraBobbing) * cemeraBobSpringForce;
            
            // 计算返回中心的力
            returnForce = -cemeraBobbing * cemeraBobReturnForce;
            
            // 添加震荡效果
            distanceToTarget = Math.abs(cemeraBobTarget - cemeraBobbing);
            oscillation = (float)Math.sin(currentTime * 0.02f * cemeraBobOscillationSpeed)
                          * distanceToTarget * 0.2f
                          * Math.max(0, 1 - Math.abs(cemeraBobVelocity) * 0.4f);
            
            // 合并所有力并限制最大力的大小
            totalForce = springForce + returnForce + oscillation;
            totalForce = Math.max(Math.min(totalForce, 0.9f), -0.9f);
            
            // 更新速度，并限制最大速度
            cemeraBobVelocity += totalForce * deltaTime * 85;
            cemeraBobVelocity *= cemeraBobDamping;
            cemeraBobVelocity = Math.max(Math.min(cemeraBobVelocity, 0.9f), -0.9f);
            
            // 更新位置
            movement = cemeraBobVelocity * deltaTime * 85;
            movement = Math.max(Math.min(movement, 0.09f), -0.09f);
            cemeraBobbing += movement;
            
            // 缓慢衰减目标值
            cemeraBobTarget *= 0.965f;
        }
        
        // 如果运动几乎停止且接近中心位置，重置所有值
        if (Math.abs(cemeraBobVelocity) < 0.00001f && Math.abs(cemeraBobbing) < 0.00001f) {
            cemeraBobbing = 0;
            cemeraBobTarget = 0;
            cemeraBobVelocity = 0;
        }
        
        GlStateManager.rotate(cemeraBobbing * 5, 0, 0, 1);
    }
    
    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event) {
        InstantBulletRenderer.RenderAllTrails(event.getPartialTicks());
        LaserDotRenderer.renderLaserDots();
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
                if(backpack!=null) {
                    if (isJetFly && event.player.isElytraFlying()&&!event.player.isSneaking()) {
                        if (jetFireTime == 0 && jetCoolTime == 0) {
                            jetFireTime = 1;
                            ModularWarfare.NETWORK.sendToServer(new PacketBackpackJet(true, Minecraft.getMinecraft().player.getUniqueID()));
                        }
                    }
                    if(jetFireTime>0&&jetCoolTime==0){
                        double speed = event.player.motionX * event.player.motionX
                            + event.player.motionY * event.player.motionY + event.player.motionZ * event.player.motionZ;
                        speed = Math.sqrt(speed);
                        jetFireTime++;
                        if (jetFireTime > backpack.jetElytraBoostDuration) {
                            jetCoolTime = backpack.jetElytraBoostCoolTime;
                            jetFireTime=0;
                        }
                        Vec3d vec = event.player.getLookVec();
                        vec = vec.scale(Math.max(speed, backpack.jetElytraBoost));
                        if(event.player.isElytraFlying()) {
                            event.player.motionX = vec.x;
                            event.player.motionY = vec.y;
                            event.player.motionZ = vec.z;
                        }
                        AnimationUtils.isJet.put(event.player.getUniqueID(), System.currentTimeMillis()+100);
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
                        ModularWarfare.NETWORK.sendToServer(new PacketBackpackJet(false, Minecraft.getMinecraft().player.getUniqueID()));
                        AnimationUtils.isJet.put(event.player.getUniqueID(), System.currentTimeMillis()+100);
                    }else {
                        jetPower=backpack.jetIdleForce;
                    }   
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiLaunch(GuiOpenEvent event) {
        if (serverCustomInventory) {
            if (event.getGui() != null && event.getGui().getClass() == GuiInventory.class) {
                final EntityPlayer player = Minecraft.getMinecraft().player;
                if (!player.isCreative()) {
                    event.setCanceled(true);
                    ModularWarfare.NETWORK.sendToServer(new PacketOpenGui(0));
                }
            }
        }
    }

    @SubscribeEvent
    public void onCustomPacketRegistration(FMLNetworkEvent.CustomPacketRegistrationEvent<INetHandlerPlayClient> event) {
        boolean isRegister = event.getOperation().equals("REGISTER");
        if (event.getRegistrations().contains("MWF_sync_customInventory_enabled")) {
            serverCustomInventory = isRegister;
        }
        if (event.getRegistrations().contains("MWF_sync_customInventory_disabled")) {
            serverCustomInventory = !isRegister;
        }
        if (event.getRegistrations().contains("MWF_sync_allowGunModifyGui_enabled")) {
            serverAllowGunModifyGui = isRegister;
        }
        if (event.getRegistrations().contains("MWF_sync_allowGunModifyGui_disabled")) {
            serverAllowGunModifyGui = !isRegister;
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

