package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketParticle extends PacketBase {
    public ParticleType particleType;
    public double posX;
    public double posY;
    public double posZ;

    public static enum ParticleType {
        UNKOWN, EXPLOSION, ROCKET
    }

    public PacketParticle() {
        // TODO Auto-generated constructor stub
    }

    public PacketParticle(ParticleType particleType, double posX, double posY, double posZ) {
        this.particleType = particleType;
        if (this.particleType == null) {
            this.particleType = ParticleType.UNKOWN;
        }
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(particleType.ordinal());
        data.writeDouble(posX);
        data.writeDouble(posY);
        data.writeDouble(posZ);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.particleType = ParticleType.values()[data.readInt()];
        this.posX = data.readDouble();
        this.posY = data.readDouble();
        this.posZ = data.readDouble();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        if (this.particleType == ParticleType.EXPLOSION) {
            ModularWarfare.PROXY.spawnExplosionParticle(clientPlayer.world, this.posX, this.posY, this.posZ);
        }else if (this.particleType == ParticleType.ROCKET) {
            ModularWarfare.PROXY.spawnRocketParticle(clientPlayer.world, this.posX, this.posY, this.posZ);
        }
    }

}
