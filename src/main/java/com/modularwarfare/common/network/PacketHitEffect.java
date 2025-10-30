package com.modularwarfare.common.network;
import com.modularwarfare.common.particle.EntityBloodFX;
import com.modularwarfare.common.particle.EntityShotFX;
import com.modularwarfare.common.particle.ParticleExplosion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleBlockDust;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class PacketHitEffect extends PacketBase {

    private double hitX;
    private double hitY;
    private double hitZ;

    public PacketHitEffect() {
    } // Don't delete

    public PacketHitEffect(double x, double y, double z) {
        this.hitX = x;
        this.hitY = y;
        this.hitZ = z;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeDouble(this.hitX);
        data.writeDouble(this.hitY);
        data.writeDouble(this.hitZ);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.hitX = data.readDouble();
        this.hitY = data.readDouble();
        this.hitZ = data.readDouble();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {

    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer entityPlayer) {
        for (int i = 0; i < 5; i++) {
            Particle smoke = new EntityShotFX(Minecraft.getMinecraft().world, hitX, hitY, hitZ, 1.0f * new Random().nextFloat(), 1.0f * new Random().nextFloat(), 1.0f * new Random().nextFloat(), 2.0f * new Random().nextFloat());
            Minecraft.getMinecraft().effectRenderer.addEffect(smoke);
        }
    }

}
