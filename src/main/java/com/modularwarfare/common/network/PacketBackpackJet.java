package com.modularwarfare.common.network;

import java.lang.reflect.Field;
import java.util.UUID;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.AnimationUtils;
import com.modularwarfare.common.backpacks.BackpackType;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;
import com.modularwarfare.common.guns.WeaponSoundType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

public class PacketBackpackJet extends PacketBase {
    public UUID playerEntityUniqueID;
    public boolean jetFire;
    public int jetDuraton = 100;
    public static Field fieldFloatingTickCount;

    public PacketBackpackJet() {
        // TODO Auto-generated constructor stub
    }

    public PacketBackpackJet(boolean jetFire, UUID playerEntityUniqueID) {
        this.jetFire = jetFire;
        this.playerEntityUniqueID = playerEntityUniqueID;
    }

    public PacketBackpackJet(UUID playerEntityUniqueID, int jetDuraton) {
        this.playerEntityUniqueID = playerEntityUniqueID;
        this.jetDuraton = jetDuraton;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buf = new PacketBuffer(data);
        buf.writeUniqueId(playerEntityUniqueID);
        buf.writeBoolean(jetFire);
        buf.writeInt(jetDuraton);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer buf = new PacketBuffer(data);
        playerEntityUniqueID = buf.readUniqueId();
        jetFire = buf.readBoolean();
        jetDuraton = buf.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        playerEntity.fallDistance = 0;
        playerEntity.handleFalling(jetDuraton, jetFire);
        if (playerEntity.hasCapability(CapabilityExtra.CAPABILITY, null)) {
            final IExtraItemHandler extraSlots = playerEntity.getCapability(CapabilityExtra.CAPABILITY, null);
            final ItemStack itemstackBackpack = extraSlots.getStackInSlot(0);

            if (!itemstackBackpack.isEmpty()) {
                if (itemstackBackpack.getItem() instanceof ItemBackpack) {
                    BackpackType backpack = ((ItemBackpack)itemstackBackpack.getItem()).type;
                    if (!jetFire) {
                        ModularWarfare.NETWORK.sendToAllTracking(new PacketBackpackJet(playerEntity.getUniqueID(), 100),
                            playerEntity);
                    } else {
                        ModularWarfare.NETWORK.sendToAllTracking(
                            new PacketBackpackJet(playerEntity.getUniqueID(), backpack.jetElytraBoostDuration * 50),
                            playerEntity);
                    }
                    if (backpack.isJet && backpack.weaponSoundMap != null) {
                        backpack.playSoundPos(playerEntity.getPosition(), playerEntity.world, WeaponSoundType.JetWork);
                        if (jetFire) {
                            backpack.playSoundPos(playerEntity.getPosition(), playerEntity.world,
                                WeaponSoundType.JetFire);
                        } else {
                            ((WorldServer)playerEntity.world).spawnParticle(EnumParticleTypes.BLOCK_DUST,
                                playerEntity.posX, playerEntity.posY, playerEntity.posZ, 5, 0.0D, 0.0D, 0.0D,
                                0.15000000596046448D, Block.getStateId(playerEntity.world.getBlockState(playerEntity.getPosition().down())));
                            ((WorldServer)playerEntity.world).spawnParticle(EnumParticleTypes.BLOCK_DUST,
                                playerEntity.posX, playerEntity.posY, playerEntity.posZ, 5, 0.0D, 0.0D, 0.0D,
                                0.15000000596046448D, Block.getStateId(playerEntity.world.getBlockState(playerEntity.getPosition().down(2))));
                        }
                    }
                }
            }
        }
        if (fieldFloatingTickCount == null) {
            Class clz = NetHandlerPlayServer.class;
            try {
                fieldFloatingTickCount = clz.getDeclaredField("field_147365_f");
            } catch (NoSuchFieldException | SecurityException e) {
                try {
                    fieldFloatingTickCount = clz.getDeclaredField("floatingTickCount");
                } catch (NoSuchFieldException | SecurityException e1) {
                    e1.printStackTrace();
                }
            }
            if (fieldFloatingTickCount != null) {
                fieldFloatingTickCount.setAccessible(true);
            }
        }
        try {
            fieldFloatingTickCount.set(playerEntity.connection, 0);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        AnimationUtils.isJet.put(playerEntityUniqueID, System.currentTimeMillis() + jetDuraton);
    }

}
