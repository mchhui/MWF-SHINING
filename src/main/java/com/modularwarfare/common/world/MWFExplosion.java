package com.modularwarfare.common.world;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MWFExplosion
{
    private final boolean causesFire;
    private final boolean allowBlockDrops;
    private final boolean damagesTerrain;
    private final Random random;
    private final World world;
    private final double x;
    private final double y;
    private final double z;
    private final Entity exploder;
    private final float size;
    private final float damage;
    private final float knockback;
    private final List<BlockPos> affectedBlockPositions;
    private final Map<EntityPlayer, Vec3d> playerKnockbackMap;
    private final Vec3d position;
    private final Explosion explosion;

    @SideOnly(Side.CLIENT)
    public MWFExplosion(World worldIn, Entity entityIn, double x, double y, double z, float size, List<BlockPos> affectedPositions)
    {
        this(worldIn, entityIn, x, y, z, size, 4.0f, 1.0f, false, true, true, affectedPositions);
    }

    @SideOnly(Side.CLIENT)
    public MWFExplosion(World worldIn, Entity entityIn, double x, double y, double z, float size, float damage, float knockback, boolean causesFire, boolean damagesTerrain, boolean allowBlockDrops, List<BlockPos> affectedPositions)
    {
        this(worldIn, entityIn, x, y, z, size, damage, knockback, causesFire, damagesTerrain, allowBlockDrops);
        this.affectedBlockPositions.addAll(affectedPositions);
        this.explosion.getAffectedBlockPositions().addAll(affectedPositions);
    }

    public MWFExplosion(World worldIn, Entity entityIn, double x, double y, double z, float size, float damage, float knockback, boolean flaming, boolean damagesTerrain, boolean allowBlockDrops)
    {
        this.random = new Random();
        this.world = worldIn;
        this.exploder = entityIn;
        this.size = size;
        this.damage = damage;
        this.knockback = knockback;
        this.x = x;
        this.y = y;
        this.z = z;
        this.causesFire = flaming;
        this.allowBlockDrops = allowBlockDrops;
        this.damagesTerrain = damagesTerrain;
        this.position = new Vec3d(this.x, this.y, this.z);
        this.explosion=new Explosion(worldIn, entityIn, x, y, z, size, flaming, damagesTerrain);
        this.affectedBlockPositions=this.explosion.getAffectedBlockPositions();
        this.playerKnockbackMap = this.explosion.getPlayerKnockbackMap();
    }

    public void doExplosionA()
    {
        Set<BlockPos> set = Sets.<BlockPos>newHashSet();
        
        if(this.damagesTerrain) {
            float f = this.size * (0.7F + this.world.rand.nextFloat() * 0.6F);
            for (int x = -(int)this.size; x <= (int)this.size; x++) {
                for (int y = -(int)this.size; y <= (int)this.size; y++) {
                    for (int z = -(int)this.size; z <= (int)this.size; z++) {
                        BlockPos blockPos = new BlockPos(this.x + x, this.y + y, this.z + z);
                        if (world.getBlockState(blockPos).getMaterial() != Material.AIR) {
                            if (f - (world.getBlockState(blockPos).getBlock().getExplosionResistance(world,
                                    blockPos, this.exploder, this.explosion) + 0.3) * 0.3f > 0) {
                                if (Math.sqrt(x * x + y * y + z * z) <= this.size) {
                                    set.add(blockPos);
                                }
                            }
                        }
                    }
                }
            }  
        }

        this.affectedBlockPositions.addAll(set);
        float range = this.size * 2.0F;
        int k1 = MathHelper.floor(this.x - (double)range - 1.0D);
        int l1 = MathHelper.floor(this.x + (double)range + 1.0D);
        int i2 = MathHelper.floor(this.y - (double)range - 1.0D);
        int i1 = MathHelper.floor(this.y + (double)range + 1.0D);
        int j2 = MathHelper.floor(this.z - (double)range - 1.0D);
        int j1 = MathHelper.floor(this.z + (double)range + 1.0D);
        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this.exploder, new AxisAlignedBB((double)k1, (double)i2, (double)j2, (double)l1, (double)i1, (double)j1));
        net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.world, this.explosion, list, range);
        Vec3d vec3d = new Vec3d(this.x, this.y, this.z);

        for (Entity entity : list)
        {
            if (!entity.isImmuneToExplosions())
            {
                double distance = entity.getDistance(this.x, this.y, this.z);
                if (distance <= range)
                {
                    Vec3d entityPos = entity.getPositionVector().add(0, entity.getEyeHeight() / 2, 0);
                    if (world.rayTraceBlocks(vec3d, entityPos, false, true, false) == null) {
                        double scale = Math.pow(1.0 - (distance / range), 2.0);
                        entity.attackEntityFrom(DamageSource.causeExplosionDamage(this.explosion), 
                            this.damage * (float)scale);
                            
                        double d5 = entity.posX - this.x;
                        double d7 = entity.posY + (double)entity.getEyeHeight() - this.y;
                        double d9 = entity.posZ - this.z;
                        double d13 = (double)MathHelper.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
                        if (d13 != 0.0D)
                        {
                            d5 = d5 / d13;
                            d7 = d7 / d13;
                            d9 = d9 / d13;
                            
                            double knockbackStrength = scale * this.knockback;
                            entity.motionX += d5 * knockbackStrength;
                            entity.motionY += d7 * knockbackStrength;
                            entity.motionZ += d9 * knockbackStrength;

                            if (entity instanceof EntityPlayer)
                            {
                                EntityPlayer entityplayer = (EntityPlayer)entity;
                                if (!entityplayer.isSpectator() && (!entityplayer.isCreative() || !entityplayer.capabilities.isFlying))
                                {
                                    this.playerKnockbackMap.put(entityplayer, new Vec3d(d5 * knockbackStrength, d7 * knockbackStrength, d9 * knockbackStrength));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void doExplosionB(boolean spawnParticles)
    {
        //this.world.playSound((EntityPlayer)null, this.x, this.y, this.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);

        if (this.size >= 2.0F && this.damagesTerrain)
        {
            this.world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
        }
        else
        {
            this.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
        }

        if (this.damagesTerrain)
        {
            for (BlockPos blockpos : this.affectedBlockPositions)
            {
                IBlockState iblockstate = this.world.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if (spawnParticles)
                {
                    double d0 = (double)((float)blockpos.getX() + this.world.rand.nextFloat());
                    double d1 = (double)((float)blockpos.getY() + this.world.rand.nextFloat());
                    double d2 = (double)((float)blockpos.getZ() + this.world.rand.nextFloat());
                    double d3 = d0 - this.x;
                    double d4 = d1 - this.y;
                    double d5 = d2 - this.z;
                    double d6 = (double)MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                    d3 = d3 / d6;
                    d4 = d4 / d6;
                    d5 = d5 / d6;
                    double d7 = 0.5D / (d6 / (double)this.size + 0.1D);
                    d7 = d7 * (double)(this.world.rand.nextFloat() * this.world.rand.nextFloat() + 0.3F);
                    d3 = d3 * d7;
                    d4 = d4 * d7;
                    d5 = d5 * d7;
                    if(!this.world.isRemote && this.world instanceof WorldServer) {
                        WorldServer worldServer = (WorldServer) this.world;
                        worldServer.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, (d0 + this.x) / 2.0D, (d1 + this.y) / 2.0D, (d2 + this.z) / 2.0D, d3, d4, d5);
                        worldServer.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d0, d1, d2, d3, d4, d5);
                    }
                    
                }

                if (iblockstate.getMaterial() != Material.AIR)
                {
                    if (this.allowBlockDrops && block.canDropFromExplosion(this.explosion))
                    {
                        block.dropBlockAsItemWithChance(this.world, blockpos, this.world.getBlockState(blockpos), 1.0F / this.size, 0);
                    }

                    block.onBlockExploded(this.world, blockpos, this.explosion);
                }
            }
        }

        if (this.causesFire)
        {
            for (BlockPos blockpos1 : this.affectedBlockPositions)
            {
                if (this.world.getBlockState(blockpos1).getMaterial() == Material.AIR && this.world.getBlockState(blockpos1.down()).isFullBlock() && this.random.nextInt(3) == 0)
                {
                    this.world.setBlockState(blockpos1, Blocks.FIRE.getDefaultState());
                }
            }
        }
    }

    public Map<EntityPlayer, Vec3d> getPlayerKnockbackMap()
    {
        return this.playerKnockbackMap;
    }

    @Nullable
    public EntityLivingBase getExplosivePlacedBy()
    {
        if (this.exploder == null)
        {
            return null;
        }
        else if (this.exploder instanceof EntityTNTPrimed)
        {
            return ((EntityTNTPrimed)this.exploder).getTntPlacedBy();
        }
        else
        {
            return this.exploder instanceof EntityLivingBase ? (EntityLivingBase)this.exploder : null;
        }
    }

    public void clearAffectedBlockPositions()
    {
        this.affectedBlockPositions.clear();
    }

    public List<BlockPos> getAffectedBlockPositions()
    {
        return this.affectedBlockPositions;
    }

    public Vec3d getPosition(){ return this.position; }
}
