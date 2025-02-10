package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.entity.grenades.EntitySmokeGrenade;
import com.modularwarfare.common.entity.grenades.EntityStunGrenade;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.init.ModSounds;
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
            
            float strength = throwStrength * (isLowThrow ? type.throwStrengthLow : type.throwStrength);
            
            switch (type.grenadeType) {
                case Frag:
                    EntityGrenade grenade = new EntityGrenade(player.world, player, strength, type, isLowThrow);
                    if(remainingFuseTime > 0) {
                        grenade.fuse = (int)(remainingFuseTime * 20);
                    }
                    type.playSound(player, WeaponSoundType.GrenadeThrow, stack);
                    player.world.spawnEntity(grenade);
                    break;
                case Smoke:
                    EntitySmokeGrenade smoke = new EntitySmokeGrenade(player.world, player, strength, type, isLowThrow);
                    if(remainingFuseTime > 0) {
                        smoke.fuse = (int)(remainingFuseTime * 20);
                    }
                    type.playSound(player, WeaponSoundType.GrenadeThrow, stack);
                    player.world.spawnEntity(smoke);
                    break;
                case Stun:
                    EntityStunGrenade stun = new EntityStunGrenade(player.world, player, strength, type, isLowThrow);
                    if(remainingFuseTime > 0) {
                        stun.fuse = (int)(remainingFuseTime * 20);
                    }
                    type.playSound(player, WeaponSoundType.GrenadeThrow, stack);
                    player.world.spawnEntity(stun);
                    break;
            }
        }
    }
} 