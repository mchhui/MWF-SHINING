package com.modularwarfare.common.network;

import com.modularwarfare.client.handler.DecalDrawer;
import com.modularwarfare.client.handler.DecalDrawer.BulletHole;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketBulletHole extends PacketBase {

    private int aliveTime;
    private double posX;
    private double posY;
    private double posZ;
    private double aX;
    private double aY;
    private double aZ;

    public PacketBulletHole() {
    } // Don't delete

    public PacketBulletHole(int aliveTime, double posX, double posY, double posZ, double aX, double aY, double aZ) {
        this.aliveTime = aliveTime;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.aX = aX;
        this.aY = aY;
        this.aZ = aZ;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.aliveTime);
        data.writeDouble(this.posX);
        data.writeDouble(this.posY);
        data.writeDouble(this.posZ);
        data.writeDouble(this.aX);
        data.writeDouble(this.aY);
        data.writeDouble(this.aZ);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.aliveTime = data.readInt();
        this.posX = data.readDouble();
        this.posY = data.readDouble();
        this.posZ = data.readDouble();
        this.aX = data.readDouble();
        this.aY = data.readDouble();
        this.aZ = data.readDouble();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {

    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer entityPlayer) {
        BulletHole hole = new BulletHole(this.aliveTime, this.posX, this.posY, this.posZ, this.aX, this.aY, this.aZ);
        DecalDrawer.bulletHoles.add(hole);
    }

}
