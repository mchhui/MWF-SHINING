package com.modularwarfare.common.entity.grenades;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.init.ModSounds;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class EntityGrenade extends Entity {

    private static final DataParameter GRENADE_NAME = EntityDataManager.createKey(EntityGrenade.class, DataSerializers.STRING);

    public EntityLivingBase thrower;
    public GrenadeType grenadeType;
    public boolean playedSound = false;
    public float fuse;
    public boolean exploded = false;

    public EntityGrenade(World worldIn) {
        super(worldIn);
        this.fuse = 80;
        this.preventEntitySpawning = true;
        this.isImmuneToFire = true;
        this.setSize(0.35f, 0.35f);
        this.setEntityInvulnerable(false);
    }

    public EntityGrenade(World world, EntityLivingBase thrower, boolean isRightClick, GrenadeType grenadeType) {
        this(world);

        this.setGrenadeName(grenadeType.internalName);
        this.grenadeType = grenadeType;
        this.fuse = grenadeType.fuseTime * 20;
        this.exploded = false;
        this.setPosition(thrower.posX, thrower.posY + thrower.getEyeHeight(), thrower.posZ);
        float strenght = grenadeType.throwStrength;

        if (!isRightClick) {
            strenght *= 0.5f;
        }

        Vec3d vec = thrower.getLookVec();
        double modifier = 1;
        if (thrower.isSprinting()) {
            modifier = 1.25;
        }

        this.motionX = ((vec.x * 1.5) * modifier) * strenght;
        this.motionY = ((vec.y * 1.5) * modifier) * strenght;
        this.motionZ = ((vec.z * 1.5) * modifier) * strenght;

        this.thrower = thrower;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (!this.hasNoGravity()) {
            this.motionY -= 0.04D;
        }

        this.motionX *= 0.98D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.98D;

        if (this.onGround) {
            this.motionX *= 0.8D;
            this.motionZ *= 0.8D;
            if (!playedSound) {
                world.playSound(null, this.posX, this.posY, this.posZ, ModSounds.GRENADE_HIT, SoundCategory.BLOCKS, 0.50f, 1.0f);
                playedSound = true;
            }
        }

        if (Math.abs(motionX) < 0.1 && Math.abs(motionZ) < 0.1) {
            motionX = 0;
            motionZ = 0;
        }

        --this.fuse;

        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

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

                Explosion explosion = new Explosion(this.world, grenadeType.throwerVulnerable ? null : thrower, posX,
                        posY, posZ, grenadeType.explosionPower, false, grenadeType.damageWorld);
                /*
                explosion.doExplosionA();
                explosion.doExplosionB(true);
                */
                if(grenadeType.damageWorld) {
                    float f = grenadeType.explosionPower * (0.7F + this.world.rand.nextFloat() * 0.6F);
                    for (int x = -grenadeType.explosionPower; x <= grenadeType.explosionPower; x++) {
                        for (int y = -grenadeType.explosionPower; y <= grenadeType.explosionPower; y++) {
                            for (int z = -grenadeType.explosionPower; z <= grenadeType.explosionPower; z++) {
                                BlockPos blockPos = new BlockPos(posX + x, posY + y, posZ + z);
                                if (world.getBlockState(blockPos).getMaterial() != Material.AIR) {
                                    if (f - (world.getBlockState(blockPos).getBlock().getExplosionResistance(world,
                                            blockPos, this, explosion) + 0.3) * 0.3f > 0) {
                                        if (Math.sqrt(x * x + y * y + z * z) <= grenadeType.explosionPower) {
                                            Block block = world.getBlockState(blockPos).getBlock();
                                            if (block.canDropFromExplosion(explosion)) {
                                                block.dropBlockAsItemWithChance(this.world, blockPos,
                                                        this.world.getBlockState(blockPos),
                                                        1.0F / grenadeType.explosionPower, 0);
                                            }
                                            block.onBlockExploded(world, blockPos, explosion);
                                        }
                                    }
                                }
                            }
                        }
                    }  
                }

                List<Entity> entities = world.getEntitiesInAABBexcluding(this,
                        new AxisAlignedBB(posX - 1 / grenadeType.explosionParamK,
                                posY - 1 / grenadeType.explosionParamK, posZ - 1 / grenadeType.explosionParamK,
                                posX + 1 / grenadeType.explosionParamK, posY + 1 / grenadeType.explosionParamK,
                                posZ + 1 / grenadeType.explosionParamK),
                        null);
                for (Entity entity : entities) {
                    if(!grenadeType.throwerVulnerable) {
                        if(entity==thrower) {
                            continue;
                        }
                    }
                    for (int i = 1; i <= 3; i++) {
                        Vec3d entityPos = entity.getPositionVector().addVector(0, entity.getEyeHeight() / 3 * i, 0);
                        if (world.rayTraceBlocks(getPositionVector(), entityPos, false, true, false) == null) {
                            entity.attackEntityFrom(DamageSource.causeExplosionDamage(explosion),
                                    grenadeType.explosionParamA * (float) Math.max(0,
                                            (grenadeType.explosionParamK * entityPos.distanceTo(getPositionVector())
                                                    + 1)));
                            break;
                        }
                    }
                }
                ModularWarfare.PROXY.spawnExplosionParticle(this.world, this.posX, this.posY, this.posZ);
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
    }
}
