package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.GunType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

public class PacketGunTrailAskServer extends PacketBase {

    double posX;
    double posY;
    double posZ;
    double motionX;
    double motionZ;

    double dirX;
    double dirY;
    double dirZ;
    double range;
    float bulletspeed;

    boolean isPunched;
    
    String gunType;
    String model;
    String tex;
    boolean glow;

    public PacketGunTrailAskServer() {
    }

    public PacketGunTrailAskServer(GunType gunType,String model,String tex,boolean glow,double X, double Y, double Z, double motionX, double motionZ, double x, double y, double z, double range, float bulletspeed, boolean isPunched) {
        this.posX = X;
        this.posY = Y;
        this.posZ = Z;

        this.motionX = motionX;
        this.motionZ = motionZ;

        this.dirX = x;
        this.dirY = y;
        this.dirZ = z;
        this.range = range;
        this.bulletspeed = bulletspeed;
        this.isPunched = isPunched;
        
        this.gunType=gunType.internalName;
        this.model=model;
        this.tex=tex;
        this.glow=glow;
        if(this.model==null) {
            this.model="";
        }
        if(this.tex==null) {
            this.tex="";
        }
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buf=new PacketBuffer(data);
        
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);

        buf.writeDouble(motionX);
        buf.writeDouble(motionZ);

        buf.writeDouble(dirX);
        buf.writeDouble(dirY);
        buf.writeDouble(dirZ);

        buf.writeDouble(range);
        buf.writeFloat(bulletspeed);
        buf.writeBoolean(isPunched);
        
        buf.writeString(gunType);
        buf.writeString(model);
        buf.writeString(tex);
        buf.writeBoolean(glow);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buf=new PacketBuffer(data);
        
        posX = buf.readDouble();
        posY = buf.readDouble();
        posZ = buf.readDouble();

        motionX = buf.readDouble();
        motionZ = buf.readDouble();

        dirX = buf.readDouble();
        dirY = buf.readDouble();
        dirZ = buf.readDouble();

        range = buf.readDouble();
        bulletspeed = buf.readFloat();
        isPunched = buf.readBoolean();
        
        gunType=buf.readString(Short.MAX_VALUE);
        model=buf.readString(Short.MAX_VALUE);
        tex=buf.readString(Short.MAX_VALUE);
        glow=buf.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        ModularWarfare.NETWORK.sendToDimension(new PacketGunTrail(gunType,model,tex,glow,posX, posY, posZ, motionX, motionZ, dirX, dirY, dirZ, range, 10, isPunched), entityPlayer.world.provider.getDimension());
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {

    }

}
