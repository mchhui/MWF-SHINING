package com.modularwarfare.common.entity.grenades;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.init.ModSounds;
import com.modularwarfare.common.network.PacketFlashClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;


public class EntitySmokeGrenade extends EntityGrenade {

    private static final DataParameter<String> GRENADE_NAME = EntityDataManager.createKey(EntitySmokeGrenade.class, DataSerializers.STRING);
    private static final DataParameter<Boolean> IS_EXPLODED = EntityDataManager.createKey(EntitySmokeGrenade.class, DataSerializers.BOOLEAN);

    public float smokeTime = 12 * 20;
    private float smokeScale = 0.0f;
    private static final float SMOKE_SCALE_SPEED = 0.05f;
    private static final float MAX_SMOKE_SCALE = 1.0f;

    public EntitySmokeGrenade(World worldIn) {
        super(worldIn);
    }

    public EntitySmokeGrenade(World world, EntityLivingBase thrower, float throwStrength, GrenadeType grenadeType) {
        this(world, thrower, throwStrength, grenadeType, false);
    }

    public EntitySmokeGrenade(World world, EntityLivingBase thrower, float throwStrength, GrenadeType grenadeType, boolean isLowThrow) {
        super(world, thrower, throwStrength, grenadeType, isLowThrow);
        this.smokeTime = grenadeType != null ? grenadeType.smokeTime * 20 : 220;
        this.smokeScale = 0.0f;
    }

    public EntitySmokeGrenade(World world, EntityLivingBase thrower, boolean isRightClick, GrenadeType grenadeType) {
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
        } else {
            this.handleWaterMovement();
            if (!this.isInWater()) {
                this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX, this.posY + 0.2D, this.posZ, 0.0D, 0.0D, 0.0D);
            } else {
                this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX, this.posY + 0.2D, this.posZ, 0.0D, 0.1D, 0.0D);
            }
        }

        if(this.isExploded()){
            if(smokeScale < MAX_SMOKE_SCALE) {
                smokeScale = Math.min(MAX_SMOKE_SCALE, smokeScale + SMOKE_SCALE_SPEED);
            }
            
            --this.smokeTime;
            if(this.smokeTime <= 0){
                setDead();
            }
        }
    }

    @Override
    public void explode(){
        if (!this.isExploded()) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, ModSounds.GRENADE_SMOKE, SoundCategory.BLOCKS, 2.0f, 1.0f);
            this.setExploded(true);
            this.fuse = 0;
            this.smokeTime = 220;
            this.smokeScale = 0.0f;
        }
    }

    public boolean isExploded() {
        return this.dataManager.get(IS_EXPLODED);
    }

    private void setExploded(boolean exploded) {
        this.dataManager.set(IS_EXPLODED, exploded);
    }

    public String getGrenadeName() {
        return this.dataManager.get(GRENADE_NAME);
    }

    public void setGrenadeName(String grenadeName) {
        this.dataManager.set(GRENADE_NAME, grenadeName);
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
        super.entityInit();
        this.dataManager.register(GRENADE_NAME, "");
        this.dataManager.register(IS_EXPLODED, false);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setFloat("smokeTime", this.smokeTime);
        compound.setBoolean("exploded", this.isExploded());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.smokeTime = compound.getFloat("smokeTime");
        this.setExploded(compound.getBoolean("exploded"));
    }

    public float getSmokeScale() {
        return this.smokeScale;
    }
}
