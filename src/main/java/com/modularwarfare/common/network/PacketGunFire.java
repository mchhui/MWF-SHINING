package com.modularwarfare.common.network;

import java.util.ArrayList;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponFireMode;
import com.modularwarfare.common.guns.manager.FireManager;
import com.modularwarfare.common.guns.manager.FireManager.FireData;
import com.modularwarfare.common.network.PacketOtherShooterAnimation.AnimationType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketGunFire extends PacketBase {

    public String internalname;
    public float rotationPitch;
    public float rotationYaw;

    public ArrayList<Hit> clientSuggetions = null;

    public static class Hit {
        // Entity
        public int victimEntityId = -1;
        public String hitboxType = "";
        public float remainingPenetrate;
        public float remainingBlockPenetrate;
        public double distance;

        // Block
        public double hitX;
        public double hitY;
        public double hitZ;
        public EnumFacing facing;
    }

    public PacketGunFire() {} // Don't delete

    public PacketGunFire(String internalname, float rotationPitch, float rotationYaw, ArrayList clientSuggetions) {
        this.internalname = internalname;
        this.rotationPitch = rotationPitch;
        this.rotationYaw = rotationYaw;
        this.clientSuggetions = clientSuggetions;
    }

    public PacketGunFire(String internalname, float rotationPitch, float rotationYaw) {
        this.internalname = internalname;
        this.rotationPitch = rotationPitch;
        this.rotationYaw = rotationYaw;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        ByteBufUtils.writeUTF8String(data, this.internalname);
        data.writeFloat(this.rotationPitch);
        data.writeFloat(this.rotationYaw);
        if (this.clientSuggetions != null) {
            data.writeInt(this.clientSuggetions.size());
            for (Hit hit : this.clientSuggetions) {
                data.writeInt(hit.victimEntityId);
                ByteBufUtils.writeUTF8String(data, hit.hitboxType);
                data.writeFloat(hit.remainingPenetrate);
                data.writeFloat(hit.remainingBlockPenetrate);
                data.writeDouble(hit.distance);
                data.writeDouble(hit.hitX);
                data.writeDouble(hit.hitY);
                data.writeDouble(hit.hitZ);
                if (hit.facing != null) {
                    data.writeInt(hit.facing.getIndex());
                } else {
                    data.writeInt(-1);
                }
            }
        } else {
            data.writeInt(-1);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.internalname = ByteBufUtils.readUTF8String(data);
        this.rotationPitch = data.readFloat();
        this.rotationYaw = data.readFloat();
        int clientSuggetionsSize = data.readInt();
        if (clientSuggetionsSize >= 0) {
            this.clientSuggetions = new ArrayList<>();
            for (int i = 0; i < clientSuggetionsSize; i++) {
                Hit hit = new Hit();
                hit.victimEntityId = data.readInt();
                hit.hitboxType = ByteBufUtils.readUTF8String(data);
                hit.remainingPenetrate = data.readFloat();
                hit.remainingBlockPenetrate = data.readFloat();
                hit.distance = data.readDouble();
                hit.hitX = data.readDouble();
                hit.hitY = data.readDouble();
                hit.hitZ = data.readDouble();
                int facingIndex = data.readInt();
                if (facingIndex != -1) {
                    hit.facing = EnumFacing.values()[facingIndex];
                }
                this.clientSuggetions.add(hit);
            }
        }
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {

        IThreadListener mainThread = (WorldServer)entityPlayer.world;
        mainThread.addScheduledTask(new Runnable() {
            public void run() {
                if (entityPlayer != null) {
                    if (ModularWarfare.gunTypes.get(internalname) != null) {
                        ItemGun itemGun = ModularWarfare.gunTypes.get(internalname);
                        WeaponFireMode fireMode = GunType.getFireMode(entityPlayer.getHeldItemMainhand());
                        FireData fireData = FireData.buildServer(rotationPitch, rotationYaw, entityPlayer.world, entityPlayer.getHeldItemMainhand(), itemGun, fireMode, clientSuggetions);
                        FireManager.fire(entityPlayer, fireData);
                        entityPlayer.sendSlotContents(entityPlayer.inventoryContainer, entityPlayer.inventoryContainer.inventorySlots.size() - 1 - 9 + entityPlayer.inventory.currentItem, entityPlayer.getHeldItemMainhand());
                    }
                }
            }
        });

    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {

    }
}
