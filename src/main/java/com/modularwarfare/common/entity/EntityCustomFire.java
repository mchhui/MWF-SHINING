package com.modularwarfare.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.math.AxisAlignedBB;
import java.util.List;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import safx.SAPackets;
import safx.packets.PacketSpawnParticleOnEntity;

public class EntityCustomFire extends Entity {
    private int lifetime;
    private float damage;
    private int fireDuration;
    private boolean throughWalls;
    private Entity exploder;

    public EntityCustomFire(World worldIn) {
        super(worldIn);
        this.setSize(0.25F, 0.25F);
        this.lifetime = 100; // 默认5秒
        this.damage = 1.0F;
        this.fireDuration = 5;
        this.noClip = false;
    }

    public EntityCustomFire(World worldIn, double x, double y, double z, int lifetime, float damage, int fireDuration, boolean throughWalls, Entity exploder) {
        this(worldIn);
        this.setPosition(x, y, z);
        this.lifetime = lifetime;
        this.damage = damage;
        this.fireDuration = fireDuration;
        this.throughWalls = throughWalls;
        this.exploder = exploder;
        
        // 创建火焰特效
        if (!this.world.isRemote && this.isAddedToWorld()) {
            SAPackets.network.sendToAll(new PacketSpawnParticleOnEntity("ExplosionFire", this, 0, 0, 0, true));
        }
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!this.world.isRemote) {
            // 检查生命周期
            if (this.lifetime-- <= 0) {
                this.setDead();
                return;
            }

            // 检查是否有方块支撑
            BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
            boolean hasSupport = false;
            
            // 检查六个方向是否有方块
            if (world.getBlockState(pos.down()).isFullBlock() ||
                world.getBlockState(pos.north()).isFullBlock() ||
                world.getBlockState(pos.south()).isFullBlock() ||
                world.getBlockState(pos.east()).isFullBlock() ||
                world.getBlockState(pos.west()).isFullBlock()) {
                hasSupport = true;
            }

            // 如果没有支撑，应用重力
            if (!hasSupport) {
                this.motionY -= 0.03D;
                this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            } else {
                this.motionY = 0;
            }

            // 伤害和点燃附近实体
            AxisAlignedBB boundingBox = this.getEntityBoundingBox().grow(0.5D);
            List<Entity> nearbyEntities = this.world.getEntitiesWithinAABBExcludingEntity(this, boundingBox);
            
            for (Entity entity : nearbyEntities) {
                if (entity != this.exploder && !entity.isImmuneToFire()) {
                    if (this.throughWalls || this.canSee(entity)) {
                        entity.attackEntityFrom(DamageSource.IN_FIRE, this.damage);
                        entity.setFire(this.fireDuration);
                    }
                }
            }
        }

        // 生成粒子效果
        if (this.world.isRemote) {
            // for (int i = 0; i < 2; i++) {
            //     this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
            //         this.posX + (this.rand.nextDouble() - 0.5D) * 0.25D,
            //         this.posY + (this.rand.nextDouble() - 0.5D) * 0.25D,
            //         this.posZ + (this.rand.nextDouble() - 0.5D) * 0.25D,
            //         0.0D, 0.0D, 0.0D);
            // }
            
            // 随机播放火焰音效
            if (this.rand.nextInt(24) == 0) {
                this.world.playSound(this.posX, this.posY, this.posZ,
                    net.minecraft.init.SoundEvents.BLOCK_FIRE_AMBIENT,
                    SoundCategory.BLOCKS, 1.0F + this.rand.nextFloat(),
                    this.rand.nextFloat() * 0.7F + 0.3F, false);
            }
        }
    }

    private boolean canSee(Entity target) {
        return this.world.rayTraceBlocks(
            this.getPositionVector().add(0, this.height / 2, 0),
            target.getPositionVector().add(0, target.height / 2, 0),
            false, true, false) == null;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.lifetime = compound.getInteger("Lifetime");
        this.damage = compound.getFloat("Damage");
        this.fireDuration = compound.getInteger("FireDuration");
        this.throughWalls = compound.getBoolean("ThroughWalls");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("Lifetime", this.lifetime);
        compound.setFloat("Damage", this.damage);
        compound.setInteger("FireDuration", this.fireDuration);
        compound.setBoolean("ThroughWalls", this.throughWalls);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }
} 