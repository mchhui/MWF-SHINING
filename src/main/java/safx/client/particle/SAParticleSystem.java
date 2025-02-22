package safx.client.particle;

import java.util.List;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import safx.client.ClientProxy;
import safx.client.particle.SAParticleSystemType.DirResult;
import safx.util.EntityCondition;
import safx.util.MathUtil;
import safx.util.Vec3dr;

/**
 * A particle system which spawns particles
 */
@SideOnly(Side.CLIENT)
public class SAParticleSystem extends Particle implements ISAParticle {
	
	public SAParticleSystemType type;
	
	public EntityCondition condition = EntityCondition.NONE;
	
//	public double posX;
//	public double posY;
//	public double posZ;
//	
//	public double motionX;
//	public double motionY;
//	public double motionZ;
	
	int systemLifetime;
	int spawnDelay = 0;
	
	public float scale =1.0f; //global scale
	public float rotationYaw;
	public float rotationPitch;
	
	public float prevRotationYaw;
	public float prevRotationPitch;
	
	public int initialDelay;
	public int ticksExisted = 0;
	
	public float startSizeRate;
	public float startSizeRateDamping;
	public float startSize = 0.0f;
	
	protected SAParticleStreak prevParticle = null;
	
//	private long timediff = 0;
	
	Entity entity; //parent entity (if attached to an entity)
	public boolean attachToHead = false;
	public Vec3d entityOffset = null;
	SAParticle parent; //parent particle (if attached to a particle)
	
	protected boolean itemAttached=false;
	
	public SAParticleSystem(World worldIn, SAParticleSystemType type, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
		//super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
		super(worldIn, xCoordIn + type.offset.x, yCoordIn+type.offset.y, zCoordIn+type.offset.z);
		this.motionX = xSpeedIn;
		this.motionY = ySpeedIn;
		this.motionZ = zSpeedIn;
		this.type = type;
		this.init();
	}
	
	public SAParticleSystem(Entity entity, SAParticleSystemType type) {
		this(entity.world, type, entity.posX,entity.posY,entity.posZ,0,0,0);
		this.entity = entity;
	}
		

	public SAParticleSystem(World worldIn,SAParticle part, SAParticleSystemType type) {
		this(worldIn, type, part.posX(), part.posY(), part.posZ(),0,0,0);
		this.parent = part;
		//System.out.println("Spawn attached system : " + type.name);
	}

	public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
		//This is a ParticleSystem which spawns the actual particles. Don't render anything
	}
	
	protected void init() {
		this.systemLifetime = MathUtil.randomInt(this.rand, type.systemLifetimeMin, type.systemLifetimeMax);
		this.initialDelay = MathUtil.randomInt(this.rand, type.initialDelayMin, type.initialDelayMax);
		this.startSizeRate = MathUtil.randomFloat(this.rand, type.startSizeRateMin, type.startSizeRateMax) * this.scale;
		this.startSizeRateDamping = MathUtil.randomFloat(this.rand, type.startSizeRateDampingMin, type.startSizeRateDampingMax);
		//Minecraft.getMinecraft().effectRenderer.addEffect(this);
		//timediff = System.currentTimeMillis();
	}
	public void onUpdate() {	
//		if (this.ticksExisted == 0) {
//			System.out.println("Timediff: "+(System.currentTimeMillis()-timediff));
//		}
		this.prevRotationPitch = this.rotationPitch;
		this.prevRotationYaw = this.rotationYaw;
		
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		
		if (type == null) {
			this.setExpired();
			return;
		}
		
		if (this.entity!=null && !this.itemAttached){		
	    	if (!condition.evaluate(entity)) {
	    		this.setExpired();
	    		return;
	    	}
			if (this.attachToHead && entity instanceof EntityLivingBase) {
				EntityLivingBase elb = (EntityLivingBase) entity;
				this.rotationPitch=elb.rotationPitch;
				this.rotationYaw=elb.rotationYawHead;
				Vec3dr offsetr = type.offsetr;
				if (this.entityOffset != null) offsetr = offsetr.add1(this.entityOffset);
				offsetr = offsetr.rotatePitch((float) (-elb.rotationPitch*MathUtil.D2R));
				offsetr = offsetr.rotateYaw((float) ((-elb.rotationYawHead)*MathUtil.D2R));		
				
				this.posX = elb.prevPosX + offsetr.x;
				this.posY = elb.prevPosY + /*elb.getEyeHeight() + */offsetr.y;
				this.posZ = elb.prevPosZ + offsetr.z;
			}else {
				this.rotationPitch=entity.rotationPitch;
				this.rotationYaw=entity.rotationYaw;
				Vec3d offset = type.offset;
				if (this.entityOffset != null) offset = offset.add(this.entityOffset);
				
				offset = offset.rotatePitch((float) (-entity.rotationPitch*MathUtil.D2R));
				offset = offset.rotateYaw((float) ((-entity.rotationYaw)*MathUtil.D2R));		
				
				this.posX = entity.prevPosX + offset.x;
				this.posY = entity.prevPosY + offset.y;
				this.posZ = entity.prevPosZ + offset.z;
			}
			this.motionX = entity.motionX;
			this.motionY = entity.motionY;
			this.motionZ = entity.motionZ;
			
		} else if (this.parent != null) {
			this.posX=parent.posX() + type.offset.x;
			this.posY=parent.posY() + type.offset.y;
			this.posZ=parent.posZ() + type.offset.z;
		} else {
			this.posX+=motionX;
			this.posY+=motionY;
			this.posZ+=motionZ;
		}
		
		if (initialDelay-- > 0) {
			return;
		}
		
		this.startSize = this.startSize+this.startSizeRate;
		this.startSizeRate = (this.startSizeRate * this.startSizeRateDamping);
		
		if (spawnDelay-- <= 0) {
			int count = MathUtil.randomInt(rand, type.particleCountMin, type.particleCountMax);

			for (int i = 0; i < count; i++) {
				//Get position and motion data
				DirResult dir = this.type.new DirResult();
				Vec3d position = type.volumeType.getPosition(this, dir, i, count);
				//System.out.printf("Dir: %.3f,  %.3f,  %.3f\n", dir[0], dir[1], dir[2]);
				//position = position.addVector(type.offset.x, type.offset.y, type.offset.z);
				Vec3d motion = type.velocityType.getVelocity(this, dir.values);
				
				motion = motion.scale(this.scale);
				position = position.scale(this.scale);
				
				//apply ParticleSystem's entity rotation
				
				//System.out.println("ParticleSystemRotation - Pitch: "+this.rotationPitch+" - Yaw: "+this.rotationYaw);
				
				//System.out.println(String.format("Entity pitch : %.3f,  yaw : %.3f", this.rotationPitch, this.rotationYaw));				
				
				motion = motion.rotatePitch((float) (-this.rotationPitch*MathUtil.D2R));
				motion = motion.rotateYaw((float) ((-this.rotationYaw)*MathUtil.D2R));
				if (type.volumeType != SAParticleSystemType.VOL_TRAIL) {
					position = position.rotatePitch((float) (-this.rotationPitch*MathUtil.D2R));
					position = position.rotateYaw((float) ((-this.rotationYaw)*MathUtil.D2R));		
				}
				//System.out.println("Motion: ("+motion.x+ ", "+motion.y + ", "+ motion.z+")");
				//System.out.println("Position: ("+position.x+ ", "+position.y + ", "+ position.z+")");
				
				//Spawn particle
				double mf = 0.05D; //Per Second instead of Per Tick
				SAParticle particle=null;
				if (this.type.streak) {
					SAParticleStreak particleStreak = new SAParticleStreak(this.world, this.posX+position.x, this.posY+position.y, this.posZ+position.z, motion.x*mf, motion.y*mf, motion.z*mf, this);
					if (prevParticle != null) {
						particleStreak.setPrev(prevParticle);
						prevParticle.setNext(particleStreak);
					}
					addEffect(particleStreak);
					prevParticle = particleStreak;
					particle = particleStreak;
				}else {
					if(this.itemAttached) {
						//particle = new SAParticleItemAttached(this.world, this.posX+position.x, this.posY+position.y, this.posZ+position.z, motion.x*mf, motion.y*mf, motion.z*mf, this, this.entity);
					} else {
						particle = new SAParticle(this.world, this.posX+position.x, this.posY+position.y, this.posZ+position.z, motion.x*mf, motion.y*mf, motion.z*mf, this);
					}
					addEffect(particle);
				}
				if (this.type.attachedSystem != null && !this.type.attachedSystem.equals("")) {
					List<SAParticleSystem> systems = SAFX.createFXOnParticle(this.world, particle, this.type.attachedSystem);
					if (systems!=null) {
						for (SAParticleSystem s : systems) {
							s.scale = this.scale;
							addEffect(s);
						}
					}
				}
			}			
			spawnDelay = MathUtil.randomInt(this.rand, type.spawnDelayMin, type.spawnDelayMax);
		}
		
		if (this.entity!=null){
			if (this.entity.isDead){
				this.setExpired();
			} else if ( this.systemLifetime>0 && ticksExisted>= this.systemLifetime){
				this.setExpired();
			}
			
		} else if (this.parent != null) {
			if (!this.parent.isAlive()) {
				this.setExpired();
			}
		}else {
			if (ticksExisted >= this.systemLifetime) {
				this.setExpired();
			}
		}
		ticksExisted++;
	}

	protected void addEffect(ISAParticle s) {
		ClientProxy.get().particleManager.addEffect(s);
	}
	
	/**
	 * Retrieve what effect layer (what texture) the particle should be rendered
	 * with. 0 for the particle sprite sheet, 1 for the main Texture atlas, and 3
	 * for a custom texture
	 */
	public int getFXLayer() {
		return 1;
	}
	
	public double posX() {
		return posX;
	}
	
	public double posY() {
		return posY;
	}
	
	public double posZ() {
		return posZ;
	}
	
	public double motionX() {
		return motionX;
	}
	
	public double motionY() {
		return motionY;
	}
	
	public double motionZ() {
		return motionZ;
	}

	@Override
	public Vec3d getPos() {
		return new Vec3d(this.posX, this.posY, this.posZ);
	}

	@Override
	public boolean shouldRemove() {
		return !this.isAlive();
	}

	@Override
	public void updateTick() {
		this.onUpdate();
	}

	@Override
	public void doRender(BufferBuilder buffer, Entity entityIn, float partialTickTime, float rotX, float rotZ,
			float rotYZ, float rotXY, float rotXZ) {
		this.renderParticle(buffer, entityIn, partialTickTime, rotX, rotZ, rotYZ, rotXY, rotXZ);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox(float partialTickTime, Entity viewEntity) {
		//DOESN'T MATTER, SYSTEM DOES NOT RENDER STUFF ANYWAY
	    float s = 0.5f; 
		return new AxisAlignedBB(this.posX-s, this.posY-s, this.posZ-s, this.posX+s, this.posY+s, this.posZ+s);
	}

	@Override
	public double getDepth() {
		return 0;
	}

	@Override
	public void setDepth(double depth) {
	}

	@Override
	public void setItemAttached() {
	}

	@Override
	public void setExpired() {
		super.setExpired();
		this.entity=null;
	}
	
//ACTUALLY WE DON'T NEED THIS	
//	@SideOnly(Side.CLIENT)
//	public static class Factory implements IParticleFactory {
//		public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, int... parameters) {
//        	if (parameters.length <= 0) return null;
//        	SAParticleSystemType type = SAParticleList.getType(parameters[0]);
//			return new SAParticleSystem(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, type);
//		}
//	}
}