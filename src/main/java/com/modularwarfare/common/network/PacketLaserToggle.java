package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.ItemGun;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
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
        ItemStack gunStack = entityPlayer.getHeldItemMainhand();
        if(gunStack.getItem() instanceof ItemGun) {
            ((ItemGun)gunStack.getItem()).setLaserEnabled(gunStack, laserEnabled);
        }
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

        ModularWarfare.NETWORK.sendTo(new PacketLaserToggleClient(entityPlayer.getUniqueID(), laserEnabled), entityPlayer);
    }

    @Override 
    public void handleClientSide(EntityPlayer entityPlayer) {
        ItemStack gunStack = entityPlayer.getHeldItemMainhand();
        if(gunStack.getItem() instanceof ItemGun) {
            ((ItemGun)gunStack.getItem()).setLaserEnabled(gunStack, laserEnabled);
        }
    }
}