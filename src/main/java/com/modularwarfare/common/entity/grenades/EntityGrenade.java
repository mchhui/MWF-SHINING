package com.modularwarfare.common.entity.grenades;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.init.ModSounds;
import com.modularwarfare.common.world.MWFExplosion;
import com.modularwarfare.utility.DamageControlHelper;
import com.modularwarfare.utility.RayUtil;
import mchhui.modularmovements.coremod.ModularMovementsHooks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.*;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityGrenade extends Entity {
    private static final double FLYING_DRAG = 0.98D;
    private static final double GROUND_FRICTION = 0.8D;
    private static final double ENTITY_HIT_BOUNCE_RETENTION = 0.15D;
    private static final double BLOCK_HIT_BOUNCE_RETENTION = 0.15D;

    private static final DataParameter GRENADE_NAME = EntityDataManager.createKey(EntityGrenade.class, DataSerializers.STRING);
    private static final DataParameter<Boolean> IS_STUCK = EntityDataManager.createKey(EntityGrenade.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> STUCK_TO_ENTITY_ID = EntityDataManager.createKey(EntityGrenade.class, DataSerializers.VARINT);
    private static final DataParameter<BlockPos> STUCK_POS = EntityDataManager.createKey(EntityGrenade.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Float> STUCK_ROT_X = EntityDataManager.createKey(EntityGrenade.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> STUCK_ROT_Y = EntityDataManager.createKey(EntityGrenade.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> STUCK_ROT_Z = EntityDataManager.createKey(EntityGrenade.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> STUCK_TICKS = EntityDataManager.createKey(EntityGrenade.class, DataSerializers.VARINT);

    public EntityLivingBase thrower;
    public GrenadeType grenadeType;
    public boolean playedSound = false;
    public float fuse;
    public boolean exploded = false;
    protected boolean hasBouncedOnGround = false;
    private Entity stuckEntity;
    private EnumFacing stuckFace;
    protected final Set<Integer> impactedEntityIds = new HashSet<>();

    public EntityGrenade(World worldIn) {
        super(worldIn);
        this.fuse = 80;
        this.preventEntitySpawning = true;
        this.isImmuneToFire = true;
        this.setSize(0.25f, 0.25f);
        this.setEntityInvulnerable(false);
    }

    public EntityGrenade(World world, EntityLivingBase thrower, float throwStrength, GrenadeType grenadeType) {
        this(world, thrower, throwStrength, grenadeType, false);
    }

    public EntityGrenade(World world, EntityLivingBase thrower, float throwStrength, GrenadeType grenadeType, boolean isLowThrow) {
        this(world);

        this.setGrenadeName(grenadeType.internalName);
        this.grenadeType = grenadeType;
        this.fuse = grenadeType.fuseTime * 20;
        this.exploded = false;
        
        Vec3d eye = thrower.getPositionEyes(1);
        if(ModularWarfare.isLoadedModularMovements) {
            if (thrower instanceof EntityPlayer) {
                eye = ModularMovementsHooks.onGetPositionEyes((EntityPlayer) thrower, 1);
            }
        }
        
        this.setPosition(eye.x, eye.y, eye.z);

        Vec3d lookVec = thrower.getLookVec();
        
        if(!isLowThrow) {
            float playerPitch = thrower.rotationPitch;
            float playerYaw = thrower.rotationYaw;
            
            float newPitch = Math.max(-90, playerPitch - 10);
            
            float f = -MathHelper.sin(playerYaw * 0.017453292F) * MathHelper.cos(newPitch * 0.017453292F);
            float f1 = -MathHelper.sin(newPitch * 0.017453292F);
            float f2 = MathHelper.cos(playerYaw * 0.017453292F) * MathHelper.cos(newPitch * 0.017453292F);
            
            lookVec = new Vec3d(f, f1, f2);
        }
        
        float actualStrength = throwStrength * (thrower.isSprinting() ? 1.25f : 1.0f);
        
        this.motionX = lookVec.x * 1.5 * actualStrength;
        this.motionY = lookVec.y * 1.5 * actualStrength;
        this.motionZ = lookVec.z * 1.5 * actualStrength;

        this.thrower = thrower;
    }

    public EntityGrenade(World world, EntityLivingBase thrower, boolean isRightClick, GrenadeType grenadeType) {
        this(world, thrower, isRightClick ? grenadeType.throwStrength : grenadeType.throwStrength * 0.5f, grenadeType);
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    @Override
    public void onUpdate() {
        if (isStuck() && stuckEntity != null) {
            if (!stuckEntity.isEntityAlive()) {
                setStuck(false);
                setStuckToEntity(null);
            } else {
                Vec3d pos = stuckEntity.getPositionVector();
                this.setPosition(pos.x, pos.y + stuckEntity.getEyeHeight() / 2, pos.z);
                this.motionX = 0;
                this.motionY = 0;
                this.motionZ = 0;
                this.onGround = false;
            }
        }
        else if (isStuck() && !this.dataManager.get(STUCK_POS).equals(BlockPos.ORIGIN)) {
            BlockPos stuckPos = this.dataManager.get(STUCK_POS);
            if (this.world.isAirBlock(stuckPos)) {
                setStuck(false);
                this.dataManager.set(STUCK_POS, BlockPos.ORIGIN);
            } else {
                if (stuckFace != null) {
                    switch (stuckFace) {
                        case DOWN:  // y-
                            this.setPosition(this.posX, stuckPos.getY(), this.posZ);
                            break;
                        case UP:    // y+
                            this.setPosition(this.posX, stuckPos.getY() + 1, this.posZ);
                            break;
                        case NORTH: // z-
                            this.setPosition(this.posX, this.posY, stuckPos.getZ());
                            break;
                        case SOUTH: // z+
                            this.setPosition(this.posX, this.posY, stuckPos.getZ() + 1);
                            break;
                        case WEST:  // x-
                            this.setPosition(stuckPos.getX(), this.posY, this.posZ);
                            break;
                        case EAST:  // x+
                            this.setPosition(stuckPos.getX() + 1, this.posY, this.posZ);
                            break;
                    }
                }
                
                this.motionX = 0;
                this.motionY = 0;
                this.motionZ = 0;
                this.onGround = false;
            }
        }
        else {
            tickFlyingPhysicsStep();

            if (processFlyingCollisions()) {
                return;
            }
        }

        if (Math.abs(motionX) < 0.1 && Math.abs(motionZ) < 0.1) {
            motionX = 0;
            motionZ = 0;
        }

        --this.fuse;

        if (!isStuck()) {
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        }

        if (this.fuse <= 0) {
            explode();
            if (this.fuse <= -20) {
                this.setDead();
            }
        } else {
            this.handleWaterMovement();
            if (!this.isInWater()) {
                this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX, this.posY + 0.2D, this.posZ, 0.0D, 0.0D, 0.0D);
            } else {
                this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX, this.posY + 0.2D, this.posZ, 0.0D, 0.1D, 0.0D);
            }
        }
    }

    public String getGrenadeName() {
        return (String) this.dataManager.get(GRENADE_NAME);
    }

    public void setGrenadeName(String grenadeName) {
        this.dataManager.set(GRENADE_NAME, grenadeName);
    }

    public void explode() {
        if (!this.world.isRemote && !exploded) {
            if (grenadeType != null) {
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
        exploded = true;
        this.setDead();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(GRENADE_NAME, "");
        this.dataManager.register(IS_STUCK, false);
        this.dataManager.register(STUCK_TO_ENTITY_ID, -1);
        this.dataManager.register(STUCK_POS, BlockPos.ORIGIN);
        this.dataManager.register(STUCK_ROT_X, 0.0F);
        this.dataManager.register(STUCK_ROT_Y, 0.0F);
        this.dataManager.register(STUCK_ROT_Z, 0.0F);
        this.dataManager.register(STUCK_TICKS, 0);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setDouble("posX", this.posX);
        compound.setDouble("posY", this.posY);
        compound.setDouble("posZ", this.posZ);
        compound.setDouble("motionX", this.motionX);
        compound.setDouble("motionY", this.motionY);
        compound.setDouble("motionZ", this.motionZ);
        compound.setFloat("fuse", this.fuse);
        compound.setBoolean("hasBouncedOnGround", this.hasBouncedOnGround);
        compound.setBoolean("isStuck", isStuck());
        compound.setInteger("stuckEntityId", this.dataManager.get(STUCK_TO_ENTITY_ID));
        compound.setLong("stuckPos", this.dataManager.get(STUCK_POS).toLong());
        if (stuckFace != null) {
            compound.setInteger("stuckFace", stuckFace.getIndex());
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        posX = compound.getDouble("posX");
        posY = compound.getDouble("posY");
        posZ = compound.getDouble("posZ");
        motionX = compound.getDouble("motionX");
        motionY = compound.getDouble("motionY");
        motionZ = compound.getDouble("motionZ");
        fuse = compound.getInteger("fuse");
        hasBouncedOnGround = compound.getBoolean("hasBouncedOnGround");
        setStuck(compound.getBoolean("isStuck"));
        int entityId = compound.getInteger("stuckEntityId");
        if (entityId != -1) {
            Entity entity = this.world.getEntityByID(entityId);
            if (entity != null) {
                setStuckToEntity(entity);
            }
        }
        BlockPos pos = BlockPos.fromLong(compound.getLong("stuckPos"));
        if (compound.hasKey("stuckFace")) {
            stuckFace = EnumFacing.byIndex(compound.getInteger("stuckFace"));
        }
        if (pos != BlockPos.ORIGIN) {
            setStuckPos(pos, stuckFace);
        }
    }

    public boolean isStuck() {
        try {
            return this.dataManager != null && this.dataManager.get(IS_STUCK);
        } catch (Exception e) {
            return false;
        }
    }

    public void setStuck(boolean stuck) {
        this.dataManager.set(IS_STUCK, stuck);
    }

    public void setStuckToEntity(Entity entity) {
        this.stuckEntity = entity;
        this.dataManager.set(STUCK_TO_ENTITY_ID, entity != null ? entity.getEntityId() : -1);
    }

    public void setStuckPos(BlockPos pos, EnumFacing face) {
        this.dataManager.set(STUCK_POS, pos);
        this.stuckFace = face;
    }

    public void setStuckRotation(float x, float y, float z) {
        this.dataManager.set(STUCK_ROT_X, x);
        this.dataManager.set(STUCK_ROT_Y, y);
        this.dataManager.set(STUCK_ROT_Z, z);
    }

    public float getStuckRotX() {
        return this.dataManager.get(STUCK_ROT_X);
    }

    public float getStuckRotY() {
        return this.dataManager.get(STUCK_ROT_Y);
    }

    public float getStuckRotZ() {
        return this.dataManager.get(STUCK_ROT_Z);
    }

    public void setStuckTicks(int ticks) {
        this.dataManager.set(STUCK_TICKS, ticks);
    }

    public int getStuckTicks() {
        return this.dataManager.get(STUCK_TICKS);
    }

    /** 飞行段每 tick：重力、空气阻尼、着地摩擦与着地音效。子类自定义 onUpdate 时应先调用本方法以保持与破片手雷一致。 */
    protected void tickFlyingPhysicsStep() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (!this.hasNoGravity()) {
            this.motionY -= 0.04D;
        }

        this.motionX *= FLYING_DRAG;
        this.motionY *= FLYING_DRAG;
        this.motionZ *= FLYING_DRAG;

        if (this.onGround) {
            this.motionX *= GROUND_FRICTION;
            this.motionZ *= GROUND_FRICTION;
            if (Math.abs(motionX) < 0.1 && Math.abs(motionZ) < 0.1) {
                motionX = 0;
                motionZ = 0;
            }
            if (!playedSound) {
                world.playSound(null, this.posX, this.posY, this.posZ, ModSounds.GRENADE_HIT, SoundCategory.BLOCKS, 0.50f, 1.0f);
                playedSound = true;
            }
        }
    }

    /** 是否仍应对飞行轨迹做实体/方块碰撞（子类可按状态覆盖，例如爆炸后不再检测） */
    protected boolean shouldProcessFlyingCollisions() {
        return this.grenadeType != null && !this.exploded;
    }

    /**
     * 飞行中：impact、生物反弹、黏附生物/方块、碰方块瞬爆。
     *
     * @return true 表示本 tick 已 explode/setDead，调用方应直接 return 结束 onUpdate
     */
    protected boolean processFlyingCollisions() {
        if (!shouldProcessFlyingCollisions()) {
            return false;
        }

        Vec3d currentPos = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d nextPos = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this,
                this.getEntityBoundingBox().expand(this.motionX, this.motionY, this.motionZ).grow(1.0D));

        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (entity != thrower && entity instanceof EntityLivingBase) {
                    double motionMagnitude = Math.sqrt(this.motionX * this.motionX
                            + this.motionY * this.motionY
                            + this.motionZ * this.motionZ);
                    RayTraceResult entityTrace = entity.getEntityBoundingBox().calculateIntercept(currentPos, nextPos);
                    if (grenadeType.impactDamage > 0 && motionMagnitude > 0.1) {
                        if (DamageControlHelper.markImpactOnce(this.impactedEntityIds, entity)
                                && DamageControlHelper.canDamageTarget(this.thrower, entity, !grenadeType.throwerVulnerable)) {
                            boolean damaged = RayUtil.attackEntityWithoutKnockback(
                                    entity,
                                    DamageSource.causeThrownDamage(this, this.thrower),
                                    grenadeType.impactDamage
                            );
                            DamageControlHelper.clearHurtResistantTime(entity, damaged);
                        }
                    }
                    if (grenadeType.bounceOnEntity && !grenadeType.isSticky && !grenadeType.instantExplode
                            && motionMagnitude > 0.1 && entityTrace != null && entityTrace.sideHit != null) {
                        applyBounceFromEntityHit(entityTrace);
                        break;
                    }
                    if (grenadeType.isSticky) {
                        if (entityTrace != null) {
                            if (!this.world.isRemote) {
                                EntityGrenade newGrenade = new EntityGrenade(this.world, this.thrower, 0, this.grenadeType);
                                newGrenade.setPosition(entityTrace.hitVec.x, entityTrace.hitVec.y, entityTrace.hitVec.z);
                                newGrenade.motionX = 0;
                                newGrenade.motionY = 0;
                                newGrenade.motionZ = 0;
                                newGrenade.fuse = this.fuse;
                                newGrenade.setStuck(true);
                                newGrenade.setStuckToEntity(entity);

                                this.world.spawnEntity(newGrenade);
                                this.setDead();
                            }

                            return true;
                        }
                    } else if (grenadeType.instantExplode) {
                        explode();
                        return true;
                    }
                }
            }
        }

        RayTraceResult raytraceresult = this.world.rayTraceBlocks(currentPos, nextPos, false, true, false);

        if (raytraceresult != null && raytraceresult.typeOfHit == RayTraceResult.Type.BLOCK) {
            if (grenadeType.isSticky) {
                if (!this.world.isRemote) {
                    BlockPos hitPos = raytraceresult.getBlockPos();
                    EnumFacing hitFace = raytraceresult.sideHit;

                    Vec3d hitVec = raytraceresult.hitVec;

                    double offset = 0.01D;
                    hitVec = hitVec.add(
                            hitFace.getXOffset() * offset,
                            hitFace.getYOffset() * offset,
                            hitFace.getZOffset() * offset
                    );

                    EntityGrenade newGrenade = new EntityGrenade(this.world, this.thrower, 0, this.grenadeType);
                    newGrenade.setPosition(hitVec.x, hitVec.y, hitVec.z);
                    newGrenade.motionX = 0;
                    newGrenade.motionY = 0;
                    newGrenade.motionZ = 0;
                    newGrenade.fuse = this.fuse;
                    newGrenade.setStuck(true);
                    newGrenade.setStuckPos(hitPos, hitFace);

                    this.world.spawnEntity(newGrenade);
                    this.setDead();
                }

                return true;
            } else if (grenadeType.instantExplode && !exploded) {
                explode();
                return true;
            } else if (grenadeType.bounceOnBlock) {
                applyBounceFromBlockHit(raytraceresult);
            }
        }

        return false;
    }

    /** 沿实体 AABB 碰撞面反弹；法线方向出射速率约为入射的 {@link #ENTITY_HIT_BOUNCE_RETENTION}（反向） */
    protected void applyBounceFromEntityHit(RayTraceResult trace) {
        if (trace == null || trace.sideHit == null || trace.hitVec == null) {
            return;
        }
        EnumFacing side = trace.sideHit;
        switch (side.getAxis()) {
            case X:
                this.motionX = -this.motionX * ENTITY_HIT_BOUNCE_RETENTION;
                break;
            case Y:
                this.motionY = -this.motionY * ENTITY_HIT_BOUNCE_RETENTION;
                break;
            case Z:
                this.motionZ = -this.motionZ * ENTITY_HIT_BOUNCE_RETENTION;
                break;
            default:
                break;
        }
        Vec3d hit = trace.hitVec;
        this.setPosition(
                hit.x + side.getXOffset() * 0.05D,
                hit.y + side.getYOffset() * 0.05D,
                hit.z + side.getZOffset() * 0.05D
        );
        if (!this.world.isRemote) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, ModSounds.GRENADE_HIT, SoundCategory.BLOCKS, 0.5f, 1.0f);
        }
    }

    /** 沿方块碰撞面反弹；地面（UP 面）仅允许一次反弹。 */
    protected void applyBounceFromBlockHit(RayTraceResult trace) {
        if (trace == null || trace.sideHit == null || trace.hitVec == null) {
            return;
        }
        EnumFacing side = trace.sideHit;
        if (grenadeType == null) {
            return;
        }
        if (!grenadeType.bounceOnBlock) {
            return;
        }
        if (side == EnumFacing.UP && this.hasBouncedOnGround) {
            return;
        }
        switch (side.getAxis()) {
            case X:
                this.motionX = -this.motionX * BLOCK_HIT_BOUNCE_RETENTION;
                break;
            case Y:
                this.motionY = -this.motionY * BLOCK_HIT_BOUNCE_RETENTION;
                if (side == EnumFacing.UP) {
                    this.hasBouncedOnGround = true;
                }
                break;
            case Z:
                this.motionZ = -this.motionZ * BLOCK_HIT_BOUNCE_RETENTION;
                break;
            default:
                break;
        }
        Vec3d hit = trace.hitVec;
        this.setPosition(
                hit.x + side.getXOffset() * 0.05D,
                hit.y + side.getYOffset() * 0.05D,
                hit.z + side.getZOffset() * 0.05D
        );
        if (!this.world.isRemote) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, ModSounds.GRENADE_HIT, SoundCategory.BLOCKS, 0.5f, 1.0f);
        }
    }
}
