package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.ItemGun;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class PacketFlashlightToggle extends PacketBase {

    private boolean flashlightEnabled;

    public PacketFlashlightToggle() {}

    public PacketFlashlightToggle(boolean enabled) {
        this.flashlightEnabled = enabled;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeBoolean(flashlightEnabled);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.flashlightEnabled = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        ItemStack gunStack = entityPlayer.getHeldItemMainhand();
        if (gunStack.getItem() instanceof ItemGun) {
            ((ItemGun) gunStack.getItem()).setFlashlightEnabled(gunStack, flashlightEnabled);
        }
        ModularWarfare.NETWORK.sendToAllAround(
                new PacketFlashlightToggleClient(entityPlayer.getUniqueID(), flashlightEnabled),
                new NetworkRegistry.TargetPoint(
                        entityPlayer.dimension,
                        entityPlayer.posX,
                        entityPlayer.posY,
                        entityPlayer.posZ,
                        128));
        ModularWarfare.NETWORK.sendTo(
                new PacketFlashlightToggleClient(entityPlayer.getUniqueID(), flashlightEnabled), entityPlayer);
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        ItemStack gunStack = entityPlayer.getHeldItemMainhand();
        if (gunStack.getItem() instanceof ItemGun) {
            ((ItemGun) gunStack.getItem()).setFlashlightEnabled(gunStack, flashlightEnabled);
        }
    }
}
