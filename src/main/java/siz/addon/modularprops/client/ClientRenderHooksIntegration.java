package siz.addon.modularprops.client;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import com.modularwarfare.ModConfig;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.OffhandHideHelper;
import com.modularwarfare.client.compat.AtomicShaderCompat;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.gui.GuiGunModify;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.client.scope.ScopeUtils;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.AmmoType;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.common.guns.AttachmentType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAttachment;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.melee.MeleeType;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.utility.OptifineHelper;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import siz.addon.modularprops.client.fpp.enhanced.renderers.RenderCustomItemEnhanced;
import siz.addon.modularprops.client.fpp.enhanced.renderers.RenderCustomBlockEnhanced;
import siz.addon.modularprops.common.custom.ItemCustom;
import siz.addon.modularprops.common.custom.CustomBlockType;
import siz.addon.modularprops.common.custom.CustomItemType;
import siz.addon.modularprops.common.custom.ItemBlockCustom;

/**
 * 集成到ModularWarfare的渲染系统 通过事件系统确保自定义道具能够被正确渲染
 */
@SideOnly(Side.CLIENT)
public class ClientRenderHooksIntegration {
    private float equippedProgress = 1f, prevEquippedProgress = 1f;
    public static RenderCustomItemEnhanced customItemRenderer = new RenderCustomItemEnhanced();
    public static RenderCustomBlockEnhanced customBlockRenderer = new RenderCustomBlockEnhanced();

    private float getFOVModifier(float partialTicks) {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        float f1 = 70.0F;

        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).getHealth() <= 0.0F) {
            float f2 = (float)((EntityLivingBase)entity).deathTime + partialTicks;
            f1 /= (1.0F - 500.0F / (f2 + 500.0F)) * 2.0F + 1.0F;
        }

        IBlockState state = ActiveRenderInfo.getBlockStateAtEntityViewpoint(Minecraft.getMinecraft().world, entity, partialTicks);

        if (state.getMaterial() == Material.WATER)
            f1 = f1 * 60.0F / 70.0F;

        return f1;
    }

    @SubscribeEvent
    void onRenderHeldItem(RenderSpecificHandEvent event) {
        event.setCanceled(renderHeldItem(event.getItemStack(), event.getHand(), event.getPartialTicks(), getFOVModifier(event.getPartialTicks())));
    }

    public boolean renderHeldItem(ItemStack stack, EnumHand hand, float partialTicksTime, float fov) {
        Minecraft mc = Minecraft.getMinecraft();
        boolean result = false;
        if (mc.currentScreen instanceof GuiGunModify) {
            return true;
        }
        if (hand == EnumHand.OFF_HAND && mc.player != null
                && OffhandHideHelper.shouldHideOffhandForMainhand(mc.player.getHeldItemMainhand())) {
            return true;
        }
        if (stack != null && stack.getItem() instanceof ItemCustom) {
            result = true;
            BaseType type = ((BaseItem)stack.getItem()).baseType;
            BaseItem item = ((BaseItem)stack.getItem());

            if (hand != EnumHand.MAIN_HAND) {
                return true;
            }

            if (item.render3d && type.hasModel() && !type.getAssetDir().equalsIgnoreCase("attachments")) {
                result = true;
                float partialTicks = partialTicksTime;
                EntityRenderer renderer = mc.entityRenderer;
                float farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16F;

                GL11.glDepthRange(ModConfig.INSTANCE.hud.handDepthRangeMin, ModConfig.INSTANCE.hud.handDepthRangeMax);

                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();

                float zFar = 2 * farPlaneDistance;
                Project.gluPerspective(fov, (float)mc.displayWidth / (float)mc.displayHeight, 0.00001F, zFar);
                GlStateManager.scale(ModConfig.INSTANCE.hud.projectionScale.x, ModConfig.INSTANCE.hud.projectionScale.y, ModConfig.INSTANCE.hud.projectionScale.z);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();
                GlStateManager.scale(1 / zFar, 1 / zFar, 1 / zFar);

                // Fixed the bug gun renders bug
                if (Double.isNaN(RenderParameters.collideFrontDistance)) {
                    RenderParameters.collideFrontDistance = 0;
                }
                boolean flag = mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase)mc.getRenderViewEntity()).isPlayerSleeping();

                if (mc.gameSettings.thirdPersonView == 0 && !flag && !mc.gameSettings.hideGUI && !mc.playerController.isSpectator() && mc.getRenderViewEntity().equals(mc.player)) {
                    renderer.enableLightmap();
                    float f1 = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
                    EntityPlayerSP entityplayersp = mc.player;
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
                    int i = mc.world.getCombinedLight(new BlockPos(entityplayersp.posX, entityplayersp.posY + (double)entityplayersp.getEyeHeight(), entityplayersp.posZ), 0);
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)(i & 65535), (float)(i >> 16));

                    // Do hand rotations
                    float f5 = entityplayersp.prevRenderArmPitch + (entityplayersp.renderArmPitch - entityplayersp.prevRenderArmPitch) * partialTicks;
                    float f6 = entityplayersp.prevRenderArmYaw + (entityplayersp.renderArmYaw - entityplayersp.prevRenderArmYaw) * partialTicks;
                    GlStateManager.rotate((entityplayersp.rotationPitch - f5) * 0.1F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate((entityplayersp.rotationYaw - f6) * 0.1F, 0.0F, 1.0F, 0.0F);

                    GlStateManager.enableRescaleNormal();
                    GlStateManager.pushMatrix();

                    // Do vanilla weapon swing
                    float f7 = -0.4F * MathHelper.sin(MathHelper.sqrt(f2) * (float)Math.PI);
                    float f8 = 0.2F * MathHelper.sin(MathHelper.sqrt(f2) * (float)Math.PI * 2.0F);
                    float f9 = -0.2F * MathHelper.sin(f2 * (float)Math.PI);
                    GlStateManager.translate(f7, f8, f9);

                    GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
                    GlStateManager.translate(0.0F, f1 * -0.6F, 0.0F);
                    GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                    float f10 = MathHelper.sin(f2 * f2 * (float)Math.PI);
                    float f11 = MathHelper.sin(MathHelper.sqrt(f2) * (float)Math.PI);
                    GlStateManager.rotate(f10 * -20.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(f11 * -20.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.rotate(f11 * -80.0F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.scale(0.4F, 0.4F, 0.4F);
                    GlStateManager.pushMatrix();

                    if (item instanceof ItemCustom) {
                        if (((CustomItemType)type).animationType.equals(WeaponAnimationType.BASIC)) {
                            
                        } else {
                            if (AtomicShaderCompat.isGBufferFillActive()) {
                                AtomicShaderCompat.rebindFillAndGunPbr();
                            }
                            customItemRenderer.renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, mc.player.getHeldItemMainhand(), mc.world, mc.player);
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
            }
        }
        if (stack != null && stack.getItem() instanceof ItemBlockCustom) {
            result = true;
            BaseType type = ((ItemBlockCustom)stack.getItem()).type;
            ItemBlockCustom item = ((ItemBlockCustom)stack.getItem());

            if (hand != EnumHand.MAIN_HAND) {
                return true;
            }

            if (type.hasModel() && !type.getAssetDir().equalsIgnoreCase("attachments")) {
                result = true;
                float partialTicks = partialTicksTime;
                EntityRenderer renderer = mc.entityRenderer;
                float farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16F;

                GL11.glDepthRange(ModConfig.INSTANCE.hud.handDepthRangeMin, ModConfig.INSTANCE.hud.handDepthRangeMax);

                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();

                float zFar = 2 * farPlaneDistance;
                Project.gluPerspective(fov, (float)mc.displayWidth / (float)mc.displayHeight, 0.00001F, zFar);
                GlStateManager.scale(ModConfig.INSTANCE.hud.projectionScale.x, ModConfig.INSTANCE.hud.projectionScale.y, ModConfig.INSTANCE.hud.projectionScale.z);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();
                GlStateManager.scale(1 / zFar, 1 / zFar, 1 / zFar);

                // Fixed the bug gun renders bug
                if (Double.isNaN(RenderParameters.collideFrontDistance)) {
                    RenderParameters.collideFrontDistance = 0;
                }
                boolean flag = mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase)mc.getRenderViewEntity()).isPlayerSleeping();

                if (mc.gameSettings.thirdPersonView == 0 && !flag && !mc.gameSettings.hideGUI && !mc.playerController.isSpectator() && mc.getRenderViewEntity().equals(mc.player)) {
                    renderer.enableLightmap();
                    float f1 = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
                    EntityPlayerSP entityplayersp = mc.player;
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
                    int i = mc.world.getCombinedLight(new BlockPos(entityplayersp.posX, entityplayersp.posY + (double)entityplayersp.getEyeHeight(), entityplayersp.posZ), 0);
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)(i & 65535), (float)(i >> 16));

                    // Do hand rotations
                    float f5 = entityplayersp.prevRenderArmPitch + (entityplayersp.renderArmPitch - entityplayersp.prevRenderArmPitch) * partialTicks;
                    float f6 = entityplayersp.prevRenderArmYaw + (entityplayersp.renderArmYaw - entityplayersp.prevRenderArmYaw) * partialTicks;
                    GlStateManager.rotate((entityplayersp.rotationPitch - f5) * 0.1F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate((entityplayersp.rotationYaw - f6) * 0.1F, 0.0F, 1.0F, 0.0F);

                    GlStateManager.enableRescaleNormal();
                    GlStateManager.pushMatrix();

                    // Do vanilla weapon swing
                    float f7 = -0.4F * MathHelper.sin(MathHelper.sqrt(f2) * (float)Math.PI);
                    float f8 = 0.2F * MathHelper.sin(MathHelper.sqrt(f2) * (float)Math.PI * 2.0F);
                    float f9 = -0.2F * MathHelper.sin(f2 * (float)Math.PI);
                    GlStateManager.translate(f7, f8, f9);

                    GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
                    GlStateManager.translate(0.0F, f1 * -0.6F, 0.0F);
                    GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                    float f10 = MathHelper.sin(f2 * f2 * (float)Math.PI);
                    float f11 = MathHelper.sin(MathHelper.sqrt(f2) * (float)Math.PI);
                    GlStateManager.rotate(f10 * -20.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(f11 * -20.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.rotate(f11 * -80.0F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.scale(0.4F, 0.4F, 0.4F);
                    GlStateManager.pushMatrix();

                    if (item instanceof ItemBlockCustom) {
                        if (((CustomBlockType)type).animationType.equals(WeaponAnimationType.BASIC)) {
                            
                        } else {
                            if (AtomicShaderCompat.isGBufferFillActive()) {
                                AtomicShaderCompat.rebindFillAndGunPbr();
                            }
                            customBlockRenderer.renderItem(CustomItemRenderType.EQUIPPED_FIRST_PERSON, hand, mc.player.getHeldItemMainhand(), mc.world, mc.player);
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
            }
        }
        return result;
    }

}
