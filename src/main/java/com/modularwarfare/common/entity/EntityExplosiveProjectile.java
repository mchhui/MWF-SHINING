package com.modularwarfare.common.entity;

import java.util.Set;
import java.util.Collections;


import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.common.world.MWFExplosion;
import com.modularwarfare.utility.DamageControlHelper;
import com.modularwarfare.utility.RayUtil;


import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;

import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;




public class EntityExplosiveProjectile extends EntityBullet implements IProjectile, net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData {

    private float gravity;
    private boolean hasSmoke;
    private boolean hasExplosion;
    private float impactDamage;
    private boolean ignoreShooter = true;
    private boolean collideEntities = true;
    private Set<String> ignoredEntityTypes = Collections.emptySet();


    public EntityExplosiveProjectile(World world) {
        super(world);
        setSize(0.2F, 0.2F);
    }

    public EntityExplosiveProjectile(World par1World, EntityPlayer par2EntityPlayer, float damage, float accuracy, float velocity, String bulletName, float gravity, boolean isSmoke, boolean isExplosion) {
        this(par1World, par2EntityPlayer, damage, accuracy, velocity, bulletName, gravity, isSmoke, isExplosion,
                par2EntityPlayer.rotationPitch, par2EntityPlayer.rotationYaw);
    }

    public EntityExplosiveProjectile(World world, EntityLivingBase shooter, float damage, float accuracy,
            float velocity, String bulletName, float gravity, boolean isSmoke, boolean isExplosion, float pitch, float yaw) {
        super(world, shooter, damage, accuracy, velocity, bulletName, pitch, yaw);
        this.gravity = gravity;
        this.hasSmoke = isSmoke;
        this.hasExplosion = isExplosion;
        this.impactDamage = damage;
    }

    @Override
    public void onUpdate() {
        super.onEntityUpdate();
        if (++ticks > 600) { setDead(); return; }
        Vec3d start = getPositionVector();
        Vec3d velocity = new Vec3d(motionX, motionY, motionZ);
        Vec3d end = start.add(velocity);
        RayTraceResult hit = com.modularwarfare.api.ProjectileAPI.trace(world, ignoreShooter ? shootingEntity : null, this, start, end, collideEntities, ignoredEntityTypes);
        if (!world.isRemote && hit != null && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
            setPosition(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z);
            if (hit.entityHit != null && impactDamage > 0) {
                BulletType type = getBulletType();
                if (DamageControlHelper.canDamageTarget(shootingEntity, hit.entityHit, type != null && !type.shooterVulnerable)) {
                    boolean damaged = RayUtil.attackEntityWithoutKnockback(hit.entityHit,
                            new net.minecraft.util.EntityDamageSourceIndirect("arrow", this, shootingEntity).setProjectile(), impactDamage);
                    DamageControlHelper.clearHurtResistantTime(hit.entityHit, damaged);
                }
            }
            if (hasExplosion) explode();
            setDead();
            return;
        }
        setPosition(end.x, end.y, end.z);
        Vec3d next = com.modularwarfare.api.ProjectileAPI.nextVelocity(world, end, velocity, gravity);
        motionX = next.x; motionY = next.y; motionZ = next.z;
        rotationYaw = (float) Math.toDegrees(Math.atan2(motionX, motionZ));
        rotationPitch = (float) Math.toDegrees(Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)));
        if (hasSmoke && world.isRemote) ModularWarfare.PROXY.spawnRocketParticle(world, posX, posY, posZ);
    }

    public void setIgnoreShooter(boolean ignoreShooter) { this.ignoreShooter = ignoreShooter; }
    public void setCollideEntities(boolean value) { collideEntities = value; }
    public void setIgnoredEntityTypes(Set<String> ignoredEntityTypes) {
        this.ignoredEntityTypes = ignoredEntityTypes == null ? Collections.emptySet() : new java.util.HashSet<>(ignoredEntityTypes);
    }

    @Override
    public void writeSpawnData(io.netty.buffer.ByteBuf buffer) {
        buffer.writeFloat(gravity).writeBoolean(hasSmoke).writeBoolean(hasExplosion).writeFloat(impactDamage);
        buffer.writeInt(shootingEntity == null ? -1 : shootingEntity.getEntityId());
        buffer.writeBoolean(collideEntities);
    }

    @Override
    public void readSpawnData(io.netty.buffer.ByteBuf buffer) {
        gravity = buffer.readFloat(); hasSmoke = buffer.readBoolean();
        hasExplosion = buffer.readBoolean(); impactDamage = buffer.readFloat();
        shootingEntity = world.getEntityByID(buffer.readInt());
        collideEntities = buffer.readBoolean();
        player = shootingEntity instanceof EntityPlayer ? (EntityPlayer) shootingEntity : null;
    }

    @Override
    public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("bulletName", getBulletName());
        tag.setBoolean("collideEntities", collideEntities);
        tag.setFloat("gravity", gravity); tag.setBoolean("smoke", hasSmoke);
        tag.setBoolean("explosion", hasExplosion); tag.setFloat("impactDamage", impactDamage);
        tag.setInteger("projectileAge", ticks);
        if (shootingEntity != null) tag.setUniqueId("shooter", shootingEntity.getUniqueID());
    }

    @Override
    public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setBulletType(tag.getString("bulletName"));
        collideEntities = !tag.hasKey("collideEntities") || tag.getBoolean("collideEntities");
        gravity = tag.getFloat("gravity"); hasSmoke = tag.getBoolean("smoke");
        hasExplosion = tag.getBoolean("explosion"); impactDamage = tag.getFloat("impactDamage");
        ticks = tag.getInteger("projectileAge");
        if (tag.hasUniqueId("shooter") && world instanceof net.minecraft.world.WorldServer)
            shootingEntity = ((net.minecraft.world.WorldServer) world).getEntityFromUuid(tag.getUniqueId("shooter"));
    }
    public void explode() {
        if (!this.world.isRemote) {
            if (ModularWarfare.bulletTypes.containsKey(this.getBulletName())) {
                ItemBullet itemBullet = ModularWarfare.bulletTypes.get(this.getBulletName());
                MWFExplosion explosion = new MWFExplosion(this.world, this.shootingEntity, posX, posY, posZ,
                        itemBullet.type.explosionRange, itemBullet.type.explosionDamage, itemBullet.type.explosionKnockback,
                        itemBullet.type.causesFire, itemBullet.type.damageWorld, itemBullet.type.allowBlockDrops);
                explosion.setIgnoreFriendlyTargets(!itemBullet.type.shooterVulnerable);
                explosion.doExplosionA();
                explosion.doExplosionB(true);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new com.modularwarfare.api.ProjectileExplosionEvent(this, explosion.getDamagedEntities()));
                
                String modelPath = null;
                String texturePath = null;
                
                if(itemBullet.type.customExplosionModel != null && !itemBullet.type.customExplosionModel.isEmpty()) {
                    modelPath = "modularwarfare:explosion/obj/" + itemBullet.type.customExplosionModel;
                }
                
                if(itemBullet.type.customExplosionTexture != null && !itemBullet.type.customExplosionTexture.isEmpty()) {
                    texturePath = "modularwarfare:explosion/texture/" + itemBullet.type.customExplosionTexture;
                }
                
                ModularWarfare.PROXY.spawnExplosionParticle(this.world, this.posX, this.posY, this.posZ, modelPath, texturePath, itemBullet.type.causesFire);
            }
        }
        this.setDead();
    }
    
    private BulletType getBulletType() {
        ItemBullet itemBullet = ModularWarfare.bulletTypes.get(this.getBulletName());
        return itemBullet != null ? itemBullet.type : null;
    }
}
