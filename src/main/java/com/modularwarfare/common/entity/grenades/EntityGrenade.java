package com.modularwarfare.common.entity.grenades;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.init.ModSounds;
import com.modularwarfare.common.world.MWFExplosion;
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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.List;

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
                MWFExplosion explosion = new MWFExplosion(this.world, grenadeType.throwerVulnerable ? null : thrower, posX,
                        posY, posZ, grenadeType.explosionRange, grenadeType.explosionDamage, grenadeType.explosionKnockback,
                        false, grenadeType.damageWorld, grenadeType.damageWorld);
                explosion.doExplosionA();
                explosion.doExplosionB(true);
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
