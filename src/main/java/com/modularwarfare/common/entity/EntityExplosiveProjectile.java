package com.modularwarfare.common.entity;

import java.util.List;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.common.world.MWFExplosion;
import com.modularwarfare.utility.DamageControlHelper;
import com.modularwarfare.utility.RayUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class EntityExplosiveProjectile extends EntityBullet implements IProjectile {

    private float gravity;
    private boolean hasSmoke;
    private boolean hasExplosion;
    private float impactDamage;
    private final Set<Integer> impactedEntityIds = new HashSet<>();

    public EntityExplosiveProjectile(World world) {
        super(world);
        setSize(0.2F, 0.2F);
    }

    public EntityExplosiveProjectile(World par1World, EntityPlayer par2EntityPlayer, float damage, float accuracy, float velocity, String bulletName, float gravity, boolean isSmoke, boolean isExplosion) {
        super(par1World, par2EntityPlayer, damage, accuracy, velocity, bulletName, gravity, isSmoke, isExplosion);
        this.gravity = gravity;
        this.hasSmoke = isSmoke;
        this.hasExplosion = isExplosion;
        this.impactDamage = damage;
    }

    public void onUpdate() {
        super.onUpdate();

        this.motionY += this.gravity;

        Vec3d vec3d1 = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d vec3d = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
        RayTraceResult raytraceresult = this.world.rayTraceBlocks(vec3d1, vec3d, false, true, false);

        if (hasSmoke) {
            ModularWarfare.PROXY.spawnRocketParticle(this.world, this.posX, this.posY, this.posZ);
        }

        if (raytraceresult != null && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, raytraceresult)) {
            if (hasExplosion) {
                explode();
            }
            this.setDead();
        }

        List<Entity> entities = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().expand(this.motionX, this.motionY, this.motionZ).grow(0.5D));
        for (Entity entity : entities) {
            if (entity != null && entity.canBeCollidedWith()) {
                raytraceresult = new RayTraceResult(entity);
                if (hasExplosion) {
                    explode();
                }
                BulletType bulletType = getBulletType();
                boolean ignoreFriendlyTargets = bulletType != null && !bulletType.shooterVulnerable;
                if (DamageControlHelper.markImpactOnce(this.impactedEntityIds, entity)
                        && DamageControlHelper.canDamageTarget(this.player, entity, ignoreFriendlyTargets)) {
                    boolean damaged = RayUtil.attackEntityWithoutKnockback(
                            entity,
                            DamageSource.causePlayerDamage(this.player),
                            this.impactDamage
                    );
                    DamageControlHelper.clearHurtResistantTime(entity, damaged);
                }
                this.setDead();
                return;
            }
        }
    }

    public void explode() {
        if (!this.world.isRemote) {
            if (ModularWarfare.bulletTypes.containsKey(this.getBulletName())) {
                ItemBullet itemBullet = ModularWarfare.bulletTypes.get(this.getBulletName());
                MWFExplosion explosion = new MWFExplosion(this.world, this.player, posX, posY, posZ,
                        itemBullet.type.explosionRange, itemBullet.type.explosionDamage, itemBullet.type.explosionKnockback,
                        itemBullet.type.causesFire, itemBullet.type.damageWorld, itemBullet.type.allowBlockDrops);
                explosion.setIgnoreFriendlyTargets(!itemBullet.type.shooterVulnerable);
                explosion.doExplosionA();
                explosion.doExplosionB(true);
                
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