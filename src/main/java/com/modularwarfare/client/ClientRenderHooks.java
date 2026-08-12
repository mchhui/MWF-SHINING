package com.modularwarfare.client;

import org.lwjgl.input.Keyboard;
import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.OffhandHideHelper;
import com.modularwarfare.api.AnimationUtils;
import com.modularwarfare.api.RenderHandFisrtPersonEvent;
import com.modularwarfare.client.fpp.basic.animations.AnimStateMachine;
import com.modularwarfare.client.fpp.basic.configs.ArmorRenderConfig;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.basic.renderers.*;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGrenadeEnhanced;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderMelee;
import com.modularwarfare.client.gui.GuiGunModify;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.client.laser.LaserRenderManager;
import com.modularwarfare.client.compat.AtomicShaderCompat;
import com.modularwarfare.client.flashlight.FlashlightRenderManager;
import com.modularwarfare.client.model.ModelCustomArmor;
import com.modularwarfare.client.scope.ScopeUtils;
import com.modularwarfare.client.view.ShoulderAimCorrect;
import com.modularwarfare.common.armor.ArmorType;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.entity.grenades.EntitySmokeGrenade;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.melee.ItemMelee;
import com.modularwarfare.common.melee.MeleeType;
import com.modularwarfare.common.network.PacketAimingRequest;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.utility.OptifineHelper;
import com.modularwarfare.utility.RenderHelperMW;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBiped.ArmPose;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.glu.Project;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.UUID;

@SideOnly(Side.CLIENT)
public class ClientRenderHooks {

    public static HashMap<EntityLivingBase, AnimStateMachine> weaponBasicAnimations = new HashMap<EntityLivingBase, AnimStateMachine>();
    public static IdentityHashMap<EntityLivingBase, EnhancedStateMachine> weaponEnhancedAnimations = new IdentityHashMap<EntityLivingBase, EnhancedStateMachine>();

    //这个数组到底是那个蠢蛋想出来的
//    public static CustomItemRenderer[] customRenderers = new CustomItemRenderer[20];
    public static boolean isAimingScope;
    public static boolean isAiming;
    public float partialTicks;
    private Minecraft mc;
    private float equippedProgress = 1f, prevEquippedProgress = 1f;
    public static boolean debug=false;
    
    //增强型换枪参数
    public static int currentGun=-1;
    public static int wannaSlot=-1;

    public static final ResourceLocation grenade_smoke = new ResourceLocation("modularwarfare", "textures/particles/smoke.png");
    public static final ResourceLocation grenade_smoke_model = null;

    private static final HashMap<UUID, SmoothAimPose> aimSmooth = new HashMap<UUID, SmoothAimPose>();
    private static final HashMap<UUID, float[]> aimPitchBackup = new HashMap<UUID, float[]>();
    private static long aimBodyLastNs;
    private static float aimBodyFrameDt = 1f / 60f;
    private static long aimSmoothRenderStamp;

    private static final class SmoothAimPose {
        float lookYaw;
        float lookPitch;
        float bodyYaw;
        boolean init;
        long steppedStamp;
    }

    public ClientRenderHooks() {
        mc = Minecraft.getMinecraft();
//        customRenderers[0] = ClientProxy.gunEnhancedRenderer = new RenderGunEnhanced();
//        customRenderers[1] = ClientProxy.gunStaticRenderer = new RenderGunStatic();
//        customRenderers[2] = ClientProxy.ammoRenderer = new RenderAmmo();
//        customRenderers[3] = ClientProxy.attachmentRenderer = new RenderAttachment();
//        customRenderers[8] = ClientProxy.grenadeStaticRenderer = new RenderGrenade();
//        customRenderers[10] = ClientProxy.grenadeEnhancedRenderer = new RenderGrenadeEnhanced();
        ClientProxy.gunEnhancedRenderer = new RenderGunEnhanced();
        ClientProxy.gunStaticRenderer = new RenderGunStatic();
        ClientProxy.ammoRenderer = new RenderAmmo();
        ClientProxy.attachmentRenderer = new RenderAttachment();
        ClientProxy.grenadeStaticRenderer = new RenderGrenade();
        ClientProxy.grenadeEnhancedRenderer = new RenderGrenadeEnhanced();
        ClientProxy.meleeRenderer = new RenderMelee();
    }

    public static AnimStateMachine getAnimMachine(EntityLivingBase entityPlayer) {
        AnimStateMachine animation = null;
        if (weaponBasicAnimations.containsKey(entityPlayer)) {
            animation = weaponBasicAnimations.get(entityPlayer);
        } else {
            animation = new AnimStateMachine();
            weaponBasicAnimations.put(entityPlayer, animation);
        }
        return animation;
    }

    public static EnhancedStateMachine getEnhancedAnimMachine(EntityLivingBase entityPlayer) {
        EnhancedStateMachine animation = null;
        if (weaponEnhancedAnimations.containsKey(entityPlayer)) {
            animation = weaponEnhancedAnimations.get(entityPlayer);
        } else {
            animation = new EnhancedStateMachine();
            weaponEnhancedAnimations.put(entityPlayer, animation);
        }
        return animation;
    }

    @SubscribeEvent
    void renderTick(TickEvent.RenderTickEvent event) {
        switch (event.phase) {
            case START: {
                long now = System.nanoTime();
                if (aimBodyLastNs != 0L) {
                    float dt = (now - aimBodyLastNs) * 1.0e-9f;
                    if (dt < 0.001f) {
                        dt = 0.001f;
                    } else if (dt > 0.05f) {
                        dt = 0.05f;
                    }
                    aimBodyFrameDt = dt;
                }
                aimBodyLastNs = now;
                aimSmoothRenderStamp++;
                RenderParameters.smoothing = event.renderTickTime;
                SetPartialTick(event.renderTickTime);
                if(!Minecraft.getMinecraft().getFramebuffer().isStencilEnabled()) {
                    Minecraft.getMinecraft().getFramebuffer().enableStencil();
                }
                boolean flag=true;
                if (Minecraft.getMinecraft().player != null) {
                    if (Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() instanceof ItemGun) {
                        if (((ItemGun)Minecraft.getMinecraft().player.getHeldItemMainhand()
                            .getItem()).type.animationType.equals(WeaponAnimationType.ENHANCED)) {
                            flag=false;
                        }
                    }
                }
                if(flag) {
                    currentGun=-1;
                }
                if(currentGun==-1&&wannaSlot!=-1) {
                    if(Minecraft.getMinecraft().player!=null) {
                        Minecraft.getMinecraft().player.inventory.currentItem=wannaSlot;
                        if (Minecraft.getMinecraft().currentScreen instanceof GuiGunModify) {
                            Minecraft.getMinecraft().displayGuiScreen(null);
                        }
                        wannaSlot=-1;  
                    }
                }
                break;
            }
            case END: {
                if (mc.player == null || mc.world == null) {
                    currentGun=-1;
                    wannaSlot=-1;
                    return;  
                }
                if (ClientProxy.gunUI.hitMarkerTime > 0)
                    ClientProxy.gunUI.hitMarkerTime--;
                ModularWarfare.NETWORK.sendToServer(new PacketAimingRequest(isAiming||isAimingScope));
                break;
            }
        }
    }

    @SubscribeEvent
    void onClientTickAimBody(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.world == null) {
            return;
        }
        for (EntityPlayer player : mc.world.playerEntities) {
            UUID id = player.getUniqueID();
            if (!isThirdPersonAiming(id)) {
                aimSmooth.remove(id);
                aimPitchBackup.remove(id);
                continue;
            }
            SmoothAimPose state = aimSmooth.get(id);
            if (state == null || !state.init) {
                stepAimPose(player, 1f, 1f / 20f);
                state = aimSmooth.get(id);
            }
            if (state == null) {
                continue;
            }
            player.prevRenderYawOffset = state.bodyYaw;
            player.renderYawOffset = state.bodyYaw;
            player.prevRotationYawHead = state.lookYaw;
            player.rotationYawHead = state.lookYaw;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    void onRender(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.getEntityPlayer();
        UUID id = player.getUniqueID();
        if (!isThirdPersonAiming(id)) {
            aimSmooth.remove(id);
            return;
        }
        float partialTicks = event.getPartialRenderTick();
        ShoulderAimCorrect.AimLook smoothed = stepAimPose(player, partialTicks, aimBodyFrameDt);
        player.prevRenderYawOffset = smoothed.bodyYaw;
        player.renderYawOffset = smoothed.bodyYaw;
        // Head (+ ModelBiped BOW arms) follow eased look; pitch restored in Post.
        aimPitchBackup.put(id, new float[] { player.prevRotationPitch, player.rotationPitch });
        player.prevRotationPitch = smoothed.lookPitch;
        player.rotationPitch = smoothed.lookPitch;
        player.prevRotationYawHead = smoothed.lookYaw;
        player.rotationYawHead = smoothed.lookYaw;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onRenderAimPoseRestore(RenderPlayerEvent.Post event) {
        UUID id = event.getEntityPlayer().getUniqueID();
        float[] backup = aimPitchBackup.remove(id);
        if (backup == null) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        player.prevRotationPitch = backup[0];
        player.rotationPitch = backup[1];
    }

    /**
     * Eased look for ELM aim-bone SET (same state as vanilla TP aim pose).
     */
    public static ShoulderAimCorrect.AimLook getAimLook(EntityPlayer player, float partialTicks) {
        UUID id = player.getUniqueID();
        SmoothAimPose state = aimSmooth.get(id);
        if (state != null && state.init && state.steppedStamp == aimSmoothRenderStamp) {
            return new ShoulderAimCorrect.AimLook(state.lookYaw, state.lookPitch, state.bodyYaw, true);
        }
        return stepAimPose(player, partialTicks, aimBodyFrameDt);
    }

    /**
     * Ease-out toward instant target: {@code aimBodySettleSeconds} ≈ time to close ~95%
     * (fast then slow). {@code 0} snaps.
     */
    private static ShoulderAimCorrect.AimLook stepAimPose(EntityPlayer player, float partialTicks, float dtSeconds) {
        ShoulderAimCorrect.AimLook target = ShoulderAimCorrect.resolve(player, partialTicks);
        UUID id = player.getUniqueID();
        SmoothAimPose state = aimSmooth.get(id);
        if (state == null) {
            state = new SmoothAimPose();
            aimSmooth.put(id, state);
        }
        float settle = aimBodySettleSeconds();
        if (!state.init || settle <= 1e-4f) {
            state.lookYaw = target.lookYaw;
            state.lookPitch = target.lookPitch;
            state.bodyYaw = target.bodyYaw;
            state.init = true;
            state.steppedStamp = aimSmoothRenderStamp;
            return new ShoulderAimCorrect.AimLook(state.lookYaw, state.lookPitch, state.bodyYaw,
                target.bodyFollowsLook);
        }
        float dt = dtSeconds;
        if (dt < 0.001f) {
            dt = 0.001f;
        } else if (dt > 0.05f) {
            dt = 0.05f;
        }
        // alpha = 1 - exp(-3*dt/settle) → ~95% of remaining gap closed after `settle` seconds.
        float alpha = 1f - (float) Math.exp(-3.0 * dt / settle);
        if (alpha < 0f) {
            alpha = 0f;
        } else if (alpha > 1f) {
            alpha = 1f;
        }
        state.lookYaw = state.lookYaw + MathHelper.wrapDegrees(target.lookYaw - state.lookYaw) * alpha;
        state.bodyYaw = state.bodyYaw + MathHelper.wrapDegrees(target.bodyYaw - state.bodyYaw) * alpha;
        float pitchDelta = target.lookPitch - state.lookPitch;
        state.lookPitch = state.lookPitch + pitchDelta * alpha;
        if (state.lookPitch < -90f) {
            state.lookPitch = -90f;
        } else if (state.lookPitch > 90f) {
            state.lookPitch = 90f;
        }
        state.steppedStamp = aimSmoothRenderStamp;
        return new ShoulderAimCorrect.AimLook(state.lookYaw, state.lookPitch, state.bodyYaw,
            target.bodyFollowsLook);
    }

    private static float aimBodySettleSeconds() {
        if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.client != null
            && ModConfig.INSTANCE.client.aimBodySettleSeconds != null) {
            return ModConfig.INSTANCE.client.aimBodySettleSeconds.floatValue();
        }
        return 0.5f;
    }

    @SubscribeEvent
    void renderItemFrame(RenderItemInFrameEvent event) {
        Item item = event.getItem().getItem();
        if (item instanceof ItemGun) {
            BaseType type = ((BaseItem) event.getItem().getItem()).baseType;
            if (type.enhancedModel != null) {
                event.setCanceled(true);

                int rotation = event.getEntityItemFrame().getRotation();
                GlStateManager.rotate(-rotation * 45F, 0F, 0F, 1F);
                RenderHelper.enableStandardItemLighting();
                GlStateManager.rotate(rotation * 45F, 0F, 0F, 1F);
                GlStateManager.pushMatrix();
                ClientProxy.gunEnhancedRenderer.drawThirdGun(null, RenderType.ITEMFRAME, null, event.getItem());
                GlStateManager.popMatrix();
            } else if (type.hasModel()) {
                event.setCanceled(true);

                int rotation = event.getEntityItemFrame().getRotation();
                GlStateManager.rotate(-rotation * 45F, 0F, 0F, 1F);
                RenderHelper.enableStandardItemLighting();
                GlStateManager.rotate(rotation * 45F, 0F, 0F, 1F);
                GlStateManager.pushMatrix();
                float scale = 0.75F;
                GlStateManager.scale(scale, scale, scale);
                GlStateManager.translate(0.15F, -0.15F, 0F);
                ClientProxy.gunStaticRenderer.renderItem(CustomItemRenderType.ENTITY, EnumHand.MAIN_HAND, event.getItem());
                GlStateManager.popMatrix();
            }
        } else if (item instanceof ItemGrenade) {
            BaseType type = ((BaseItem) event.getItem().getItem()).baseType;
            GrenadeType grenadeType = (GrenadeType) type;
            if (grenadeType.animationType == WeaponAnimationType.ENHANCED) {
                event.setCanceled(true);

                int rotation = event.getEntityItemFrame().getRotation();
                GlStateManager.rotate(-rotation * 45F, 0F, 0F, 1F);
                RenderHelper.enableStandardItemLighting();
                GlStateManager.rotate(rotation * 45F, 0F, 0F, 1F);
                GlStateManager.pushMatrix();
                ClientProxy.grenadeEnhancedRenderer.renderThirdPersonGrenade(null, RenderType.ITEMFRAME, null, event.getItem(), false);
                GlStateManager.popMatrix();
            } else if (type.hasModel()) {
                event.setCanceled(true);

                int rotation = event.getEntityItemFrame().getRotation();
                GlStateManager.rotate(-rotation * 45F, 0F, 0F, 1F);
                RenderHelper.enableStandardItemLighting();
                GlStateManager.rotate(rotation * 45F, 0F, 0F, 1F);
                GlStateManager.pushMatrix();
                float scale = 0.75F;
                GlStateManager.scale(scale, scale, scale);
                GlStateManager.translate(0.15F, -0.15F, 0F);
                ClientProxy.grenadeStaticRenderer.renderItem(CustomItemRenderType.ENTITY, EnumHand.MAIN_HAND, event.getItem());
                GlStateManager.popMatrix();
            }
        }else if(item instanceof ItemMelee) {
            BaseType type = ((BaseItem) event.getItem().getItem()).baseType;
            MeleeType meleeType = (MeleeType) type;
            event.setCanceled(true);
            int rotation = event.getEntityItemFrame().getRotation();
            GlStateManager.rotate(-rotation * 45F, 0F, 0F, 1F);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.rotate(rotation * 45F, 0F, 0F, 1F);
            GlStateManager.pushMatrix();
            ClientProxy.meleeRenderer.drawThirdMelee(null, RenderType.ITEMFRAME, null, event.getItem(), false);
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    void onWorldRenderLast(RenderWorldLastEvent event) {
        //For each entity loaded, process with layers
        for (Object o : mc.world.getLoadedEntityList()) {
            Entity givenEntity = (Entity) o;
            //If entity is smoke grenade, render smoke
            if (givenEntity instanceof EntitySmokeGrenade) {
                EntitySmokeGrenade smokeGrenade = (EntitySmokeGrenade) givenEntity;
                if (smokeGrenade.isExploded()) {
                    if (smokeGrenade.smokeTime > 0) {
                        // 保存当前GL状态
                        GlStateManager.pushMatrix();
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                        GlStateManager.disableLighting();
                        GlStateManager.depthMask(false);

                        // 计算烟雾大小
                        float scale = smokeGrenade.getSmokeScale();
                        int smokeSize = (int)(600 * scale);
                        
                        // 计算透明度
                        double alpha;
                        if (smokeGrenade.smokeTime > 200) {
                            // 淡入效果
                            alpha = 0.8 * (1.0 - (smokeGrenade.smokeTime - 200) / 20.0);
                        } else if (smokeGrenade.smokeTime < 20) {
                            // 淡出效果
                            alpha = 0.8 * (smokeGrenade.smokeTime / 20.0);
                        } else {
                            alpha = 0.8;
                        }

                        // 渲染烟雾
                        RenderHelperMW.renderSmoke(
                            grenade_smoke, // 纹理
                            null, // 不使用自定义模型
                            smokeGrenade.posX,
                            smokeGrenade.posY + 1,
                            smokeGrenade.posZ,
                            partialTicks,
                            smokeSize,
                            smokeSize,
                            "0xFFFFFF",
                            alpha
                        );

                        // 恢复GL状态
                        GlStateManager.depthMask(true);
                        GlStateManager.enableLighting();
                        GlStateManager.disableBlend();
                        GlStateManager.popMatrix();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    void onRenderHeldItem(RenderSpecificHandEvent event) {
        AtomicShaderCompat.onFirstPersonItemBegin();
        event.setCanceled(renderHeldItem(event.getItemStack(), event.getHand(), event.getPartialTicks(),getFOVModifier(event.getPartialTicks())));
    }
    
    // 强制接管hud渲染状态
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreRenderHUD(net.minecraftforge.client.event.RenderGameOverlayEvent.Pre event) {
        if (!ModConfig.INSTANCE.hud.forceRestoreBlendState) {
            return;
        }
        if (event.getType() == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, 
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, 
                GlStateManager.SourceFactor.ONE, 
                GlStateManager.DestFactor.ZERO
            );
        }
    }
    
    public boolean renderHeldItem(ItemStack stack,EnumHand hand,float partialTicksTime,float fov) {
        boolean result = false;
        if(mc.currentScreen instanceof GuiGunModify) {
        	return true;
        }
        if (hand == EnumHand.OFF_HAND && mc.player != null
                && OffhandHideHelper.shouldHideOffhandForMainhand(mc.player.getHeldItemMainhand())) {
            return true;
        }
        if (stack != null && stack.getItem() instanceof BaseItem) {
            BaseType type = ((BaseItem) stack.getItem()).baseType;
            BaseItem item = ((BaseItem) stack.getItem());

            if (hand != EnumHand.MAIN_HAND) {
                return true;
            }

            if (item.render3d&& type.hasModel() && !type.getAssetDir().equalsIgnoreCase("attachments")) {
                result=true;

                float partialTicks = partialTicksTime;
                EntityRenderer renderer = mc.entityRenderer;
                float farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16F;

                GL11.glDepthRange(ModConfig.INSTANCE.hud.handDepthRangeMin, ModConfig.INSTANCE.hud.handDepthRangeMax);
                
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();
                
                float zFar=2*farPlaneDistance;
                Project.gluPerspective(fov, (float)mc.displayWidth / (float)mc.displayHeight, 0.00001F, zFar);
                GlStateManager.scale(ModConfig.INSTANCE.hud.projectionScale.x, ModConfig.INSTANCE.hud.projectionScale.y,
                    ModConfig.INSTANCE.hud.projectionScale.z);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();
                GlStateManager.scale(1 / zFar, 1 / zFar, 1 / zFar);
                
                // 修复枪械渲染bug
                if(Double.isNaN(RenderParameters.collideFrontDistance)) {
                    RenderParameters.collideFrontDistance=0;
                }
                boolean flag = mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase) mc.getRenderViewEntity()).isPlayerSleeping();

                if (mc.gameSettings.thirdPersonView == 0 && !flag && !mc.gameSettings.hideGUI && !mc.playerController.isSpectator() && mc.getRenderViewEntity().equals(mc.player)) {
                    renderer.enableLightmap();
                    float f1 = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
                    EntityPlayerSP entityplayersp = this.mc.player;
                    float f2 = entityplayersp.getSwingProgress(partialTicks);
                    float f3 = entityplayersp.prevRotationPitch + (entityplayersp.rotationPitch - entityplayersp.prevRotationPitch) * partialTicks;
                    float f4 = entityplayersp.prevRotationYaw + (entityplayersp.rotationYaw - entityplayersp.prevRotationYaw) * partialTicks;

                    // Setup lighting
                    GlStateManager.disableLighting();
                    GlStateManager.pushMatrix();
                    GlStateManager.rotate(f3, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(f4, 0.0F, 1.0F, 0.0F);
                    RenderHelper.enableStandardItemLighting();
                    GlStateManager.popMatrix();

                    // Do lighting
                    int i = this.mc.world.getCombinedLight(new BlockPos(entityplayersp.posX, entityplayersp.posY + (double) entityplayersp.getEyeHeight(), entityplayersp.posZ), 0);
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (i & 65535), (float) (i >> 16));


                    // Do hand rotations
                    float f5 = entityplayersp.prevRenderArmPitch + (entityplayersp.renderArmPitch - entityplayersp.prevRenderArmPitch) * partialTicks;
                    float f6 = entityplayersp.prevRenderArmYaw + (entityplayersp.renderArmYaw - entityplayersp.prevRenderArmYaw) * partialTicks;
                    GlStateManager.rotate((entityplayersp.rotationPitch - f5) * 0.1F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate((entityplayersp.rotationYaw - f6) * 0.1F, 0.0F, 1.0F, 0.0F);

                    GlStateManager.enableRescaleNormal();
                    GlStateManager.pushMatrix();

                    // Do vanilla weapon swing
                    float f7 = -0.4F * MathHelper.sin(MathHelper.sqrt(f2) * (float) Math.PI);
                    float f8 = 0.2F * MathHelper.sin(MathHelper.sqrt(f2) * (float) Math.PI * 2.0F);
                    float f9 = -0.2F * MathHelper.sin(f2 * (float) Math.PI);
                    GlStateManager.translate(f7, f8, f9);

                    GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
                    GlStateManager.translate(0.0F, f1 * -0.6F, 0.0F);
                    GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                    float f10 = MathHelper.sin(f2 * f2 * (float) Math.PI);
                    float f11 = MathHelper.sin(MathHelper.sqrt(f2) * (float) Math.PI);
                    GlStateManager.rotate(f10 * -20.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(f11 * -20.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.rotate(f11 * -80.0F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.scale(0.4F, 0.4F, 0.4F);
                    
                    if(debug) {
                        System.out.println(new float[] {
                            f1,f2,f3,f4,f5,f6,f7,f8,f9,
                            f10,f11
                        });
                    }
                    
                    if(!ScopeUtils.isIndsideGunRendering) {
                        boolean needMirrorResources = false;
                        if (GunType.getAttachment(stack, AttachmentPresetEnum.Sight) != null) {
                            final ItemAttachment itemAttachment = (ItemAttachment) GunType.getAttachment(stack, AttachmentPresetEnum.Sight).getItem();
                            if (itemAttachment != null && itemAttachment.type != null && itemAttachment.type.sight.modeType.isMirror) {
                                needMirrorResources = true;
                            }
                        }
                        
                        if (needMirrorResources) {
                            ClientProxy.scopeUtils.initBlur();  
                        }
                        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
                        // OptiFine / scope FBO steal drops Hand MRT drawBuffers — restore fill only
                        // (do not rebind a stale held-item albedo before the item renderer runs).
                        if (AtomicShaderCompat.isGBufferFillActive()) {
                            AtomicShaderCompat.rebindFillIfActive();
                        }
                    }
                    GlStateManager.pushMatrix();

                    //Check if model is Basic or Enhanced for gun render
                    if(item instanceof ItemGun) {
                        if(((GunType)type).animationType.equals(WeaponAnimationType.BASIC)){
                            ClientProxy.gunStaticRenderer.renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, (ClientTickHandler.lastItemStack.isEmpty() ? stack : ClientTickHandler.lastItemStack), mc.world, mc.player);
                        } else{
                            //客户端预测需要 必须是即时物品
                            ItemStack heldStack = mc.player.getHeldItemMainhand();
                            // 总是更新currentGun为当前选中的槽位
                            currentGun = mc.player.inventory.currentItem;
                            
                            // 确保使用当前槽位的物品
                            if(mc.player.inventory.getStackInSlot(currentGun).getItem() instanceof ItemGun) {
                                heldStack = mc.player.inventory.getStackInSlot(currentGun);
                            }
                            
                            if (GunType.getAttachment(heldStack, AttachmentPresetEnum.Sight) != null) {
                                final ItemAttachment itemAttachment = (ItemAttachment) GunType.getAttachment(heldStack, AttachmentPresetEnum.Sight).getItem();
                                if(itemAttachment.type.sight.modeType.insideGunRendering) {
                                    renderInsideGun(heldStack, hand, partialTicksTime, fov);
                                    GL11.glDepthRange(ModConfig.INSTANCE.hud.handDepthRangeMin, ModConfig.INSTANCE.hud.handDepthRangeMax);
                                }
                            }
                            ClientProxy.gunEnhancedRenderer.renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, heldStack, mc.world, mc.player);
                            ScopeUtils.needRenderHand1=true;
                        }
                    } else if (item instanceof ItemGrenade) {
                        if(((GrenadeType)type).animationType.equals(WeaponAnimationType.BASIC)){
                            ClientProxy.grenadeStaticRenderer.renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, stack, mc.world, mc.player);
                        }else {
                            ClientProxy.grenadeEnhancedRenderer.renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, mc.player.getHeldItemMainhand(), mc.world, mc.player);
                        }
                    } else {
//                        customRenderers[type.id].renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, stack, mc.world, mc.player);
                        //这结构也是shit mount无疑了 想用java17的模式匹配ing
                        if (type instanceof AmmoType) {
                            ClientProxy.ammoRenderer.renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, stack, mc.world, mc.player);
                        } else if (type instanceof AttachmentType) {
                            ClientProxy.attachmentRenderer.renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, stack, mc.world, mc.player);
                        } else if (type instanceof MeleeType) {
                            ClientProxy.meleeRenderer.renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, stack, mc.world, mc.player);
                        }
                    }
                    
                    GlStateManager.popMatrix();
                    
                    GlStateManager.popMatrix();
                }
                
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
                
                GL11.glDepthRange(0, 1);

                
                if (ModConfig.INSTANCE.hud.forceRestoreBlendState) {
                    GlStateManager.enableLighting();
                    GlStateManager.enableDepth();
                    GlStateManager.depthMask(true);
                    GlStateManager.colorMask(true, true, true, true);
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, 
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, 
                        GlStateManager.SourceFactor.ONE, 
                        GlStateManager.DestFactor.ZERO);
                    GlStateManager.shadeModel(GL11.GL_FLAT);
                }

                // Game will render it. Why render overlays twice?
//                if (mc.gameSettings.thirdPersonView == 0 && !flag) {
//                    if(!ScopeUtils.isIndsideGunRendering) {
//                        itemRenderer.renderOverlays(partialTicks);
//                    }
//                }
                
            }
        }
        return result;
    }
    
    public void renderInsideGun(ItemStack stack,EnumHand hand,float partialTicksTime,float fov) {
        if(ScopeUtils.isIndsideGunRendering) {
            return;
        }
        if(!ScopeUtils.isRenderHand0&&OptifineHelper.isShadersEnabled()) {
            return;
        }
        
        if (ClientProxy.scopeUtils.blurFramebuffer == null) {
            return;
        }
        
        ScopeUtils.isIndsideGunRendering=true;

        boolean atomicInside = false;
        if (AtomicShaderCompat.isPipelineEnabled()
                && cloud.siz.atomic.api.render.AtomicGBufferCompat.beginOffscreenHandDeferred(partialTicksTime)) {
            atomicInside = true;
            int captureFbo = cloud.siz.atomic.api.render.AtomicGBufferCompat.getOffscreenHandCaptureFboId();
            if (captureFbo != 0) {
                // Blit main depth into offscreen-hand capture FBO (same idea as copyDepthBuffer).
                Minecraft mc = Minecraft.getMinecraft();
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, captureFbo);
                GlStateManager.colorMask(false, false, false, false);
                GL30.glBlitFramebuffer(0, 0, mc.displayWidth, mc.displayHeight, 0, 0, mc.displayWidth, mc.displayHeight,
                        GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
                GlStateManager.colorMask(true, true, true, true);
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
            }
            renderHeldItem(stack, hand, partialTicksTime, fov);
            cloud.siz.atomic.api.render.AtomicGBufferCompat.finishOffscreenHandDeferred(ScopeUtils.INSIDE_GUN_TEX);
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
            // Drop nested-pass albedo pointer; main FP renderItem will bind skin/gun fresh.
            // Do not rebindFillAndGunPbr here — that restored the inside-gun gun albedo onto TEX0
            // before arms, and melee→sighted-gun left arms sampling wrong.
            AtomicShaderCompat.clearCurrentFillAlbedo();
            if (AtomicShaderCompat.isGBufferFillActive()) {
                AtomicShaderCompat.rebindFillIfActive();
            }
        }

        if (!atomicInside) {
            int tex=ClientProxy.scopeUtils.blurFramebuffer.framebufferTexture;
            ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);
            GL30.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ScopeUtils.INSIDE_GUN_TEX, 0);
            GlStateManager.clearColor(0, 0, 0, 0);
            GL11.glClearColor(0, 0, 0, 0);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.depthMask(true);
            GlStateManager.clear (GL11.GL_DEPTH_BUFFER_BIT);
            copyDepthBuffer();
            ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
            renderHeldItem(stack, hand, partialTicksTime, fov);
            ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);
            GL30.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, tex, 0);
            ClientProxy.scopeUtils.blurFramebuffer.framebufferClear();
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
        }

        ScopeUtils.isIndsideGunRendering=false;
    }
    
    public static void copyDepthBuffer() {
        // 确保blurFramebuffer已初始化
        if (ClientProxy.scopeUtils.blurFramebuffer == null) {
            return;
        }
        
        Minecraft mc=Minecraft.getMinecraft();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ClientProxy.scopeUtils.blurFramebuffer.framebufferObject);
        GlStateManager.colorMask(false,false,false,false);
        GL30.glBlitFramebuffer(0, 0, mc.displayWidth, mc.displayHeight, 0, 0, mc.displayWidth, mc.displayHeight, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager.colorMask(true,true,true,true);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, GL11.GL_NONE);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, GL11.GL_NONE);
    }

    public void SetPartialTick(float dT) {
        partialTicks = dT;
    }
    
    public void hidePlayerModel(AbstractClientPlayer clientPlayer,RenderPlayer renderplayer) {
        ModelPlayer model = renderplayer.getMainModel();
        
        // 先重置所有部位为可见状态
        model.bipedHead.isHidden = false;
        model.bipedBody.isHidden = false;
        model.bipedLeftArm.isHidden = false;
        model.bipedRightArm.isHidden = false;
        model.bipedLeftLeg.isHidden = false;
        model.bipedRightLeg.isHidden = false;
        model.bipedHead.showModel = true;
        model.bipedBody.showModel = true;
        model.bipedLeftArm.showModel = true;
        model.bipedRightArm.showModel = true;
        model.bipedLeftLeg.showModel = true;
        model.bipedRightLeg.showModel = true;
        
        // 重置wear模型（第二层皮肤）
        model.bipedHeadwear.isHidden = false;
        model.bipedHeadwear.showModel = true;
        model.bipedLeftArmwear.isHidden = false;
        model.bipedLeftArmwear.showModel = true;
        model.bipedRightArmwear.isHidden = false;
        model.bipedRightArmwear.showModel = true;
        model.bipedBodyWear.isHidden = false;
        model.bipedBodyWear.showModel = true;
        model.bipedLeftLegwear.isHidden = false;
        model.bipedLeftLegwear.showModel = true;
        model.bipedRightLegwear.isHidden = false;
        model.bipedRightLegwear.showModel = true;
        
        // 根据配置隐藏第二层皮肤
        if(ModConfig.INSTANCE.client.hideSecondSkinWhenDressed) {
            if(clientPlayer.getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty()){
                model.bipedHeadwear.isHidden = false;
                model.bipedHeadwear.showModel = true;
            } else {
                model.bipedHeadwear.isHidden = true;
                model.bipedHeadwear.showModel = false;
            }
            if(clientPlayer.getItemStackFromSlot(EntityEquipmentSlot.CHEST).isEmpty()){
                model.bipedLeftArmwear.isHidden = false;
                model.bipedLeftArmwear.showModel = true;
                model.bipedRightArmwear.isHidden = false;
                model.bipedRightArmwear.showModel = true;
                model.bipedBodyWear.isHidden = false;
                model.bipedBodyWear.showModel = true;
            } else {
                model.bipedLeftArmwear.isHidden = true;
                model.bipedLeftArmwear.showModel = false;
                model.bipedRightArmwear.isHidden = true;
                model.bipedRightArmwear.showModel = false;
                model.bipedBodyWear.isHidden = true;
                model.bipedBodyWear.showModel = false;
            }
            if(clientPlayer.getItemStackFromSlot(EntityEquipmentSlot.LEGS).isEmpty()){
                model.bipedLeftLegwear.isHidden = false;
                model.bipedLeftLegwear.showModel = true;
                model.bipedRightLegwear.isHidden = false;
                model.bipedRightLegwear.showModel = true;
            } else {
                model.bipedLeftLegwear.isHidden = true;
                model.bipedLeftLegwear.showModel = false;
                model.bipedRightLegwear.isHidden = true;
                model.bipedRightLegwear.showModel = false;
            }  
        }
        
        // 根据护甲配置隐藏相应部位
        clientPlayer.getArmorInventoryList().forEach((stack) -> {
            ArmorType type = null;
            if (stack.getItem() instanceof ItemMWArmor) {
                type = ((ItemMWArmor) stack.getItem()).type;
            }
            if (stack.getItem() instanceof ItemSpecialArmor) {
                type = ((ItemSpecialArmor) stack.getItem()).type;
            }
            if (type != null) {
                ArmorRenderConfig config = ((ModelCustomArmor)type.bipedModel).config;
                if (config.extra.hidePlayerModel) {
                    if (config.extra.isSuit) {
                        // suit模式：隐藏整个玩家模型
                        model.bipedHead.isHidden = true;
                        model.bipedHead.showModel = false;
                        model.bipedBody.isHidden = true;
                        model.bipedBody.showModel = false;
                        model.bipedLeftArm.isHidden = true;
                        model.bipedLeftArm.showModel = false;
                        model.bipedRightArm.isHidden = true;
                        model.bipedRightArm.showModel = false;
                        model.bipedLeftLeg.isHidden = true;
                        model.bipedLeftLeg.showModel = false;
                        model.bipedRightLeg.isHidden = true;
                        model.bipedRightLeg.showModel = false;
                    } else {
                        // 非suit模式：根据护甲类型隐藏对应部位
                        switch (((ItemArmor) stack.getItem()).armorType) {
                        case HEAD:
                            model.bipedHead.isHidden = true;
                            model.bipedHead.showModel = false;
                            break;
                        case CHEST:
                            model.bipedBody.isHidden = true;
                            model.bipedBody.showModel = false;
                            model.bipedLeftArm.isHidden = true;
                            model.bipedLeftArm.showModel = false;
                            model.bipedRightArm.isHidden = true;
                            model.bipedRightArm.showModel = false;
                            break;
                        case LEGS:
                            model.bipedLeftLeg.isHidden = true;
                            model.bipedLeftLeg.showModel = false;
                            model.bipedRightLeg.isHidden = true;
                            model.bipedRightLeg.showModel = false;
                            break;
                        case FEET:
                            model.bipedLeftLeg.isHidden = true;
                            model.bipedLeftLeg.showModel = false;
                            model.bipedRightLeg.isHidden = true;
                            model.bipedRightLeg.showModel = false;
                            break;
                        default:
                            break;
                        }
                    }
                }
                if (config.extra.hideAllPlayerWearModel) {
                    model.bipedHeadwear.isHidden = true;
                    model.bipedHeadwear.showModel = false;
                    model.bipedLeftArmwear.isHidden = true;
                    model.bipedLeftArmwear.showModel = false;
                    model.bipedRightArmwear.isHidden = true;
                    model.bipedRightArmwear.showModel = false;
                    model.bipedBodyWear.isHidden = true;
                    model.bipedBodyWear.showModel = false;
                    model.bipedLeftLegwear.isHidden = true;
                    model.bipedLeftLegwear.showModel = false;
                    model.bipedRightLegwear.isHidden = true;
                    model.bipedRightLegwear.showModel = false;
                }
            }
        });
    }

    @SubscribeEvent
    public void renderThirdPose(RenderLivingEvent.Pre<?> event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer)) {
            return;
        }
        AbstractClientPlayer clientPlayer = (AbstractClientPlayer)event.getEntity();
        Render<AbstractClientPlayer> render = Minecraft.getMinecraft().getRenderManager().<AbstractClientPlayer>getEntityRenderObject(event.getEntity());
        RenderPlayer renderplayer = (RenderPlayer) render;

        //hide begin
        hidePlayerModel(clientPlayer, renderplayer);
        //hide end


        ItemStack itemstack = event.getEntity().getHeldItemMainhand();
        if (itemstack != ItemStack.EMPTY && !itemstack.isEmpty()) {
            if (!(itemstack.getItem() instanceof BaseItem)) {
                return;
            }
            BaseType type = ((BaseItem) itemstack.getItem()).baseType;
            if (!type.hasModel()) {
                return;
            }
            if (itemstack.getItem() instanceof ItemAttachment) {
                return;
            }
            if (itemstack.getItem() instanceof ItemBackpack) {
                return;
            }

            ModelBiped biped = (ModelBiped) event.getRenderer().getMainModel();
            Entity entity = event.getEntity();
            if (type.id == 1 && entity instanceof EntityPlayer) {
                if (isThirdPersonAiming(entity.getUniqueID())) {
                    biped.rightArmPose = ArmPose.BOW_AND_ARROW;
                } else {
                    biped.rightArmPose = ArmPose.BLOCK;
                    biped.leftArmPose = ArmPose.BLOCK;
                }
            } else {
                biped.rightArmPose = ArmPose.BLOCK;
            }
        }
    }
    
    @SubscribeEvent
    public void onRenderHand(RenderHandFisrtPersonEvent.Pre event) {
        AbstractClientPlayer clientPlayer = Minecraft.getMinecraft().player;
        clientPlayer.getArmorInventoryList().forEach((stack) -> {
            if (event.isCanceled()) {
                return;
            }
            ArmorType type = null;
            if (stack.getItem() instanceof ItemMWArmor) {
                type = ((ItemMWArmor) stack.getItem()).type;
            }
            if (stack.getItem() instanceof ItemSpecialArmor) {
                type = ((ItemSpecialArmor) stack.getItem()).type;
            }
            if (type != null) {
                ArmorRenderConfig config = type.renderConfig;
                if(config!=null) {
                    if (config.extra.hidePlayerModel) {
                        if (config.extra.isSuit) {
                            event.setCanceled(true);
                        } else if (((ItemArmor) stack.getItem()).armorType == EntityEquipmentSlot.CHEST) {
                            event.setCanceled(true);
                        }
                    }  
                }
            }
        });
    }

    private float getFOVModifier(float partialTicks) {
        Entity entity = this.mc.getRenderViewEntity();
        float f1 = 70.0F;

        if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0.0F) {
            float f2 = (float) ((EntityLivingBase) entity).deathTime + partialTicks;
            f1 /= (1.0F - 500.0F / (f2 + 500.0F)) * 2.0F + 1.0F;
        }

        IBlockState state = ActiveRenderInfo.getBlockStateAtEntityViewpoint(this.mc.world, entity, partialTicks);

        if (state.getMaterial() == Material.WATER)
            f1 = f1 * 60.0F / 70.0F;

        return f1;
    }

    private float interpolateRotation(float x, float y, float dT) {
        float f3;

        for (f3 = y - x; f3 < -180.0F; f3 += 360.0F) {
        }
        for (; f3 >= 180.0F; f3 -= 360.0F) {
        }

        return x + dT * f3;
    }

    public static boolean isThirdPersonAiming(java.util.UUID playerId) {
        if (playerId == null) {
            return false;
        }
        if (AnimationUtils.isAiming.containsKey(playerId)) {
            return true;
        }
        EntityPlayer player = resolveClientPlayer(playerId);
        if (player != null && isLaserOrFlashlightEnabledOnHeldGun(player.getHeldItemMainhand())) {
            return true;
        }
        if (LaserRenderManager.getInstance().getLaserState(playerId)) {
            return true;
        }
        return FlashlightRenderManager.getInstance().getFlashlightState(playerId);
    }

    /** Local or world-tracked player by UUID (client thread). */
    private static EntityPlayer resolveClientPlayer(java.util.UUID playerId) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return null;
        }
        if (mc.player != null && mc.player.getUniqueID().equals(playerId)) {
            return mc.player;
        }
        if (mc.world != null) {
            for (EntityPlayer p : mc.world.playerEntities) {
                if (p != null && playerId.equals(p.getUniqueID())) {
                    return p;
                }
            }
        }
        return null;
    }

    /** Gun NBT laser/flashlight flags when the matching attachment is present. */
    private static boolean isLaserOrFlashlightEnabledOnHeldGun(ItemStack held) {
        if (held == null || held.isEmpty() || !(held.getItem() instanceof ItemGun)) {
            return false;
        }
        ItemGun gun = (ItemGun) held.getItem();
        if (GunType.getAttachment(held, AttachmentPresetEnum.Laser) != null && gun.getLaserEnabled(held)) {
            return true;
        }
        if (GunType.getAttachment(held, AttachmentPresetEnum.Flashlight) != null
                && gun.getFlashlightEnabled(held)) {
            return true;
        }
        return false;
    }

}