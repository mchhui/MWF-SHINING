package com.modularwarfare.common.network;

import com.modularwarfare.ModConfig;
import com.modularwarfare.client.chest.ClientChestGuiSettings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;

public class PacketSyncCustomChestGui extends PacketBase {

    private boolean enable;
    private String filterMode;
    private final List<String> filters = new ArrayList<>();

    public PacketSyncCustomChestGui() {
    }

    public static PacketSyncCustomChestGui fromServerConfig() {
        final PacketSyncCustomChestGui packet = new PacketSyncCustomChestGui();
        final ModConfig.CustomChestGui config = ModConfig.INSTANCE.customChestGui;
        packet.enable = config.enable;
        packet.filterMode = config.filterMode;
        if (config.filters != null) {
            for (final ModConfig.CustomChestGui.ChestGuiFilter filter : config.filters) {
                if (filter != null && filter.guiName != null) {
                    packet.filters.add(filter.guiName);
                }
            }
        }
        return packet;
    }

    @Override
    public void encodeInto(final ChannelHandlerContext ctx, final ByteBuf data) {
        data.writeBoolean(this.enable);
        writeUTF(data, this.filterMode != null ? this.filterMode : "whitelist");
        data.writeShort(this.filters.size());
        for (final String filter : this.filters) {
            writeUTF(data, filter);
        }
    }

    @Override
    public void decodeInto(final ChannelHandlerContext ctx, final ByteBuf data) {
        this.enable = data.readBoolean();
        this.filterMode = readUTF(data);
        this.filters.clear();
        final int count = data.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            this.filters.add(readUTF(data));
        }
    }

    @Override
    public void handleServerSide(final EntityPlayerMP playerEntity) {
    }

    @Override
    public void handleClientSide(final EntityPlayer clientPlayer) {
        Minecraft.getMinecraft().addScheduledTask(() ->
                ClientChestGuiSettings.applyFromServer(this.enable, this.filterMode, this.filters));
    }
}
