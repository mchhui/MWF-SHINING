package com.modularwarfare.common.network;

import com.modularwarfare.ModConfig;
import com.modularwarfare.client.hud.GunUI;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;

public class PacketPlayerHit extends PacketBase {


    public PacketPlayerHit() {
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
        mainThread.addScheduledTask(() -> {
            if (!ModConfig.INSTANCE.hud.snap_fade_hit) {
                return;
            }

            if (!(Minecraft.getMinecraft().player.getHealth() > 0.0f)) {
                return;
            }

            /**
             * playerRecoilPitch RECOIL DAMAGE
             */
            //RenderParameters.playerRecoilPitch += 5F;
            //RenderParameters.playerRecoilYaw += new Random().nextFloat();

            GunUI.bulletSnapFade += .25f;
            GunUI.bulletSnapFade = Math.min(GunUI.bulletSnapFade, 0.9F);
        });
    }

}
