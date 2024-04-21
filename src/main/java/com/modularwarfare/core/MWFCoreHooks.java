package com.modularwarfare.core;

import com.modularwarfare.ModConfig;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.common.backpacks.BackpackType;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;
import com.modularwarfare.utility.OptifineHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.MWFOptifineShadesHelper;
import net.optifine.shaders.Shaders;

public class MWFCoreHooks {
    public static void onRender0() {
        ClientProxy.scopeUtils.onPreRenderHand0();
    }

    public static void onRender1() {
        ClientProxy.scopeUtils.onPreRenderHand1();
    }

    public static void updateElytra(EntityLivingBase entityLivingBase) {
        boolean flag = entityLivingBase.isElytraFlying();

        if (flag && !entityLivingBase.onGround && !entityLivingBase.isRiding()) {
            ItemStack itemstack = entityLivingBase.getItemStackFromSlot(EntityEquipmentSlot.CHEST);

            if (itemstack.getItem() == Items.ELYTRA && ItemElytra.isUsable(itemstack)) {
                flag = true;

                if (!entityLivingBase.world.isRemote
                    && (EntityHelper.getTicksElytraFlying(entityLivingBase) + 1) % 20 == 0) {
                    itemstack.damageItem(1, entityLivingBase);
                }
            } else if (entityLivingBase.hasCapability(CapabilityExtra.CAPABILITY, null)) {
                final IExtraItemHandler extraSlots = entityLivingBase.getCapability(CapabilityExtra.CAPABILITY, null);
                final ItemStack itemstackBackpack = extraSlots.getStackInSlot(0);

                if (!itemstackBackpack.isEmpty()) {
                    if (itemstackBackpack.getItem() instanceof ItemBackpack) {
                        BackpackType backpack = ((ItemBackpack)itemstackBackpack.getItem()).type;
                        if (entityLivingBase.isElytraFlying() && !entityLivingBase.onGround
                            && !entityLivingBase.isRiding()) {
                            if (backpack.isElytra) {
                                flag = true;
                            }
                        }
                    }
                }
            } else {
                flag = false;
            }
        } else {
            flag = false;
        }

        if (!entityLivingBase.world.isRemote) {
            EntityHelper.setFlag(entityLivingBase, 7, flag);
        }
    }
    
    public static void renderLivingAtForRenderPlayer(EntityLivingBase entityLivingBaseIn, double x, double y, double z) {
        GlStateManager.translate((float)x, (float)y, (float)z);
        if(OptifineHelper.isShadersEnabled()) {
            if(Shaders.isShadowPass&&MWFOptifineShadesHelper.getPreShadowPassThirdPersonView()==0) {
                if (entityLivingBaseIn == Minecraft.getMinecraft().player) {
                    Vec3d vec = new Vec3d(0, 0, -ModConfig.INSTANCE.general.playerShadowOffset);
                    vec = vec.rotateYaw((float)Math.toRadians(-Minecraft.getMinecraft().player.rotationYaw));
                    GlStateManager.translate(vec.x, vec.y, vec.z);
                }      
            }
        }
    }
}
