package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.model.InstantBulletRenderer;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.vector.Vector3f;
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

    boolean isPunched;

    String gunType;
    String model;
    String tex;
    boolean glow;

    public PacketGunTrail() {
    } // Don't delete

    public PacketGunTrail(GunType gunType, String model, String tex, boolean glow, double X, double Y, double Z, double motionX, double motionZ, double x, double y, double z, double range, float bulletspeed, boolean isPunched) {
        this(gunType.internalName, model, tex, glow, X, Y, Z, motionX, motionZ, x, y, z, range, bulletspeed, isPunched);
    }

    public PacketGunTrail(String gunType, String model, String tex, boolean glow, double X, double Y, double Z, double motionX, double motionZ, double x, double y, double z, double range, float bulletspeed, boolean isPunched) {
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
        buf.writeBoolean(isPunched);

        buf.writeString(gunType);
        buf.writeString(model);
        buf.writeString(tex);
        buf.writeBoolean(glow);
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
        isPunched = buf.readBoolean();

        gunType = buf.readString(Short.MAX_VALUE);
        model = buf.readString(Short.MAX_VALUE);
        tex = buf.readString(Short.MAX_VALUE);
        glow = buf.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {

    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {

        double dx = this.dirX * this.range;
        double dy = this.dirY * this.range;
        double dz = this.dirZ * this.range;
        final Vector3f vec = new Vector3f((float) posX, (float) posY, (float) posZ);
        InstantBulletRenderer.AddTrail(new InstantBulletRenderer.InstantShotTrail(ModularWarfare.gunTypes.get(gunType).type, model, tex, glow, vec, new Vector3f((float) (vec.x + dx + motionX), (float) (vec.y + dy), (float) (vec.z + dz + motionZ)), this.bulletspeed, this.isPunched));
    }

}
