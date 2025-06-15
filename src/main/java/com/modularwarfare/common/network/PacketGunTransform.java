package com.modularwarfare.common.network;

import java.util.UUID;

import com.modularwarfare.common.guns.GunTransformManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketGunTransform extends PacketBase {

    private String targetGunName;
    private UUID versionID;

    public PacketGunTransform() {}

    public PacketGunTransform(String targetGunName,UUID versionID) {
        this.targetGunName = targetGunName;
        this.versionID=versionID;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        PacketBuffer buffer=new PacketBuffer(buf);
        buffer.writeString(this.targetGunName);
        buffer.writeUniqueId(this.versionID);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        PacketBuffer buffer=new PacketBuffer(buf);
        this.targetGunName =buffer.readString(Short.MAX_VALUE);
        this.versionID=buffer.readUniqueId();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        playerEntity.getServer().addScheduledTask(() -> {
            GunTransformManager.handleTransformOnServer(playerEntity, this.targetGunName,this.versionID);
        });
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        // 客户端不需要处理
    }
} 