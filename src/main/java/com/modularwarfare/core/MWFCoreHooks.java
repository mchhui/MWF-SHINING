package com.modularwarfare.core;

import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.common.backpacks.BackpackType;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;

import net.minecraft.entity.EntityHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;

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
                            backpack.isElytra = true;
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
}
