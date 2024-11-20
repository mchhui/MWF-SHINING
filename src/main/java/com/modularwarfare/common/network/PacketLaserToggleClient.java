package com.modularwarfare.common.network;

import com.modularwarfare.client.laser.LaserRenderManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketLaserToggleClient extends PacketBase {

    private UUID playerId;
    private boolean laserEnabled;
    
    public PacketLaserToggleClient() {} // Don't delete
    
    public PacketLaserToggleClient(UUID playerId, boolean enabled) {
        this.playerId = playerId;
        this.laserEnabled = enabled;
    }
    
    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeLong(playerId.getMostSignificantBits());
        data.writeLong(playerId.getLeastSignificantBits());
        data.writeBoolean(laserEnabled);
    }
    
    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.playerId = new UUID(data.readLong(), data.readLong());
        this.laserEnabled = data.readBoolean();
    }
    
    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        // UNUSED
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            LaserRenderManager.getInstance().setLaserState(playerId, laserEnabled);
        });
    }
}