package com.modularwarfare.common.network;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.handler.CommonEventHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;

public class PacketVerification extends PacketBase {

    public boolean usingDirectoryContentPack = false;
    public ArrayList<String> md5List = new ArrayList<String>();

    public PacketVerification() {
        // TODO Auto-generated constructor stub
    }

    public PacketVerification(boolean usingDirectoryContentPack, ArrayList<String> md5List) {
        this.usingDirectoryContentPack = usingDirectoryContentPack;
        this.md5List = md5List;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        buffer.writeBoolean(usingDirectoryContentPack);
        buffer.writeInt(md5List.size());
        for (int i = 0; i < md5List.size(); i++) {
            buffer.writeString(md5List.get(i));
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        this.usingDirectoryContentPack = buffer.readBoolean();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            this.md5List.add(buffer.readString(Short.MAX_VALUE));
        }
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        if (ModConfig.INSTANCE.general.directory_pack_server_kick && usingDirectoryContentPack) {
            playerEntity.connection.disconnect(new TextComponentString(
                    "[ModularWarfare] Kicked for client-side is using directory content-pack."));
        }
        if (!ModConfig.INSTANCE.general.modified_pack_server_kick) {
            return;
        }
        ArrayList<String> serverList = ModularWarfare.contentPackHashList;
        if (!ModConfig.INSTANCE.general.content_pack_hash_list.isEmpty()) {
            serverList = ModConfig.INSTANCE.general.content_pack_hash_list;
        }
        if (serverList.size() == md5List.size()) {
            boolean flag = false;
            for (String hash : serverList) {
                if (!md5List.contains(hash)) {
                    flag = true;
                }
            }
            if (!flag) {
                CommonEventHandler.playerTimeoutMap.remove(playerEntity.getName());
                return;
            }
        }
        playerEntity.connection.disconnect(
                new TextComponentString("[ModularWarfare] Kicked for client-side is using modified content-pack."));
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        ModularWarfare.NETWORK.sendToServer(
                new PacketVerification(ModularWarfare.usingDirectoryContentPack, ModularWarfare.contentPackHashList));
    }

}
