package com.modularwarfare.common.network;

import com.modularwarfare.common.guns.GunTransformManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketGunTransform extends PacketBase {

    private String targetGunName;

    public PacketGunTransform() {}

    public PacketGunTransform(String targetGunName) {
        this.targetGunName = targetGunName;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, targetGunName);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        targetGunName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        playerEntity.getServer().addScheduledTask(() -> {
            GunTransformManager.handleTransformOnServer(playerEntity, targetGunName);
        });
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        // 客户端不需要处理
    }
} 