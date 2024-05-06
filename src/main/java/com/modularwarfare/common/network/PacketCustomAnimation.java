package com.modularwarfare.common.network;

import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.common.entity.decals.EntityBulletHole;
import com.modularwarfare.common.entity.decals.EntityDecal;
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
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;
import java.util.UUID;

public class PacketCustomAnimation extends PacketBase {

    public UUID living;
    public String name;
    public double startTime;
    public double endTime;
    public float speedFactor;
    public boolean allowReload;
    public boolean allowFire;

    public PacketCustomAnimation() {
    }



    public PacketCustomAnimation(UUID living,String name, double startTime,double endTime, float speedFactor, boolean allowReload,
        boolean allowFire) {
        this.living = living;
        this.name=""+name;
        this.startTime=startTime;
        this.endTime=endTime;
        this.speedFactor = speedFactor;
        this.allowReload = allowReload;
        this.allowFire = allowFire;
    }



    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buffer=new PacketBuffer(data);
        buffer.writeUniqueId(living);
        buffer.writeString(name);
        buffer.writeDouble(startTime);
        buffer.writeDouble(endTime);
        buffer.writeFloat(speedFactor);
        buffer.writeBoolean(allowReload);
        buffer.writeBoolean(allowFire);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buffer=new PacketBuffer(data);
        living=buffer.readUniqueId();
        name=buffer.readString(Short.MAX_VALUE);
        startTime=buffer.readDouble();
        endTime=buffer.readDouble();
        speedFactor=buffer.readFloat();
        allowReload=buffer.readBoolean();
        allowFire=buffer.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {

    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer entityPlayer) {
        AnimationController controller=ClientProxy.gunEnhancedRenderer.getController(Minecraft.getMinecraft().world.getPlayerEntityByUUID(living), null);
        controller.CUSTOM=0;
        controller.customAnimation=name;
        controller.startTime=startTime;
        controller.endTime=endTime;
        controller.customAnimationSpeed=speedFactor;
        controller.customAnimationReload=allowReload;
        controller.customAnimationFire=allowFire;
//        System.out.println("test");
    }

}