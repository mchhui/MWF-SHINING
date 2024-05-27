package com.modularwarfare.common.network;

import com.modularwarfare.common.handler.ServerTickHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketAimingRequest extends PacketBase {

    public boolean aiming;

    public PacketAimingRequest(boolean aiming) {
        this.aiming = aiming;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeBoolean(aiming);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        aiming = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        ServerTickHandler.playerAimInstant.put(entityPlayer.getUniqueID(), aiming);
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {

    }

}