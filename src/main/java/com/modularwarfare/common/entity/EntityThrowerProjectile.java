package com.modularwarfare.common.entity;

import java.util.List;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.utility.DamageControlHelper;
import com.modularwarfare.utility.RayUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import safx.SAPackets;
import safx.packets.PacketSpawnParticleOnEntity;

import java.util.HashSet;
import java.util.Set;

public class EntityThrowerProjectile extends EntityBullet implements IProjectile {

    private float gravity;
    private boolean hasSmoke;
    private float impactDamage;
    private int maxLifeTime = 60;
    private int groundLifeTime = 10;
    private double maxDistance = 15.0;
    private Vec3d initialPos;
    private float initialSpeed;
    private float speedDecayFactor = 0.95f;
    private boolean hasHitGround = false;
    private int groundHitTime = 0;
    private final Set<Integer> impactedEntityIds = new HashSet<>();

    public EntityThrowerProjectile(World world) {
        super(world);
        setSize(0.2F, 0.2F);
    }

    public EntityThrowerProjectile(World par1World, EntityPlayer par2EntityPlayer, float damage, float accuracy, float velocity, String bulletName, float gravity, boolean isSmoke) {
        super(par1World, par2EntityPlayer, damage, accuracy, velocity, bulletName, gravity, isSmoke);
        this.gravity = gravity;
        this.hasSmoke = isSmoke;
        this.impactDamage = damage;
        this.initialPos = new Vec3d(posX, posY, posZ);
        this.initialSpeed = velocity * 0.3f;
        this.motionX *= 0.3f;
        this.motionY *= 0.3f;
        this.motionZ *= 0.3f;
    }

    public void onUpdate() {
        super.onUpdate();


        if (hasHitGround) {
            if (ticksExisted - groundHitTime > groundLifeTime) {
                this.setDead();
                return;
            }
        } else if (ticksExisted > maxLifeTime) {
            this.setDead();
            return;
        }


        if (initialPos != null) {
            double distanceTraveled = new Vec3d(posX, posY, posZ).distanceTo(initialPos);
            if (distanceTraveled > maxDistance) {
                this.setDead();
                return;
            }
        }


        float lifeProgress = hasHitGround ? 
            (float)(ticksExisted - groundHitTime) / groundLifeTime : 
            (float)ticksExisted / maxLifeTime;
        float speedMultiplier = 1.0f - (lifeProgress * 0.8f);
        
        if (hasHitGround) {
            this.motionX *= 0.7f * speedDecayFactor * speedMultiplier;
            this.motionZ *= 0.7f * speedDecayFactor * speedMultiplier;
        } else {
            this.motionX *= speedDecayFactor * speedMultiplier;
            this.motionZ *= speedDecayFactor * speedMultiplier;
        }


        this.motionY += this.gravity;

        Vec3d vec3d1 = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d vec3d = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
        RayTraceResult raytraceresult = this.world.rayTraceBlocks(vec3d1, vec3d, false, true, false);

        if (hasSmoke) {
            SAPackets.sendEffectToAllAround(new PacketSpawnParticleOnEntity("FlamethrowerTrail", this, 0, 0, 0, true), this, 512.0);
        }

        if (raytraceresult != null && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, raytraceresult)) {
            if (raytraceresult.typeOfHit == RayTraceResult.Type.BLOCK) {
                switch (raytraceresult.sideHit) {
                    case UP:
                        if (!hasHitGround) {
                            hasHitGround = true;
                            groundHitTime = ticksExisted;
                            this.motionX *= 0.3f;
                            this.motionZ *= 0.3f;
                        }
                        this.motionY = 0;
                        break;
                    case DOWN:
                        this.motionY = 0;
                        break;
                    case NORTH:
                    case SOUTH:
                        this.motionZ *= -0.3f;
                        break;
                    case WEST:
                    case EAST:
                        this.motionX *= -0.3f;
                        break;
                }
            }
        }

        List<Entity> entities = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().expand(this.motionX, this.motionY, this.motionZ).grow(0.5D));
        for (Entity entity : entities) {
            if (entity != null && entity.canBeCollidedWith() && entity != this.player) {
                if (entity instanceof EntityLivingBase) {
                    float damageMultiplier = hasHitGround ? 0.1f : 0.2f; // 碰撞地面后伤害降低
                    BulletType bulletType = getBulletType();
                    boolean ignoreFriendlyTargets = bulletType != null && !bulletType.shooterVulnerable;
                    if (DamageControlHelper.markImpactOnce(this.impactedEntityIds, entity)
                            && DamageControlHelper.canDamageTarget(this.player, entity, ignoreFriendlyTargets)) {
                        boolean damaged = RayUtil.attackEntityWithoutKnockback(
                                entity,
                                DamageSource.causePlayerDamage(this.player),
                                this.impactDamage * damageMultiplier
                        );
                        DamageControlHelper.clearHurtResistantTime(entity, damaged);
                    }
                }
            }
        }

        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.setPosition(this.posX, this.posY, this.posZ);
    }
    
    private BulletType getBulletType() {
        ItemBullet itemBullet = ModularWarfare.bulletTypes.get(this.getBulletName());
        return itemBullet != null ? itemBullet.type : null;
    }
}