package safx.client.particle;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class SAParticleListType extends SAFXType {
	ArrayList<ParticleSystemEntry> particleSystems = new ArrayList<ParticleSystemEntry>();

	
	
	public SAParticleListType() {
		isList = true;
	}

	public void addParticleSystem(String particleSystem) {
		particleSystems.add(new ParticleSystemEntry(particleSystem));
	}
	
	class ParticleSystemEntry {
		//ParticleSystemType type; //Actually is just a string
		String particleSystem;
		//TODO: Delay, Offset, Scale, etc.

		public ParticleSystemEntry(String particleSystem) {
			super();
			this.particleSystem = particleSystem;
		}
	}

	
	@Override
	public List<SAParticleSystem> createParticleSystems(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		ArrayList<SAParticleSystem> list = new ArrayList<SAParticleSystem>();
		for (ParticleSystemEntry system : particleSystems) {		
			if (SAFX.FXList.containsKey(system.particleSystem)) {
				SAFXType fxtype = SAFX.FXList.get(system.particleSystem);
				list.addAll(fxtype.createParticleSystems(world, posX, posY, posZ, motionX, motionY, motionZ));
			}
		}
		return list;
	}

	@Override
	public List<SAParticleSystem> createParticleSystemsOnEntity(Entity ent) {
		ArrayList<SAParticleSystem> list = new ArrayList<SAParticleSystem>();
		for (ParticleSystemEntry system : particleSystems) {		
			if (SAFX.FXList.containsKey(system.particleSystem)) {
				SAFXType fxtype = SAFX.FXList.get(system.particleSystem);
				list.addAll(fxtype.createParticleSystemsOnEntity(ent));
			}
		}
		return list;
	}
	
	/*@Override
	public List<SAParticleSystem> createParticleSystemsOnEntityItemAttached(Entity ent, EnumHand hand) {
		ArrayList<SAParticleSystem> list = new ArrayList<SAParticleSystem>();
		for (ParticleSystemEntry system : particleSystems) {		
			if (SAFX.FXList.containsKey(system.particleSystem)) {
				SAFXType fxtype = SAFX.FXList.get(system.particleSystem);
				list.addAll(fxtype.createParticleSystemsOnEntityItemAttached(ent, hand));
			}
		}
		return list;
	}*/

	@Override
	public List<SAParticleSystem> createParticleSystemsOnParticle(World worldIn, SAParticle ent) {
		ArrayList<SAParticleSystem> list = new ArrayList<SAParticleSystem>();
		for (ParticleSystemEntry system : particleSystems) {		
			if (SAFX.FXList.containsKey(system.particleSystem)) {
				SAFXType fxtype = SAFX.FXList.get(system.particleSystem);
				list.addAll(fxtype.createParticleSystemsOnParticle(worldIn, ent));
			}
		}
		return list;
	}
}
