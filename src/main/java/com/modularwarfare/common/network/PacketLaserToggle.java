package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class PacketLaserToggle extends PacketBase {
    
    private boolean laserEnabled;
    
    public PacketLaserToggle() {}
    
    public PacketLaserToggle(boolean enabled) {
        this.laserEnabled = enabled; 
    }
    
    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeBoolean(laserEnabled);
    }
    
    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.laserEnabled = data.readBoolean();
    }
    
    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        ModularWarfare.NETWORK.sendToAllAround(
            new PacketLaserToggleClient(entityPlayer.getUniqueID(), laserEnabled),
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
        // 客户端不需要处理
    }
}