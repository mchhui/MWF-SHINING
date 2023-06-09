package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.model.ModelGun;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.WeaponAnimationType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

public class PacketOtherPlayerAnimation extends PacketBase {

    public String playerName;
    public AnimationType animationType;

    public String internalname;
    public int fireTickDelay;
    public boolean isFailed;

    public PacketOtherPlayerAnimation() {
        // TODO Auto-generated constructor stub
    }

    public PacketOtherPlayerAnimation(String playerName, AnimationType animationType, String internalname,
            int fireTickDelay, boolean isFailed) {
        this.playerName = playerName;
        this.animationType = animationType;
        this.internalname = internalname;
        this.fireTickDelay = fireTickDelay;
        this.isFailed = isFailed;
    }

    public static enum AnimationType {
        FIRE
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        buffer.writeString(playerName);
        buffer.writeEnumValue(animationType);
        buffer.writeString(internalname);
        buffer.writeInt(fireTickDelay);
        buffer.writeBoolean(isFailed);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        playerName = buffer.readString(Short.MAX_VALUE);
        animationType = buffer.readEnumValue(AnimationType.class);
        internalname=buffer.readString(Short.MAX_VALUE);
        fireTickDelay=buffer.readInt();
        isFailed=buffer.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        EntityPlayer player = Minecraft.getMinecraft().world.getPlayerEntityByName(playerName);
        if (player == null) {
            return;
        }
        if (animationType == AnimationType.FIRE) {
            if (player != null) {
                GunType gunType = ModularWarfare.gunTypes.get(internalname).type;
                if (gunType != null) {
                    if (gunType.animationType == WeaponAnimationType.BASIC) {
                        ClientRenderHooks.getAnimMachine(player).triggerShoot((ModelGun) gunType.model, gunType,
                                fireTickDelay);
                    } else {
                        ClientRenderHooks.getEnhancedAnimMachine(player).triggerShoot(
                                ClientProxy.gunEnhancedRenderer.getController(player,
                                        (GunEnhancedRenderConfig) gunType.enhancedModel.config),
                                (ModelEnhancedGun) gunType.enhancedModel, gunType, fireTickDelay, isFailed);
                    }
                }
            }
        }
    }

}
