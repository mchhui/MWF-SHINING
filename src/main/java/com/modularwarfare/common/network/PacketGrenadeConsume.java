package com.modularwarfare.common.network;

import com.modularwarfare.common.grenades.ItemGrenade;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public class PacketGrenadeConsume extends PacketBase {

    public PacketGrenadeConsume() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        ItemStack stack = playerEntity.getHeldItemMainhand();
        if (!stack.isEmpty() && stack.getItem() instanceof ItemGrenade) {
            if (!playerEntity.capabilities.isCreativeMode) {
                stack.shrink(1);
                if (stack.getCount() <= 0) {
                    playerEntity.inventory.setInventorySlotContents(playerEntity.inventory.currentItem, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
} 