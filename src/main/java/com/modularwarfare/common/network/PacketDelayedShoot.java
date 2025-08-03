package com.modularwarfare.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 延迟射击网络包
 * 用于在客户端渲染红色射线，并在延迟结束后执行射击
 */
public class PacketDelayedShoot extends PacketBase {

    public int entityId;
    public int targetEntityId; // 目标实体ID，-1表示射击坐标
    public double targetX, targetY, targetZ; // 目标坐标（当targetEntityId为-1时使用）
    public float offsetX, offsetY, offsetZ; // 射线起点偏移
    public int delayTicks; // 延迟时间（tick）
    public boolean isCoordinateShoot; // 是否为坐标射击

    public PacketDelayedShoot() {
    } // Don't delete

    public PacketDelayedShoot(int entityId, int targetEntityId, double targetX, double targetY, double targetZ, 
                             float offsetX, float offsetY, float offsetZ, int delayTicks, boolean isCoordinateShoot) {
        this.entityId = entityId;
        this.targetEntityId = targetEntityId;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.delayTicks = delayTicks;
        this.isCoordinateShoot = isCoordinateShoot;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.entityId);
        data.writeInt(this.targetEntityId);
        data.writeDouble(this.targetX);
        data.writeDouble(this.targetY);
        data.writeDouble(this.targetZ);
        data.writeFloat(this.offsetX);
        data.writeFloat(this.offsetY);
        data.writeFloat(this.offsetZ);
        data.writeInt(this.delayTicks);
        data.writeBoolean(this.isCoordinateShoot);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.entityId = data.readInt();
        this.targetEntityId = data.readInt();
        this.targetX = data.readDouble();
        this.targetY = data.readDouble();
        this.targetZ = data.readDouble();
        this.offsetX = data.readFloat();
        this.offsetY = data.readFloat();
        this.offsetZ = data.readFloat();
        this.delayTicks = data.readInt();
        this.isCoordinateShoot = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        // 服务端不需要处理，这个包主要用于客户端渲染
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer entityPlayer) {
        // 在客户端渲染射线并处理延迟射击
        // 这里将在客户端渲染系统中实现
    }
} 