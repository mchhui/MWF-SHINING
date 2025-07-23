package com.modularwarfare.melee.comon;

import com.modularwarfare.common.network.PacketBase;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mchhui.he.api.ELMAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.Loader;

public class PacketBounced extends PacketBase {

    public String ani;

    public PacketBounced() {
        // TODO Auto-generated constructor stub
    }

    public PacketBounced(String ani) {
        this.ani = ani;
    }

    @Override
    public void decodeInto(ChannelHandlerContext arg0, ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        ani = buffer.readString(Short.MAX_VALUE);
    }

    @Override
    public void encodeInto(ChannelHandlerContext arg0, ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        buffer.writeString(ani);

    }

    @Override
    public void handleClientSide(EntityPlayer arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        if (Loader.isModLoaded("hueihueaengine")) {
            ELMAPI.playAni(playerEntity.getUniqueID(), ani, 5);
        }
    }

}
