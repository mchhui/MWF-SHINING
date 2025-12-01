package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.model.InstantBulletTeslaRender;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.utility.vector.Vector3f;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

public class PacketTeslaTrail extends PacketBase {

    double posX;
    double posY;
    double posZ;
    double targetX;
    double targetY;
    double targetZ;
    float bulletSpeed;
    String gunTypeName;

    public PacketTeslaTrail() {
    }

    public PacketTeslaTrail(double startX, double startY, double startZ, 
                           double endX, double endY, double endZ, float bulletSpeed, String gunTypeName) {
        this.posX = startX;
        this.posY = startY;
        this.posZ = startZ;
        this.targetX = endX;
        this.targetY = endY;
        this.targetZ = endZ;
        this.bulletSpeed = bulletSpeed;
        this.gunTypeName = gunTypeName;
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
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        Vector3f origin = new Vector3f((float)posX, (float)posY, (float)posZ);
        Vector3f target = new Vector3f((float)targetX, (float)targetY, (float)targetZ);
        
        // 获取GunType
        GunType gunType = null;
        if(gunTypeName != null && !gunTypeName.isEmpty()) {
            if(ModularWarfare.gunTypes.containsKey(gunTypeName)) {
                gunType = ((ItemGun)ModularWarfare.gunTypes.get(gunTypeName)).type;
            }
        }
        
        InstantBulletTeslaRender.AddTeslaTrail(new InstantBulletTeslaRender.TeslaTrail(origin, target, bulletSpeed, gunType));
    }
} 