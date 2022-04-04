package com.modularwarfare.client;

import com.google.common.base.Throwables;
import com.google.gson.JsonSyntaxException;
import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.AnimationUtils;
import com.modularwarfare.api.RenderBonesEvent;
import com.modularwarfare.api.RenderHandFisrtPersonEvent;
import com.modularwarfare.client.anim.AnimStateMachine;
import com.modularwarfare.client.config.ArmorRenderConfig;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.client.model.ModelCustomArmor.Bones.BonePart.EnumBoneType;
import com.modularwarfare.client.model.objects.CustomItemRenderType;
import com.modularwarfare.client.model.objects.CustomItemRenderer;
import com.modularwarfare.client.model.renders.*;
import com.modularwarfare.client.scope.ScopeUtils;
import com.modularwarfare.common.armor.ArmorType;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.entity.grenades.EntitySmokeGrenade;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.network.BackWeaponsManager;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.mixin.client.accessor.IShaderGroup;
import com.modularwarfare.utility.OptifineHelper;
import com.modularwarfare.utility.RenderHelperMW;
import com.modularwarfare.utility.event.ForgeEvent;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBiped.ArmPose;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
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
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.glu.Project;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

public class ClientRenderHooks extends ForgeEvent {

    public static HashMap<EntityLivingBase, AnimStateMachine> weaponAnimations = new HashMap<EntityLivingBase, AnimStateMachine>();
    public static CustomItemRenderer[] customRenderers = new CustomItemRenderer[9];
    public static boolean isAimingScope;
    public static boolean isAiming;
    public float partialTicks;
    private Minecraft mc;
    private float equippedProgress = 1f, prevEquippedProgress = 1f;

    public static final ResourceLocation grenade_smoke = new ResourceLocation("modularwarfare", "textures/particles/smoke.png");


    public ClientRenderHooks() {
        mc = Minecraft.getMinecraft();
        customRenderers[1] = ClientProxy.gunStaticRenderer = new RenderGunStatic();
        customRenderers[2] = ClientProxy.ammoRenderer = new RenderAmmo();
        customRenderers[3] = ClientProxy.attachmentRenderer = new RenderAttachment();
        customRenderers[8] = ClientProxy.grenadeRenderer = new RenderGrenade();
    }

    public static AnimStateMachine getAnimMachine(EntityPlayer entityPlayer) {
        AnimStateMachine animation = null;
        if (weaponAnimations.containsKey(entityPlayer)) {
            animation = weaponAnimations.get(entityPlayer);
        } else {
            animation = new AnimStateMachine();
            weaponAnimations.put(entityPlayer, animation);
        }
        return animation;
    }

    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        switch (event.phase) {
            case START: {
                RenderParameters.smoothing = event.renderTickTime;
                SetPartialTick(event.renderTickTime);
                break;
            }
            case END: {
                if (mc.player == null || mc.world == null)
                    return;
                if (ClientProxy.gunUI.hitMarkerTime > 0)
                    ClientProxy.gunUI.hitMarkerTime--;
                break;
            }
        }
    }

    @SubscribeEvent
    public void renderItemFrame(RenderItemInFrameEvent event) {
        Item item = event.getItem().getItem();
        if (item instanceof ItemGun) {
            BaseType type = ((BaseItem) event.getItem().getItem()).baseType;
            if (type.hasModel()) {
                event.setCanceled(true);

                int rotation = event.getEntityItemFrame().getRotation();
                GlStateManager.rotate(-rotation * 45F, 0F, 0F, 1F);
                RenderHelper.enableStandardItemLighting();
                GlStateManager.rotate(rotation * 45F, 0F, 0F, 1F);
                GlStateManager.pushMatrix();
                float scale = 0.75F;
                GlStateManager.scale(scale, scale, scale);
                GlStateManager.translate(0.15F, -0.15F, 0F);
                customRenderers[type.id].renderItem(CustomItemRenderType.ENTITY, EnumHand.MAIN_HAND, event.getItem());
                GlStateManager.popMatrix();
            }
        }
    }

    @SubscribeEvent
    public void onWorldRenderLast(RenderWorldLastEvent event) {
        //For each entity loaded, process with layers
        for (Object o : mc.world.getLoadedEntityList()) {
            Entity givenEntity = (Entity) o;
            //If entity is smoke grenade, render smoke
            if (givenEntity instanceof EntitySmokeGrenade) {
                EntitySmokeGrenade smokeGrenade = (EntitySmokeGrenade) givenEntity;
                if (smokeGrenade.exploded) {
                    if (smokeGrenade.smokeTime <= 220) {
                        RenderHelperMW.renderSmoke(grenade_smoke, smokeGrenade.posX, smokeGrenade.posY + 1, smokeGrenade.posZ, partialTicks, 600, 600, "0xFFFFFF", 0.8f);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void renderHeldItem(RenderSpecificHandEvent event) {
        EntityPlayer player = mc.player;
        ItemStack stack = event.getItemStack();

        if (stack != null && stack.getItem() instanceof BaseItem) {
            BaseType type = ((BaseItem) stack.getItem()).baseType;
            BaseItem item = ((BaseItem) stack.getItem());

            if (event.getHand() != EnumHand.MAIN_HAND) {
                event.setCanceled(true);
                return;
            }

            if (type.id > customRenderers.length)
                return;

            if (item.render3d && customRenderers[type.id] != null && type.hasModel() && !type.getAssetDir().equalsIgnoreCase("attachments")) {
                //Cancel the hand render event so that we can do our own.
                event.setCanceled(true);

                float partialTicks = event.getPartialTicks();
                EntityRenderer renderer = mc.entityRenderer;
                float farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16F;
                ItemRenderer itemRenderer = mc.getItemRenderer();

                if (OptifineHelper.isLoaded()) {
                    if (!OptifineHelper.isShadersEnabled()) {
                        GlStateManager.clear(256);
                        GlStateManager.matrixMode(5889);
                        GlStateManager.loadIdentity();
                    }
                } else {
                    GlStateManager.clear(256);
                    GlStateManager.matrixMode(5889);
                    GlStateManager.loadIdentity();
                }

                Project.gluPerspective(getFOVModifier(partialTicks), (float) mc.displayWidth / (float) mc.displayHeight, 0.0001F, farPlaneDistance * 2.0F);
                GlStateManager.matrixMode(5888);
                GlStateManager.loadIdentity();

                GlStateManager.pushMatrix();

                boolean flag = mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase) mc.getRenderViewEntity()).isPlayerSleeping();

                if (mc.gameSettings.thirdPersonView == 0 && !flag && !mc.gameSettings.hideGUI && !mc.playerController.isSpectator()) {
                    renderer.enableLightmap();
                    float f1 = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
                    EntityPlayerSP entityplayersp = this.mc.player;
                    float f2 = entityplayersp.getSwingProgress(partialTicks);
                    float f3 = entityplayersp.prevRotationPitch + (entityplayersp.rotationPitch - entityplayersp.prevRotationPitch) * partialTicks;
                    float f4 = entityplayersp.prevRotationYaw + (entityplayersp.rotationYaw - entityplayersp.prevRotationYaw) * partialTicks;

                    //Setup lighting
                    GlStateManager.disableLighting();
                    GlStateManager.pushMatrix();
                    GlStateManager.rotate(f3, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(f4, 0.0F, 1.0F, 0.0F);
                    RenderHelper.enableStandardItemLighting();
                    GlStateManager.popMatrix();

                    //Do lighting
                    int i = this.mc.world.getCombinedLight(new BlockPos(entityplayersp.posX, entityplayersp.posY + (double) entityplayersp.getEyeHeight(), entityplayersp.posZ), 0);
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (i & 65535), (float) (i >> 16));


                    //Do hand rotations
                    float f5 = entityplayersp.prevRenderArmPitch + (entityplayersp.renderArmPitch - entityplayersp.prevRenderArmPitch) * partialTicks;
                    float f6 = entityplayersp.prevRenderArmYaw + (entityplayersp.renderArmYaw - entityplayersp.prevRenderArmYaw) * partialTicks;
                    GlStateManager.rotate((entityplayersp.rotationPitch - f5) * 0.1F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate((entityplayersp.rotationYaw - f6) * 0.1F, 0.0F, 1.0F, 0.0F);

                    GlStateManager.enableRescaleNormal();
                    GlStateManager.pushMatrix();

                    //Do vanilla weapon swing
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
                    if (!OptifineHelper.isShadersEnabled()&&ModConfig.INSTANCE.hud.ads_blur) {
                        ClientProxy.scopeUtils.initBlur();
                        GlStateManager.pushMatrix();
                        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ClientProxy.scopeUtils.blurFramebuffer.framebufferObject);
                        GL30.glBlitFramebuffer(0, 0, mc.displayWidth, mc.displayHeight, 0, 0, mc.displayWidth, mc.displayHeight, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
                        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
                        GL11.glEnable(GL11.GL_STENCIL_TEST);
                        GL11.glStencilMask(0xFF);
                        GL11.glClearStencil(0);
                        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
                        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
                        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0XFF);

                        if(item instanceof ItemGun) {
                            customRenderers[type.id].renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, event.getHand(), (ClientTickHandler.lastItemStack.isEmpty() ? stack : ClientTickHandler.lastItemStack), mc.world, mc.player);
                        } else {
                            customRenderers[type.id].renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, event.getHand(), stack, mc.world, mc.player);
                        }
                        GL11.glStencilMask(0x00);
                        GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0XFF);

                        boolean needBlur = false;
                        if(RenderParameters.adsSwitch != 0F) {
                            if (GunType.getAttachment(mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentEnum.Sight) != null) {
                                final ItemAttachment itemAttachment = (ItemAttachment) GunType.getAttachment(mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentEnum.Sight).getItem();
                                if (itemAttachment != null) {
                                    if (itemAttachment.type != null) {
                                        if (itemAttachment.type.sight.scopeType != WeaponScopeType.REDDOT) {
                                            if (!OptifineHelper.isShadersEnabled()) {
                                                needBlur = true;
                                                ClientProxy.scopeUtils.renderBlur();
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        mc.getFramebuffer().bindFramebuffer(false);
                        ClientProxy.scopeUtils.blurFramebuffer.bindFramebufferTexture();
                        GlStateManager.pushMatrix();
                        ScaledResolution resolution = new ScaledResolution(mc);
                        Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
                        
                        GlStateManager.disableRescaleNormal();
                        RenderHelper.disableStandardItemLighting();
                        renderer.disableLightmap();

                        if (needBlur) {
                            ClientProxy.scopeUtils.drawScaledCustomSizeModalRectFlipY(0, 0, 0, 0, 1, 1, resolution.getScaledWidth(), resolution.getScaledHeight(), 1, 1);
                        }
                        GlStateManager.popMatrix();
                        GL11.glDisable(GL11.GL_STENCIL_TEST);
                        GlStateManager.popMatrix();
                    }else {
                        if(item instanceof ItemGun) {
                            customRenderers[type.id].renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, event.getHand(), (ClientTickHandler.lastItemStack.isEmpty() ? stack : ClientTickHandler.lastItemStack), mc.world, mc.player);
                        } else {
                            customRenderers[type.id].renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, event.getHand(), stack, mc.world, mc.player);
                        }
                    }
                    GlStateManager.disableRescaleNormal();
                    RenderHelper.disableStandardItemLighting();
                    renderer.disableLightmap();
                    GlStateManager.popMatrix();
                }
                GlStateManager.popMatrix();

                if (mc.gameSettings.thirdPersonView == 0 && !flag) {
                    itemRenderer.renderOverlays(partialTicks);
                }
            }
        }
    }

    public void SetPartialTick(float dT) {
        partialTicks = dT;
    }

    @SubscribeEvent
    public void renderThirdPose(RenderLivingEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer)) {
            return;
        }

        AbstractClientPlayer clientPlayer = (AbstractClientPlayer) event.getEntity();
        Render<AbstractClientPlayer> render = Minecraft.getMinecraft().getRenderManager()
                .<AbstractClientPlayer>getEntityRenderObject(event.getEntity());
        RenderPlayer renderplayer = (RenderPlayer) render;

        if (clientPlayer.getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty()) {
            renderplayer.getMainModel().bipedHeadwear.isHidden = false;
        } else {
            renderplayer.getMainModel().bipedHeadwear.isHidden = true;
        }
        if (clientPlayer.getItemStackFromSlot(EntityEquipmentSlot.CHEST).isEmpty()) {
            renderplayer.getMainModel().bipedLeftArmwear.isHidden = false;
            renderplayer.getMainModel().bipedRightArmwear.isHidden = false;
            renderplayer.getMainModel().bipedBodyWear.isHidden = false;
        } else {
            renderplayer.getMainModel().bipedLeftArmwear.isHidden = true;
            renderplayer.getMainModel().bipedRightArmwear.isHidden = true;
            renderplayer.getMainModel().bipedBodyWear.isHidden = true;
        }
        if (clientPlayer.getItemStackFromSlot(EntityEquipmentSlot.LEGS).isEmpty()) {
            renderplayer.getMainModel().bipedLeftLegwear.isHidden = false;
            renderplayer.getMainModel().bipedRightLegwear.isHidden = false;
        } else {
            renderplayer.getMainModel().bipedLeftLegwear.isHidden = true;
            renderplayer.getMainModel().bipedRightLegwear.isHidden = true;
        }

        //hide begin
        renderplayer.getMainModel().bipedHead.isHidden = false;
        renderplayer.getMainModel().bipedBody.isHidden = false;
        renderplayer.getMainModel().bipedLeftArm.isHidden = false;
        renderplayer.getMainModel().bipedRightArm.isHidden = false;
        renderplayer.getMainModel().bipedLeftLeg.isHidden = false;
        renderplayer.getMainModel().bipedRightLeg.isHidden = false;
        renderplayer.getMainModel().bipedHead.showModel = true;
        renderplayer.getMainModel().bipedBody.showModel = true;
        renderplayer.getMainModel().bipedLeftArm.showModel = true;
        renderplayer.getMainModel().bipedRightArm.showModel = true;
        renderplayer.getMainModel().bipedLeftLeg.showModel = true;
        renderplayer.getMainModel().bipedRightLeg.showModel = true;
        clientPlayer.getArmorInventoryList().forEach((stack) -> {
            ArmorType type = null;
            if (stack.getItem() instanceof ItemMWArmor) {
                type = ((ItemMWArmor) stack.getItem()).type;
            }
            if (stack.getItem() instanceof ItemSpecialArmor) {
                type = ((ItemSpecialArmor) stack.getItem()).type;
            }
            if (type != null) {
                ArmorRenderConfig config = ModularWarfare.getRenderConfig(type, ArmorRenderConfig.class);
                if (config.extra.hidePlayerModel) {
                    boolean hide = true;
                    if (config.extra.isSuit) {
                        renderplayer.getMainModel().bipedHead.isHidden = true;
                        renderplayer.getMainModel().bipedBody.isHidden = true;
                        renderplayer.getMainModel().bipedLeftArm.isHidden = true;
                        renderplayer.getMainModel().bipedRightArm.isHidden = true;
                        renderplayer.getMainModel().bipedLeftLeg.isHidden = true;
                        renderplayer.getMainModel().bipedRightLeg.isHidden = true;
                    } else {
                        switch (((ItemArmor) stack.getItem()).armorType) {
                        case HEAD:
                            renderplayer.getMainModel().bipedHead.isHidden = hide;
                            break;
                        case CHEST:
                            renderplayer.getMainModel().bipedBody.isHidden = hide;
                            renderplayer.getMainModel().bipedLeftArm.isHidden = hide;
                            renderplayer.getMainModel().bipedRightArm.isHidden = hide;
                            break;
                        case LEGS:
                            renderplayer.getMainModel().bipedLeftLeg.isHidden = hide;
                            renderplayer.getMainModel().bipedRightLeg.isHidden = hide;
                            break;
                        case FEET:
                            renderplayer.getMainModel().bipedLeftLeg.isHidden = hide;
                            renderplayer.getMainModel().bipedRightLeg.isHidden = hide;
                            break;
                        default:
                            break;
                        }
                    }
                }
                if (config.extra.hideAllPlayerWearModel) {
                    renderplayer.getMainModel().bipedHeadwear.isHidden = true;
                    renderplayer.getMainModel().bipedLeftArmwear.isHidden = true;
                    renderplayer.getMainModel().bipedRightArmwear.isHidden = true;
                    renderplayer.getMainModel().bipedBodyWear.isHidden = true;
                    renderplayer.getMainModel().bipedLeftLegwear.isHidden = true;
                    renderplayer.getMainModel().bipedRightLegwear.isHidden = true;
                }
            }
        });
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
                if (AnimationUtils.isAiming.containsKey(((EntityPlayer) entity).getName())) {
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
                ArmorRenderConfig config = ModularWarfare.getRenderConfig(type, ArmorRenderConfig.class);
                if (config.extra.hidePlayerModel) {
                    if (config.extra.isSuit) {
                        event.setCanceled(true);
                    } else if (((ItemArmor) stack.getItem()).armorType == EntityEquipmentSlot.CHEST) {
                        event.setCanceled(true);
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

}