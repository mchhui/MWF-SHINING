package com.modularwarfare.common.grenades;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.entity.grenades.EntitySmokeGrenade;
import com.modularwarfare.common.entity.grenades.EntityStunGrenade;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.init.ModSounds;
import com.modularwarfare.common.network.PacketBase;
import com.modularwarfare.common.guns.WeaponSoundType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

public class PacketGrenadeThrow extends PacketBase {
    
    private boolean isLowThrow;
    private float remainingFuseTime;
    private float throwStrength = 1.0f;

    public PacketGrenadeThrow() {}

    public PacketGrenadeThrow(boolean isLowThrow) {
        this(isLowThrow, -1, 1.0f);
    }

    public PacketGrenadeThrow(boolean isLowThrow, float remainingFuseTime) {
        this(isLowThrow, remainingFuseTime, 1.0f);
    }

    public PacketGrenadeThrow(boolean isLowThrow, float remainingFuseTime, float throwStrength) {
        this.isLowThrow = isLowThrow;
        this.remainingFuseTime = remainingFuseTime;
        this.throwStrength = throwStrength;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        buf.writeBoolean(isLowThrow);
        buf.writeFloat(remainingFuseTime);
        buf.writeFloat(throwStrength);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        this.isLowThrow = buf.readBoolean();
        this.remainingFuseTime = buf.readFloat();
        this.throwStrength = buf.readFloat();
    }

    @Override
    public void handleClientSide(EntityPlayer player) {
    }

    @Override
    public void handleServerSide(EntityPlayerMP player) {
        ItemStack stack = player.getHeldItemMainhand();
        if (stack.getItem() instanceof ItemGrenade) {
            GrenadeType type = ((ItemGrenade) stack.getItem()).type;
            
            float actualStrength = throwStrength * (isLowThrow ? type.throwStrengthLow : type.throwStrength);
            
            Vec3d throwVec;
            if (!isLowThrow && type.animationType == WeaponAnimationType.ENHANCED) {
                float pitch = player.rotationPitch - 15;
                float yaw = player.rotationYaw;
                float f = 0.017453292F;
                
                double x = -Math.sin(yaw * f) * Math.cos(pitch * f);
                double y = -Math.sin(pitch * f);
                double z = Math.cos(yaw * f) * Math.cos(pitch * f);
                
                throwVec = new Vec3d(x, y, z);
            } else {
                throwVec = player.getLookVec();
            }
            
            double modifier = player.isSprinting() ? 1.25 : 1.0;
            
            double motionX = ((throwVec.x * 1.5) * modifier) * actualStrength;
            double motionY = ((throwVec.y * 1.5) * modifier) * actualStrength;
            double motionZ = ((throwVec.z * 1.5) * modifier) * actualStrength;
            
            switch (type.grenadeType) {
                case Frag:
                    EntityGrenade grenade = new EntityGrenade(player.world, player, actualStrength, type);
                    if(remainingFuseTime > 0) {
                        grenade.fuse = (int)(remainingFuseTime * 20);
                    }
                    grenade.motionX = motionX;
                    grenade.motionY = motionY;
                    grenade.motionZ = motionZ;
                    player.world.playSound(null, player.posX, player.posY, player.posZ, ModSounds.GRENADE_THROW, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    player.world.spawnEntity(grenade);
                    break;
                case Smoke:
                    EntitySmokeGrenade smoke = new EntitySmokeGrenade(player.world, player, actualStrength, type);
                    if(remainingFuseTime > 0) {
                        smoke.fuse = (int)(remainingFuseTime * 20);
                    }
                    smoke.motionX = motionX;
                    smoke.motionY = motionY;
                    smoke.motionZ = motionZ;
                    player.world.playSound(null, player.posX, player.posY, player.posZ, ModSounds.GRENADE_THROW, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    player.world.spawnEntity(smoke);
                    break;
                case Stun:
                    EntityStunGrenade stun = new EntityStunGrenade(player.world, player, actualStrength, type);
                    if(remainingFuseTime > 0) {
                        stun.fuse = (int)(remainingFuseTime * 20);
                    }
                    stun.motionX = motionX;
                    stun.motionY = motionY;
                    stun.motionZ = motionZ;
                    player.world.playSound(null, player.posX, player.posY, player.posZ, ModSounds.GRENADE_THROW, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    player.world.spawnEntity(stun);
                    break;
            }
            
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
        }
    }
} 