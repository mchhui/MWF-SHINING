package com.modularwarfare.client.model.layers;

import com.modularwarfare.api.RenderHeldItemLayerEvent;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.common.MinecraftForge;

public class RenderLayerHeldGun extends LayerHeldItem {

    public RenderLayerHeldGun(RenderLivingBase<?> livingEntityRendererIn) {
        super(livingEntityRendererIn);
    }

    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack itemstack = entitylivingbaseIn.getHeldItemMainhand();
        if (itemstack != ItemStack.EMPTY && !itemstack.isEmpty()) {

            RenderHeldItemLayerEvent event = new RenderHeldItemLayerEvent(itemstack, this, entitylivingbaseIn, partialTicks);
            MinecraftForge.EVENT_BUS.post(event);

            if (!(itemstack.getItem() instanceof ItemGun)) {
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

            if (((GunType) type).animationType == WeaponAnimationType.BASIC) {
                this.translateToHand(EnumHandSide.RIGHT);
                GlStateManager.translate(-0.06, 0.38, -0.02);
                if (ClientRenderHooks.customRenderers[type.id] != null) {
                    ClientRenderHooks.customRenderers[type.id].renderItem(CustomItemRenderType.EQUIPPED, null, itemstack,
                            entitylivingbaseIn.world, entitylivingbaseIn, partialTicks);
                }
            } else if (((GunType) type).animationType == WeaponAnimationType.ENHANCED) {

                GunType gunType = (GunType) type;
                EnhancedModel model = type.enhancedModel;

                GunEnhancedRenderConfig config = (GunEnhancedRenderConfig) gunType.enhancedModel.config;
                ClientProxy.gunEnhancedRenderer.drawThirdGun(livingEntityRenderer,RenderType.PLAYER, entitylivingbaseIn, itemstack);

            }

            GlStateManager.popMatrix();

        }
    }
}