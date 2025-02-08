package com.modularwarfare.client.model.layers;

import com.modularwarfare.api.RenderHeldItemLayerEvent;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.enhanced.configs.GrenadeEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGrenadeEnhanced;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.common.MinecraftForge;

public class RenderLayerHeldGrenade extends LayerHeldItem {

    public RenderLayerHeldGrenade(RenderLivingBase<?> livingEntityRendererIn) {
        super(livingEntityRendererIn);
    }

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount,
                             float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack itemstack = entitylivingbaseIn.getHeldItemMainhand();
        if (itemstack != ItemStack.EMPTY && !itemstack.isEmpty()) {

            RenderHeldItemLayerEvent event = new RenderHeldItemLayerEvent(itemstack, this, entitylivingbaseIn, partialTicks);
            MinecraftForge.EVENT_BUS.post(event);

            if (!(itemstack.getItem() instanceof ItemGrenade)) {
                return;
            }
            BaseType type = ((BaseItem) itemstack.getItem()).baseType;
            if (!type.hasModel()) {
                return;
            }

            GlStateManager.pushMatrix();
            if (entitylivingbaseIn.isSneaking()) {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }

            if(((GrenadeType)type).animationType == WeaponAnimationType.BASIC) {
                this.translateToHand(EnumHandSide.RIGHT);
                GlStateManager.translate(-0.06, 0.38, -0.02);
                if (ClientRenderHooks.customRenderers[type.id] != null) {
                    ClientRenderHooks.customRenderers[type.id].renderItem(CustomItemRenderType.EQUIPPED, null, itemstack,
                            entitylivingbaseIn.world, entitylivingbaseIn, partialTicks);
                }
            } else if(((GrenadeType)type).animationType == WeaponAnimationType.ENHANCED) {

                GrenadeType grenadeType = (GrenadeType) type;
                EnhancedModel model = type.enhancedModel;

                GrenadeEnhancedRenderConfig config = (GrenadeEnhancedRenderConfig) grenadeType.enhancedModel.config;
                
                ClientProxy.grenadeEnhancedRenderer.renderThirdPersonGrenade(livingEntityRenderer, RenderType.PLAYER, entitylivingbaseIn, itemstack, entitylivingbaseIn.isSneaking());

            }

            GlStateManager.popMatrix();
        }
    }
} 