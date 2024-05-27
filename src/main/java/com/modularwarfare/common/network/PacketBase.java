package com.modularwarfare.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

public abstract class PacketBase {
    /**
     * Util method for quickly writing strings
     */
    public static void writeUTF(ByteBuf data, String s) {
        ByteBufUtils.writeUTF8String(data, s);
    }

    /**
     * Util method for quickly reading strings
     */
    public static String readUTF(ByteBuf data) {
        return ByteBufUtils.readUTF8String(data);
    }

    public static void writeUniqueId(ByteBuf data, UUID uuid) {
        data.writeLong(uuid.getMostSignificantBits());
        data.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUniqueId(ByteBuf data) {
        return new UUID(data.readLong(), data.readLong());
    }

    /**
     * Encode the packet into a ByteBuf stream. Advanced data handlers can be found at @link{net.minecraftforge.fml.common.network.ByteBufUtils}
     */
    public abstract void encodeInto(ChannelHandlerContext ctx, ByteBuf data);

    /**
     * Decode the packet from a ByteBuf stream. Advanced data handlers can be found at @link{net.minecraftforge.fml.common.network.ByteBufUtils}
     */
    public abstract void decodeInto(ChannelHandlerContext ctx, ByteBuf data);

    /**
     * Handle the packet on server side, post-decoding
     */
    public abstract void handleServerSide(EntityPlayerMP playerEntity);

    /**
     * Handle the packet on client side, post-decoding
     */
    @SideOnly(Side.CLIENT)
    public abstract void handleClientSide(EntityPlayer clientPlayer);
}