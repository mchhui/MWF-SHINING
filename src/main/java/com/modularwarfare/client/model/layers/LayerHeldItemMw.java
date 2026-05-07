package com.modularwarfare.client.model.layers;

import java.lang.reflect.Method;

import com.modularwarfare.client.OffhandHideHelper;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LayerHeldItemMw extends LayerHeldItem {

    private static final Method RENDER_HELD_ITEM = ReflectionHelper.findMethod(LayerHeldItem.class, "renderHeldItem",
            "func_188358_a", EntityLivingBase.class, ItemStack.class, ItemCameraTransforms.TransformType.class,
            EnumHandSide.class);

    static {
        RENDER_HELD_ITEM.setAccessible(true);
    }

    public LayerHeldItemMw(RenderLivingBase<?> livingEntityRendererIn) {
        super(livingEntityRendererIn);
    }

    private static void renderHeld(LayerHeldItem layer, EntityLivingBase entity, ItemStack stack,
            ItemCameraTransforms.TransformType transform, EnumHandSide side) {
        try {
            RENDER_HELD_ITEM.invoke(layer, entity, stack, transform, side);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    //为什么修改缩放而不是直接发空物品?因为发空物品可能会导致双端数据不同步从而产生NPE

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack main = entitylivingbaseIn.getHeldItemMainhand();
        ItemStack off = entitylivingbaseIn.getHeldItemOffhand();

        if (!main.isEmpty() || !off.isEmpty()) {
            GlStateManager.pushMatrix();

            if (this.livingEntityRenderer.getMainModel().isChild) {
                GlStateManager.translate(0.0F, 0.75F, 0.0F);
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
            }

            if (entitylivingbaseIn.isSneaking()) {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }

            renderHeld(this, entitylivingbaseIn, main, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND,
                    EnumHandSide.RIGHT);

            boolean hideOffhand = entitylivingbaseIn instanceof EntityPlayer
                    && OffhandHideHelper.shouldHideOffhandForMainhand(main) && !off.isEmpty();
            if (hideOffhand) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(0.0F, 0.0F, 0.0F);
            }
            renderHeld(this, entitylivingbaseIn, off, ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND,
                    EnumHandSide.LEFT);
            if (hideOffhand) {
                GlStateManager.popMatrix();
            }

            GlStateManager.popMatrix();
        }
    }
}
