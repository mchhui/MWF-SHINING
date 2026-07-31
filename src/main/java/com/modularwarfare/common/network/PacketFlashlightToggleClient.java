package com.modularwarfare.common.network;

import com.modularwarfare.client.flashlight.FlashlightRenderManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketFlashlightToggleClient extends PacketBase {

    private UUID playerId;
    private boolean flashlightEnabled;

    public PacketFlashlightToggleClient() {}

    public PacketFlashlightToggleClient(UUID playerId, boolean enabled) {
        this.playerId = playerId;
        this.flashlightEnabled = enabled;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeLong(playerId.getMostSignificantBits());
        data.writeLong(playerId.getLeastSignificantBits());
        data.writeBoolean(flashlightEnabled);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.playerId = new UUID(data.readLong(), data.readLong());
        this.flashlightEnabled = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        // UNUSED
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            FlashlightRenderManager.getInstance().setFlashlightState(playerId, flashlightEnabled);
        });
    }
}
