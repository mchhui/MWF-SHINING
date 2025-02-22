package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.GunType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

public class PacketTeslaTrailAskServer extends PacketBase {

    double posX;
    double posY;
    double posZ;
    double targetX;
    double targetY;
    double targetZ;
    float bulletSpeed;
    String gunTypeName;

    public PacketTeslaTrailAskServer() {
    }

    public PacketTeslaTrailAskServer(double startX, double startY, double startZ,
                                    double endX, double endY, double endZ, float bulletSpeed, GunType gunType) {
        this.posX = startX;
        this.posY = startY;
        this.posZ = startZ;
        this.targetX = endX;
        this.targetY = endY;
        this.targetZ = endZ;
        this.bulletSpeed = bulletSpeed;
        this.gunTypeName = gunType != null ? gunType.internalName : "";
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buf = new PacketBuffer(data);
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);
        buf.writeDouble(targetX);
        buf.writeDouble(targetY);
        buf.writeDouble(targetZ);
        buf.writeFloat(bulletSpeed);
        buf.writeString(gunTypeName);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buf = new PacketBuffer(data);
        posX = buf.readDouble();
        posY = buf.readDouble();
        posZ = buf.readDouble();
        targetX = buf.readDouble();
        targetY = buf.readDouble();
        targetZ = buf.readDouble();
        bulletSpeed = buf.readFloat();
        gunTypeName = buf.readString(32767);
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        // 服务器收到请求后,向所有客户端广播特斯拉效果
        ModularWarfare.NETWORK.sendToDimension(
            new PacketTeslaTrail(posX, posY, posZ, targetX, targetY, targetZ, bulletSpeed, gunTypeName),
            entityPlayer.dimension);
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
    }
} 