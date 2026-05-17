package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.trail.TrailOriginResolver;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

public class PacketGunTrail extends PacketBase {

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

    String gunType;
    String model;
    String tex;
    boolean glow;
    int shooterEntityId;

    public PacketGunTrail() {
    } // Don't delete

    public PacketGunTrail(int shooterEntityId, String gunType, String model, String tex, boolean glow, double X,
            double Y, double Z, double motionX, double motionZ, double x, double y, double z, double range,
            float bulletspeed) {
        this.shooterEntityId = shooterEntityId;
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
        this.gunType = gunType;
        this.model = model;
        this.tex = tex;
        this.glow = glow;
        if (this.model == null) {
            this.model = "";
        }
        if (this.tex == null) {
            this.tex = "";
        }
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buf = new PacketBuffer(data);
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

        buf.writeString(gunType);
        buf.writeString(model);
        buf.writeString(tex);
        buf.writeBoolean(glow);
        buf.writeInt(shooterEntityId);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buf = new PacketBuffer(data);
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

        gunType = buf.readString(Short.MAX_VALUE);
        model = buf.readString(Short.MAX_VALUE);
        tex = buf.readString(Short.MAX_VALUE);
        glow = buf.readBoolean();
        shooterEntityId = buf.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {

    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        if (TrailOriginResolver.shouldIgnoreServerTrailForLocalShooter(shooterEntityId)) {
            return;
        }
        TrailOriginResolver.queueGunTrail(shooterEntityId, gunType, model, tex, glow, posX, posY, posZ, motionX,
                motionZ, dirX, dirY, dirZ, range, bulletspeed);
    }

}
