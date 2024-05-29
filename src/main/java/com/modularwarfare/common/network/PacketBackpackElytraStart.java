package com.modularwarfare.common.network;

import com.modularwarfare.common.backpacks.BackpackType;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;

public class PacketBackpackElytraStart extends PacketBase {

    public PacketBackpackElytraStart() {
    } // Don't delete

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        ItemStack itemstack = playerEntity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);

        if (itemstack.getItem() == Items.ELYTRA && ItemElytra.isUsable(itemstack)) {
            if (itemstack.getItem() == Items.ELYTRA && ItemElytra.isUsable(itemstack)) {
                return;
            }
        }
        if (playerEntity.hasCapability(CapabilityExtra.CAPABILITY, null)) {
            final IExtraItemHandler extraSlots = playerEntity.getCapability(CapabilityExtra.CAPABILITY, null);
            final ItemStack itemstackBackpack = extraSlots.getStackInSlot(0);

            if (!itemstackBackpack.isEmpty()) {
                if (itemstackBackpack.getItem() instanceof ItemBackpack) {
                    BackpackType backpack = ((ItemBackpack) itemstackBackpack.getItem()).type;
                    if (backpack.isElytra) {
                        if (!playerEntity.onGround && playerEntity.motionY < 0.0D && !playerEntity.isElytraFlying() && !playerEntity.isInWater()) {
                            playerEntity.setElytraFlying();
                        } else if (backpack.elytraStoppable) {
                            playerEntity.clearElytraFlying();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        // TODO Auto-generated method stub

    }

}
