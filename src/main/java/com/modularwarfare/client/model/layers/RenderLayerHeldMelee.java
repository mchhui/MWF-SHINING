 package com.modularwarfare.client.model.layers;

import com.modularwarfare.api.RenderHeldItemLayerEvent;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.melee.ItemMelee;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.common.MinecraftForge;

public class RenderLayerHeldMelee extends LayerHeldItem {

    public RenderLayerHeldMelee(RenderLivingBase<?> livingEntityRendererIn) {
        super(livingEntityRendererIn);
         // TODO Auto-generated constructor stub
    }

    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack itemstack = entitylivingbaseIn.getHeldItemMainhand();
        if (itemstack != ItemStack.EMPTY && !itemstack.isEmpty()) {
            if (!(itemstack.getItem() instanceof ItemMelee)) {
                return;
            }
            BaseType type = ((BaseItem)itemstack.getItem()).baseType;
            if (!type.hasModel()) {
                return;
            }
            GlStateManager.pushMatrix();
            if (entitylivingbaseIn.isSneaking()) {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }
            ClientProxy.meleeRenderer.drawThirdMelee(livingEntityRenderer, RenderType.PLAYER, entitylivingbaseIn, itemstack, false);
            GlStateManager.popMatrix();
        }
    }
    
}
