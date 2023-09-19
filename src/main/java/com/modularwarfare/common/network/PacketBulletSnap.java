package com.modularwarfare.common.network;

import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.hud.GunUI;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;

public class PacketBulletSnap extends PacketBase {
    public PacketBulletSnap() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {

    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        IThreadListener mainThread = Minecraft.getMinecraft();
        mainThread.addScheduledTask(new Runnable() {
            public void run() {
                GunUI.bulletSnapFade += .25f;
                if (GunUI.bulletSnapFade > 0.8F) {
                    GunUI.bulletSnapFade = 0.8F;
                }
            }
        });
    }

}
