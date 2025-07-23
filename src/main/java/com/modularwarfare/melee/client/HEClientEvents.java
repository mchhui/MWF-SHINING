package com.modularwarfare.melee.client;

import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.loader.api.model.ObjModelRenderer;
import com.modularwarfare.melee.client.configs.AnimationMeleeType;
import com.modularwarfare.melee.client.configs.MeleeRenderConfig;
import com.modularwarfare.melee.common.melee.ItemMelee;
import mchhui.he.api.event.entitydisplay.EasyLivingModelEvent.RenderOverlay;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import java.util.HashSet;

public class HEClientEvents {
    
    @SubscribeEvent
    public void onRenderOverlay(RenderOverlay event) {
        HashSet<String> except = new HashSet<String>();
        except.add("fp_root");

        ItemStack itemstack = event.entity.getHeldItemMainhand();
        if (!(itemstack.getItem() instanceof ItemMelee)) {
            return;
        }
        BaseType type = ((BaseItem) itemstack.getItem()).baseType;
        if (!type.hasModel()) {
            return;
        }

        boolean glowTxtureMode = ObjModelRenderer.glowTxtureMode;
        ObjModelRenderer.glowTxtureMode = true;
        GlStateManager.pushMatrix();
        if (event.model != null && event.model.existNodeState("righthand_mwf_item")) {
            event.model.applyNodeStateTransform("righthand_mwf_item", () -> {

                EnhancedModel model = type.enhancedModel;
                MeleeRenderConfig config = (MeleeRenderConfig) type.enhancedModel.config;
                GlStateManager.pushMatrix();
                if (config.extra.thirdPersonRender3D) {

                    GlStateManager.translate(-0.06, 0.38, -0.02);

                    GL11.glRotatef(-90F, 0F, 1F, 0F);
                    GL11.glRotatef(90F, 0F, 0F, 1F);
                    GL11.glTranslatef(0.25F, 0.2F, -0.05F);
                    GL11.glScalef(1 / 16F, 1 / 16F, 1 / 16F);

                    GL11.glRotatef(config.extra.thirdPersonRotation.x, 1F, 0F, 0F);
                    GL11.glRotatef(config.extra.thirdPersonRotation.y, 0F, 1F, 0F);
                    GL11.glRotatef(config.extra.thirdPersonRotation.z, 0F, 0F, 1F);

                    GL11.glTranslatef(config.extra.thirdPersonOffset.x, config.extra.thirdPersonOffset.y,
                            config.extra.thirdPersonOffset.z);

                    GL11.glScalef(config.extra.thirdPersonScale, config.extra.thirdPersonScale,
                            config.extra.thirdPersonScale);

                    model.updateAnimation(
                            (float) config.meleeAnimations.get(AnimationMeleeType.DEFAULT).get(0).getStartTime(config.FPS));

                    int skinId = 0;
                    if (itemstack.hasTagCompound()) {
                        if (itemstack.getTagCompound().hasKey("skinId")) {
                            skinId = itemstack.getTagCompound().getInteger("skinId");
                        }
                    }
                    String path = skinId > 0 ? type.modelSkins[skinId].getSkin() : type.modelSkins[0].getSkin();
                    RenderMelee meleeRender = ClientProxy.meleeRenderer;
                    meleeRender.bindTexture("melee", path);

                    ObjModelRenderer.glowTxtureMode = true;

                    model.renderPartExcept(RenderParameters.partsWithAmmo);

                    ObjModelRenderer.glowTxtureMode = glowTxtureMode;
                }
                GlStateManager.popMatrix();

            });
        }
        GlStateManager.popMatrix();
        ObjModelRenderer.glowTxtureMode = glowTxtureMode;
    }
} 