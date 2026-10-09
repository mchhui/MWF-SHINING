package siz.addon.modularprops.client.model.layers;

import com.modularwarfare.api.RenderHeldItemLayerEvent;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import siz.addon.modularprops.client.ClientProxy;
import siz.addon.modularprops.client.ClientRenderHooksIntegration;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig;
import siz.addon.modularprops.client.fpp.enhanced.renderers.RenderCustomItemEnhanced;
import siz.addon.modularprops.common.custom.CustomBlockType;
import siz.addon.modularprops.common.custom.CustomItemType;
import siz.addon.modularprops.common.custom.ItemBlockCustom;
import siz.addon.modularprops.common.custom.ItemCustom;

public class RenderLayerHeldCustomItem extends LayerHeldItem {

    public RenderLayerHeldCustomItem(RenderLivingBase<?> livingEntityRendererIn) {
        super(livingEntityRendererIn);
    }

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack itemstack = entitylivingbaseIn.getHeldItemMainhand();
        if (itemstack != ItemStack.EMPTY && !itemstack.isEmpty()) {
            RenderHeldItemLayerEvent event = new RenderHeldItemLayerEvent(itemstack, this, entitylivingbaseIn, partialTicks);
            MinecraftForge.EVENT_BUS.post(event);
            if (itemstack.getItem() instanceof ItemCustom) {
                BaseType type = ((BaseItem)itemstack.getItem()).baseType;
                if (!type.hasModel()) {
                    return;
                }

                GlStateManager.pushMatrix();
                if (entitylivingbaseIn.isSneaking()) {
                    GlStateManager.translate(0.0F, 0.2F, 0.0F);
                }
                if (((CustomItemType)type).animationType == WeaponAnimationType.ENHANCED) {
                    ClientRenderHooksIntegration.customItemRenderer.renderThirdPersonCustomItem(this.livingEntityRenderer, RenderType.PLAYER, entitylivingbaseIn, itemstack, entitylivingbaseIn.isSneaking());
                }
                GlStateManager.popMatrix();
            }else if(itemstack.getItem() instanceof ItemBlockCustom) {
                BaseType type = ((ItemBlockCustom)itemstack.getItem()).type;
                if (!type.hasModel()) {
                    return;
                }
//                System.out.println("test");
                GlStateManager.pushMatrix();
                if (entitylivingbaseIn.isSneaking()) {
                    GlStateManager.translate(0.0F, 0.2F, 0.0F);
                }
                if (((CustomBlockType)type).animationType == WeaponAnimationType.ENHANCED) {
                    ClientRenderHooksIntegration.customBlockRenderer.renderThirdPersonCustomBlock(this.livingEntityRenderer, RenderType.PLAYER, entitylivingbaseIn, itemstack, entitylivingbaseIn.isSneaking());
                }
                GlStateManager.popMatrix();
            }
        }
    }
}

