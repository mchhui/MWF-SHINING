package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.handler.ServerTickHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class PacketAimPoseSync extends PacketBase {

    public float lookYaw;
    public float lookPitch;
    public float bodyYaw;
    public boolean active;

    public PacketAimPoseSync() {
    }

    public PacketAimPoseSync(float lookYaw, float lookPitch, float bodyYaw, boolean active) {
        this.lookYaw = lookYaw;
        this.lookPitch = lookPitch;
        this.bodyYaw = bodyYaw;
        this.active = active;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeFloat(lookYaw);
        data.writeFloat(lookPitch);
        data.writeFloat(bodyYaw);
        data.writeBoolean(active);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        lookYaw = data.readFloat();
        lookPitch = data.readFloat();
        bodyYaw = data.readFloat();
        active = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        if (active) {
            ServerTickHandler.setPlayerAimPose(entityPlayer.getUniqueID(), lookYaw, lookPitch, bodyYaw);
        } else {
            ServerTickHandler.clearPlayerAimPose(entityPlayer.getUniqueID());
        }
        ModularWarfare.NETWORK.sendToAllAround(
            new PacketAimPoseSyncClient(entityPlayer.getUniqueID(), lookYaw, lookPitch, bodyYaw, active),
            new NetworkRegistry.TargetPoint(
                entityPlayer.dimension,
                entityPlayer.posX,
                entityPlayer.posY,
                entityPlayer.posZ,
                128
            )
        );
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
    }
}
