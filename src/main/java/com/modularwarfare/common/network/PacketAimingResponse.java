package com.modularwarfare.common.network;

import com.modularwarfare.api.AnimationUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketAimingResponse extends PacketBase {

    public UUID entityUniqueID;
    public boolean aiming;

    public PacketAimingResponse() {
    } // Don't delete

    public PacketAimingResponse(UUID entityUniqueID, boolean aiming) {
        this.entityUniqueID = entityUniqueID;
        this.aiming = aiming;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        writeUniqueId(data, entityUniqueID);
        data.writeBoolean(aiming);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        entityUniqueID = readUniqueId(data);
        aiming = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        if (aiming) {
            AnimationUtils.isAiming.put(entityUniqueID, aiming);
        } else {
            AnimationUtils.isAiming.remove(entityUniqueID);
        }
    }

}