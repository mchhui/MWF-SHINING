package com.modularwarfare.common.network;

import com.modularwarfare.client.view.AimPoseClientStore;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketAimPoseSyncClient extends PacketBase {

    public UUID playerId;
    public float lookYaw;
    public float lookPitch;
    public float bodyYaw;
    public boolean active;

    public PacketAimPoseSyncClient() {
    }

    public PacketAimPoseSyncClient(UUID playerId, float lookYaw, float lookPitch, float bodyYaw, boolean active) {
        this.playerId = playerId;
        this.lookYaw = lookYaw;
        this.lookPitch = lookPitch;
        this.bodyYaw = bodyYaw;
        this.active = active;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        writeUniqueId(data, playerId);
        data.writeFloat(lookYaw);
        data.writeFloat(lookPitch);
        data.writeFloat(bodyYaw);
        data.writeBoolean(active);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        playerId = readUniqueId(data);
        lookYaw = data.readFloat();
        lookPitch = data.readFloat();
        bodyYaw = data.readFloat();
        active = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            AimPoseClientStore.apply(playerId, lookYaw, lookPitch, bodyYaw, active);
        });
    }
}
