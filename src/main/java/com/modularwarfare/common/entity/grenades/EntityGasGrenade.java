package com.modularwarfare.common.entity.grenades;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.init.ModSounds;
import com.modularwarfare.common.world.MWFExplosion;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.entity.MoverType;

public class EntityGasGrenade extends EntityGrenade {
    private int secondaryExplosionTimer = 0;
    private int secondaryExplosionCount = 0;
    private boolean hasStartedSecondaryExplosions = false;
    private boolean playedSound = false;

    public EntityGasGrenade(World worldIn) {
        super(worldIn);
    }

    public EntityGasGrenade(World world, EntityLivingBase thrower, float throwStrength, GrenadeType grenadeType) {
        this(world, thrower, throwStrength, grenadeType, false);
    }

    public EntityGasGrenade(World world, EntityLivingBase thrower, float throwStrength, GrenadeType grenadeType, boolean isLowThrow) {
        super(world, thrower, throwStrength, grenadeType, isLowThrow);
        this.preventEntitySpawning = true;
        this.isImmuneToFire = true;
        this.setSize(0.35f, 0.35f);
        this.setEntityInvulnerable(false);
    }

    public EntityGasGrenade(World world, EntityLivingBase thrower, boolean isRightClick, GrenadeType grenadeType) {
        this(world, thrower, isRightClick ? grenadeType.throwStrength : grenadeType.throwStrength * 0.5f, grenadeType);
    }

    @Override
    public void onUpdate() {
        if (grenadeType == null) {
            super.onUpdate();
            return;
        }
        
        tickFlyingPhysicsStep();

        if (processFlyingCollisions()) {
            return;
        }

        if (!isStuck()) {
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        }

        // 处理引信和爆炸
        --this.fuse;
        
        if (this.fuse <= 0 && !exploded) {
            // 如果有初始爆炸，执行一次
            if (grenadeType.gasType.hasFirstExplosion) {
                if (!this.world.isRemote) {
                    explode();
                }
            }
            // 无论是否有初始爆炸，都标记为已爆炸并开始二次爆炸计时
            exploded = true;
            hasStartedSecondaryExplosions = false;
            secondaryExplosionTimer = 0;
        }
        
        // 处理二次爆炸效果
        if (exploded) {
            if (!hasStartedSecondaryExplosions) {
                // 等待二阶段爆炸延迟
                if (secondaryExplosionTimer < grenadeType.gasType.explosionDelay * 20) {
                    secondaryExplosionTimer++;
                } else {
                    hasStartedSecondaryExplosions = true;
                    secondaryExplosionTimer = 0;
                }
            } else if (secondaryExplosionCount < grenadeType.gasType.explosionCount) {
                // 执行二阶段爆炸
                if (secondaryExplosionTimer >= grenadeType.gasType.explosionInterval * 20) {
                    if (!this.world.isRemote) {
                        doSecondaryExplosion();
                    }
                    secondaryExplosionTimer = 0;
                    secondaryExplosionCount++;
                } else {
                    secondaryExplosionTimer++;
                }
                
                // 产生烟雾粒子效果
                if (!this.world.isRemote) {
                    for (int i = 0; i < 8; i++) {
                        double particleX = this.posX + (this.rand.nextDouble() - 0.5D) * grenadeType.gasType.explosionRange;
                        double particleY = this.posY + (this.rand.nextDouble() - 0.5D) * grenadeType.gasType.explosionRange;
                        double particleZ = this.posZ + (this.rand.nextDouble() - 0.5D) * grenadeType.gasType.explosionRange;
                        this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, particleX, particleY, particleZ, 0.0D, 0.0D, 0.0D);
                    }
                }
            } else {
                // 只有在完成所有二次爆炸后才销毁实体
                this.setDead();
            }
        } else {
            // 未爆炸时的烟雾效果
            if (!this.isInWater()) {
                this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX, this.posY + 0.2D, this.posZ, 0.0D, 0.0D, 0.0D);
            } else {
                this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX, this.posY + 0.2D, this.posZ, 0.0D, 0.1D, 0.0D);
            }
        }
    }

    @Override
    public void explode() {
        if (!this.world.isRemote && !exploded && grenadeType != null) {
            MWFExplosion explosion = new MWFExplosion(this.world, grenadeType.throwerVulnerable ? null : thrower, posX,
                    posY, posZ, grenadeType.explosionRange, grenadeType.explosionDamage, grenadeType.explosionKnockback,
                    grenadeType.causesFire, grenadeType.damageWorld, grenadeType.allowBlockDrops);
            
            explosion.setExplosionThroughWalls(grenadeType.explosionThroughWalls);
            
            if (grenadeType.explosionPotionEffects != null) {
                explosion.setPotionEffects(grenadeType.explosionPotionEffects);
            }
            explosion.setFireLevel(grenadeType.explosionFireLevel);
            explosion.setKnockLevel(grenadeType.explosionKnockLevel);
            explosion.setBanShield(grenadeType.banShield);
            explosion.setIgnoreFriendlyTargets(!grenadeType.throwerVulnerable);
            
            explosion.doExplosionA();
            explosion.doExplosionB(true);
            ModularWarfare.PROXY.spawnExplosionParticle(this.world, this.posX, this.posY, this.posZ, null, null, grenadeType.causesFire);
        }
    }

    private void doSecondaryExplosion() {
        if (grenadeType.gasType == null) return;
        
        MWFExplosion explosion = new MWFExplosion(
            this.world,
            grenadeType.gasType.throwerVulnerable ? null : thrower,
            posX, posY, posZ,
            grenadeType.gasType.explosionRange,
            grenadeType.gasType.explosionDamage,
            grenadeType.gasType.explosionKnockback,
            grenadeType.gasType.causesFire,
            grenadeType.gasType.damageWorld,
            grenadeType.gasType.allowBlockDrops
        );
        
        explosion.setExplosionThroughWalls(grenadeType.gasType.explosionThroughWalls);
        
        if (grenadeType.gasType.explosionPotionEffects != null) {
            explosion.setPotionEffects(grenadeType.gasType.explosionPotionEffects);
        }
        
        explosion.setFireLevel(grenadeType.gasType.explosionFireLevel);
        explosion.setKnockLevel(grenadeType.gasType.explosionKnockLevel);
        explosion.setBanShield(grenadeType.banShield);
        explosion.setIgnoreFriendlyTargets(!grenadeType.gasType.throwerVulnerable);
        
        explosion.doExplosionA();
        explosion.doExplosionB(true);
        ModularWarfare.PROXY.spawnExplosionParticle(this.world, this.posX, this.posY, this.posZ, null, null, grenadeType.gasType.causesFire);
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }
} 