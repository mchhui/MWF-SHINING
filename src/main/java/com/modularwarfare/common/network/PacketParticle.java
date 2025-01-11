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
    public String modelPath;
    public String texturePath;

    public static enum ParticleType {
        UNKOWN, EXPLOSION, ROCKET
    }

    public PacketParticle() {
    }  // Don't delete

    public PacketParticle(ParticleType particleType, double posX, double posY, double posZ) {
        this(particleType, posX, posY, posZ, null, null);
    }

    public PacketParticle(ParticleType particleType, double posX, double posY, double posZ, String modelPath, String texturePath) {
        this.particleType = particleType;
        if (this.particleType == null) {
            this.particleType = ParticleType.UNKOWN;
        }
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.modelPath = modelPath;
        this.texturePath = texturePath;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(particleType.ordinal());
        data.writeDouble(posX);
        data.writeDouble(posY);
        data.writeDouble(posZ);
        data.writeBoolean(modelPath != null);
        if(modelPath != null) {
            writeUTF(data, modelPath);
        }
        data.writeBoolean(texturePath != null);
        if(texturePath != null) {
            writeUTF(data, texturePath);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.particleType = ParticleType.values()[data.readInt()];
        this.posX = data.readDouble();
        this.posY = data.readDouble();
        this.posZ = data.readDouble();
        if(data.readBoolean()) {
            this.modelPath = readUTF(data);
        }
        if(data.readBoolean()) {
            this.texturePath = readUTF(data);
        }
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        if (this.particleType == ParticleType.EXPLOSION) {
            ModularWarfare.PROXY.spawnExplosionParticle(clientPlayer.world, this.posX, this.posY, this.posZ, this.modelPath, this.texturePath);
        } else if (this.particleType == ParticleType.ROCKET) {
            ModularWarfare.PROXY.spawnRocketParticle(clientPlayer.world, this.posX, this.posY, this.posZ);
        }
    }
}
