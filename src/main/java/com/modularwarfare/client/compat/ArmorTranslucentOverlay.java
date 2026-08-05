package com.modularwarfare.client.compat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.modularwarfare.client.model.FakeLayerBipedArmor;

import cloud.siz.atomic.api.render.AtomicExternalDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Queues MWF translucent armor during entity G-buffer fill and redraws with SrcA on
 * {@link AtomicExternalDraw.AtomicExternalDrawEvent.EntityForwardOverlay}.
 */
@SideOnly(Side.CLIENT)
public final class ArmorTranslucentOverlay {

    private static final List<QueuedArmor> QUEUE = new ArrayList<QueuedArmor>();
    private static boolean drawing;
    private static Field layerRenderersField;
    private static Field mainModelField;
    public static final ArmorTranslucentOverlay INSTANCE = new ArmorTranslucentOverlay();

    private ArmorTranslucentOverlay() {
    }

    public static boolean isDrawing() {
        return drawing;
    }

    public static void beginFrame() {
        if (!drawing) {
            QUEUE.clear();
        }
    }

    public static void queue(
            EntityLivingBase entity,
            RenderLivingBase<?> renderer,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            float scale) {
        if (entity == null || renderer == null || drawing) {
            return;
        }
        QUEUE.add(
                new QueuedArmor(
                        entity,
                        renderer,
                        limbSwing,
                        limbSwingAmount,
                        partialTicks,
                        ageInTicks,
                        netHeadYaw,
                        headPitch,
                        scale));
    }

    @SubscribeEvent
    public void onEntityForwardOverlay(
            AtomicExternalDraw.AtomicExternalDrawEvent.EntityForwardOverlay event) {
        draw(event.getContext().getPartialTicks());
    }

    public static void draw(float partialTicks) {
        if (QUEUE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || mc.entityRenderer == null) {
            QUEUE.clear();
            return;
        }
        Entity view = mc.getRenderViewEntity();
        if (view == null) {
            QUEUE.clear();
            return;
        }

        drawing = true;
        try {
            OpenGlHelper.glUseProgram(0);
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
            GlStateManager.color(1f, 1f, 1f, 1f);

            double camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * (double) partialTicks;
            double camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * (double) partialTicks;
            double camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * (double) partialTicks;

            for (int i = 0; i < QUEUE.size(); i++) {
                QueuedArmor q = QUEUE.get(i);
                if (q.entity == null || q.entity.isDead) {
                    continue;
                }
                try {
                    drawOne(q, camX, camY, camZ, partialTicks);
                } catch (Throwable ignored) {
                }
            }
        } finally {
            drawing = false;
            QUEUE.clear();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            TextureSamplingRegistry.restoreDefaultTexUnit();
            TextureSamplingRegistry.restoreVanillaBlocksAtlasSampling();
        }
    }

    private static void drawOne(
            QueuedArmor q, double camX, double camY, double camZ, float framePartialTicks)
            throws Exception {
        EntityLivingBase entity = q.entity;
        RenderLivingBase<?> renderer = q.renderer;
        float pt = q.partialTicks;

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) pt - camX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) pt - camY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) pt - camZ;

        ModelBase mainModel = getMainModel(renderer);
        if (mainModel == null) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        try {
            float bodyYaw =
                    entity.prevRenderYawOffset
                            + (entity.renderYawOffset - entity.prevRenderYawOffset) * pt;
            float headYaw =
                    entity.prevRotationYawHead
                            + (entity.rotationYawHead - entity.prevRotationYawHead) * pt;
            float pitch =
                    entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * pt;

            GlStateManager.translate((float) x, (float) y, (float) z);
            GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
            if (entity.deathTime > 0) {
                float f = ((float) entity.deathTime + pt - 1.0F) / 20.0F * 1.6F;
                if (f > 1.0F) {
                    f = 1.0F;
                }
                GlStateManager.rotate(f * 90.0F, 0.0F, 0.0F, 1.0F);
            }

            float scaleFactor = 0.0625F;
            GlStateManager.enableRescaleNormal();
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
            GlStateManager.translate(0.0F, -1.501F, 0.0F);

            float limbAmt =
                    entity.prevLimbSwingAmount
                            + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * pt;
            float limb = entity.limbSwing - entity.limbSwingAmount * (1.0F - pt);
            if (entity.isChild()) {
                limb *= 3.0F;
            }
            if (limbAmt > 1.0F) {
                limbAmt = 1.0F;
            }

            mainModel.isChild = entity.isChild();
            mainModel.setLivingAnimations(entity, limb, limbAmt, pt);
            mainModel.setRotationAngles(
                    limb, limbAmt, (float) entity.ticksExisted + pt, headYaw - bodyYaw, pitch, scaleFactor, entity);

            FakeLayerBipedArmor armorLayer = findArmorLayer(renderer);
            if (armorLayer != null) {
                armorLayer.renderTranslucentOnly(
                        entity,
                        q.limbSwing,
                        q.limbSwingAmount,
                        q.partialTicks,
                        q.ageInTicks,
                        q.netHeadYaw,
                        q.headPitch,
                        q.scale);
            }
        } finally {
            GlStateManager.enableCull();
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
        }
    }

    @SuppressWarnings("unchecked")
    private static FakeLayerBipedArmor findArmorLayer(RenderLivingBase<?> renderer) {
        try {
            if (layerRenderersField == null) {
                layerRenderersField =
                        ReflectionHelper.findField(
                                RenderLivingBase.class, "layerRenderers", "field_177097_h");
            }
            List<LayerRenderer<?>> layers = (List<LayerRenderer<?>>) layerRenderersField.get(renderer);
            if (layers == null) {
                return null;
            }
            for (int i = 0; i < layers.size(); i++) {
                LayerRenderer<?> layer = layers.get(i);
                if (layer instanceof FakeLayerBipedArmor) {
                    return (FakeLayerBipedArmor) layer;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ModelBase getMainModel(RenderLivingBase<?> renderer) {
        try {
            if (mainModelField == null) {
                mainModelField =
                        ReflectionHelper.findField(
                                RenderLivingBase.class, "mainModel", "field_77045_g");
            }
            return (ModelBase) mainModelField.get(renderer);
        } catch (Throwable t) {
            return null;
        }
    }

    private static final class QueuedArmor {
        final EntityLivingBase entity;
        final RenderLivingBase<?> renderer;
        final float limbSwing;
        final float limbSwingAmount;
        final float partialTicks;
        final float ageInTicks;
        final float netHeadYaw;
        final float headPitch;
        final float scale;

        QueuedArmor(
                EntityLivingBase entity,
                RenderLivingBase<?> renderer,
                float limbSwing,
                float limbSwingAmount,
                float partialTicks,
                float ageInTicks,
                float netHeadYaw,
                float headPitch,
                float scale) {
            this.entity = entity;
            this.renderer = renderer;
            this.limbSwing = limbSwing;
            this.limbSwingAmount = limbSwingAmount;
            this.partialTicks = partialTicks;
            this.ageInTicks = ageInTicks;
            this.netHeadYaw = netHeadYaw;
            this.headPitch = headPitch;
            this.scale = scale;
        }
    }
}
