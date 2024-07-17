 package com.modularwarfare.common.network;

import com.modularwarfare.common.playerstate.PlayerState;
import com.modularwarfare.common.playerstate.PlayerStateManager;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketPlayerState extends PacketBase {
    public PlayerState state;
    
    public PacketPlayerState() {
        // TODO Auto-generated constructor stub
    }
    
    public PacketPlayerState(PlayerState state) {
        this.state=state;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeFloat(state.recoilPitchFactor);
        data.writeFloat(state.recoilYawFactor);
        data.writeFloat(state.accuracyFactor);
        data.writeFloat(state.roundsPerMinFactor);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        state=new PlayerState();
        state.recoilPitchFactor=data.readFloat();
        state.recoilYawFactor=data.readFloat();
        state.accuracyFactor=data.readFloat();
        state.roundsPerMinFactor=data.readFloat();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        // TODO Auto-generated method stub
         
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        PlayerStateManager.clientPlayerState=state;
    }

}
