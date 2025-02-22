package safx.client.particle;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public abstract class SAFXType {
	public String name;
	boolean isList = false;
	
	public abstract List<SAParticleSystem> createParticleSystems(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ);
	public abstract List<SAParticleSystem> createParticleSystemsOnEntity(Entity ent);
	public abstract List<SAParticleSystem> createParticleSystemsOnParticle(World worldIn, SAParticle part);
	//public abstract List<SAParticleSystem> createParticleSystemsOnEntityItemAttached(Entity ent, EnumHand hand);
}
