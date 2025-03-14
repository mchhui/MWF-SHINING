package com.modularwarfare.common.network;

import com.modularwarfare.common.entity.EntityCustomFire;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import safx.SAPackets;
import safx.packets.PacketSpawnParticleOnEntity;
import safx.util.EntityCondition;

public class PacketSpawnCustomFire extends PacketBase {
    private double x, y, z;
    private int lifeTime;
    private int duration;
    private float damage;
    private boolean throughWalls;
    private int exploderId;  // 爆炸源实体的ID

    public PacketSpawnCustomFire() {} // 需要无参构造函数

    public PacketSpawnCustomFire(double x, double y, double z, int lifeTime, float damage, int duration, boolean throughWalls, int exploderId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.lifeTime = lifeTime;
        this.duration = duration;
        this.damage = damage;
        this.throughWalls = throughWalls;
        this.exploderId = exploderId;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeDouble(x);
        data.writeDouble(y);
        data.writeDouble(z);
        data.writeInt(lifeTime);
        data.writeInt(duration);
        data.writeFloat(damage);
        data.writeBoolean(throughWalls);
        data.writeInt(exploderId);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        x = data.readDouble();
        y = data.readDouble();
        z = data.readDouble();
        lifeTime = data.readInt();
        duration = data.readInt();
        damage = data.readFloat();
        throughWalls = data.readBoolean();
        exploderId = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        World world = playerEntity.world;
        EntityCustomFire fireEntity = new EntityCustomFire(
            world,
            x, y, z,
            lifeTime,
            damage,
            duration,
            throughWalls,
            world.getEntityByID(exploderId)
        );
        world.spawnEntity(fireEntity);
        
        // 生成实体后发送特效包
        if (fireEntity.isAddedToWorld()) {
            SAPackets.network.sendToAll(new PacketSpawnParticleOnEntity(
                "FlamethrowerTrail", 
                fireEntity, 
                0, 0, 0, 
                true,
                EntityCondition.NONE,
                1.5f
            ));
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        // 客户端不需要处理，因为实体会自动同步
    }
}