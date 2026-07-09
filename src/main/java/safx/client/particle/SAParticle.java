package safx.client.particle;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.List;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
//import safx.client.models.projectiles.ModelRocket;
import safx.client.particle.SAParticleSystemType.AlphaEntry;
import safx.client.particle.SAParticleSystemType.ColorEntry;
import safx.client.render.SARenderHelper;
import safx.client.render.SARenderHelper.RenderType;
import safx.client.render.particle.SAInstancedParticleShader;
//import safx.client.render.item.RenderItemBase;
import safx.debug.Keybinds;
import safx.SAConfig;
import safx.util.LightCache;
import safx.util.MathUtil;
import net.minecraftforge.fml.common.Loader;
import safx.client.ClientProxy;

import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
/**
 * An actual spawned particle
 */
@SideOnly(Side.CLIENT)
public class SAParticle extends Particle implements ISAParticle {
	
	 protected static final VertexFormat VERTEX_FORMAT = (new VertexFormat()).addElement(DefaultVertexFormats.POSITION_3F).addElement(DefaultVertexFormats.TEX_2F).addElement(DefaultVertexFormats.COLOR_4UB).addElement(DefaultVertexFormats.TEX_2S).addElement(DefaultVertexFormats.NORMAL_3B).addElement(DefaultVertexFormats.PADDING_1B);
	   
	
//	public double posX;
//	public double posY;
//	public double posZ;
	
	int lifetime;
	
	float angle;
	float angleRate;
	float angleRateDamping;
	
	float size;
	float sizePrev;
	float sizeRate;
	float sizeRateDamping;
	
	float animationSpeed;
	
	double velX;
	double velY;
	double velZ;
	float velocityDamping;
	float velocityDampingOnGround;
	
	float systemVelocityFactor;
	
	SAParticleSystem particleSystem;
	SAParticleSystemType type;
	
	int variationFrame;
	
	protected double depth;
	
	protected boolean itemAttached=false;
	protected Vec3d surfaceNormal = null;
	protected int blockHitSpawnCount = 0;
	protected int blockHitCooldownTicks = 0;
	protected int remainingBlockHitChainBudget = 0;
	protected static final double SURFACE_RENDER_OFFSET = 0.01D;

	private final double[] quadLocal = new double[12];
	private final double[] axisScratch = new double[3];
	
	//int angle;

	public SAParticle(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn,
			double ySpeedIn, double zSpeedIn, SAParticleSystem particleSystem) {
		super(worldIn, xCoordIn, yCoordIn, zCoordIn);
		this.motionX = xSpeedIn;
		this.motionY = ySpeedIn;
		this.motionZ = zSpeedIn;

		this.particleSystem = particleSystem;
		this.type = particleSystem.type;
		this.init();
		}
		
		private void init() {
			this.lifetime = MathUtil.randomInt(rand, type.lifetimeMin, type.lifetimeMax);
			this.particleMaxAge = lifetime;
			this.size = MathUtil.randomFloat(rand, type.sizeMin, type.sizeMax) * this.particleSystem.scale;
			this.size+= (this.particleSystem.startSize);
			this.sizeRate = MathUtil.randomFloat(rand, type.sizeRateMin, type.sizeRateMax)  * this.particleSystem.scale;
			this.sizeRateDamping = MathUtil.randomFloat(rand, type.sizeRateDampingMin, type.sizeRateDampingMax);
			this.animationSpeed = MathUtil.randomFloat(rand, type.animationSpeedMin, type.animationSpeedMax);
			this.velocityDamping = MathUtil.randomFloat(rand, type.velocityDampingMin, type.velocityDampingMax);
			this.systemVelocityFactor = MathUtil.randomFloat(rand, type.systemVelocityFactorMin, type.systemVelocityFactorMax);
		    this.velocityDampingOnGround = MathUtil.randomFloat(rand, type.velocityDampingOnGroundMin, type.velocityDampingOnGroundMax);
			
		    this.angle = MathUtil.randomFloat(rand, type.angleMin, type.angleMax);
		    this.angleRate = MathUtil.randomFloat(rand, type.angleRateMin, type.angleRateMax);
		    this.angleRateDamping = MathUtil.randomFloat(rand, type.angleRateDampingMin, type.angleRateDampingMax);
		    
		    //System.out.printf("###INIT:Motion1=(%.2f / %.2f / %.2f)\n",this.motionX, this.motionY, this.motionZ);
		    
			this.motionX+=(systemVelocityFactor*particleSystem.motionX());
			this.motionY+=(systemVelocityFactor*particleSystem.motionY());
			this.motionZ+=(systemVelocityFactor*particleSystem.motionZ());
			
			//System.out.printf("###INIT:Motion=(%.2f / %.2f / %.2f)\n",this.motionX, this.motionY, this.motionZ);
			////System.out.println("###INIT:VelType="+this.type.velocityType.toString());
			//System.out.printf("###INIT:Type.VelocityData=[%.2f, %.2f, %.2f]\n",this.type.velocityDataMin[0], this.type.velocityDataMin[1], this.type.velocityDataMin[2]);
			
			this.velX = this.motionX;
			this.velY = this.motionY;
			this.velZ = this.motionZ;
			
			this.variationFrame = rand.nextInt(type.frames);
			if (this.particleSystem != null && this.particleSystem.getSurfaceNormal() != null) {
				Vec3d n = this.particleSystem.getSurfaceNormal();
				if (n.lengthSquared() > 1.0E-6) {
					this.surfaceNormal = n.normalize();
				}
			}
			int inheritedBudget = this.particleSystem != null ? this.particleSystem.getInheritedBlockHitChainBudget() : -1;
			if (inheritedBudget >= 0) {
				this.remainingBlockHitChainBudget = inheritedBudget;
			} else {
				this.remainingBlockHitChainBudget = Math.max(0, type.blockHitChainBudget);
			}
			
//			if (type.randomRotation) {
//				angle = rand.nextInt(4);
//			}
		}
	

    public void onUpdate()
    {

		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		this.sizePrev = this.size;

		lifetime--;
		if (this.particleAge++ >= this.particleMaxAge) {
			this.setExpired();
			return;
		}
		
		/*---
		 * Move with System
		 */
		if (this.type.particlesStickToSystem) {
			
			if (this.particleSystem.entity != null) {
				
				if (!this.particleSystem.entity.isEntityAlive()) {
					this.setExpired();
					return;
				}
				
				if (this.type.particlesMoveWithSystem && this.particleSystem.attachToHead && this.particleSystem.entity instanceof EntityLivingBase) {
					EntityLivingBase ent = (EntityLivingBase)this.particleSystem.entity;
					
					double p = ent.rotationPitch*MathUtil.D2R;
					double y = ent.rotationYawHead*MathUtil.D2R;
					
					double prevP = ent.prevRotationPitch * MathUtil.D2R;
					double prevY =ent.prevRotationYawHead * MathUtil.D2R;
					
					Vec3d offsetBase = this.particleSystem.entityOffset.add(this.particleSystem.type.offset);
					
					//ViewBobbing
					/*if (this.particleSystem.entity == Minecraft.getMinecraft().player
							&& Minecraft.getMinecraft().gameSettings.thirdPersonView == 0
							&& Minecraft.getMinecraft().gameSettings.viewBobbing) {
						Vec3d vec = setupViewBobbing(1.0f).scale(2.0);
						offsetBase = offsetBase.add(vec);
					}
					*/
					Vec3d offset = offsetBase.rotatePitch((float)-p);
					offset = offset.rotateYaw((float)-y);
					
					Vec3d offsetP = offsetBase.rotatePitch((float)-prevP);
					offsetP = offsetP.rotateYaw((float)-prevY);
										
					this.prevPosX = this.particleSystem.entity.prevPosX + offsetP.x;
					this.prevPosY = this.particleSystem.entity.prevPosY + ent.getEyeHeight() + offsetP.y;
					this.prevPosZ = this.particleSystem.entity.prevPosZ + offsetP.z;
					this.posX = this.particleSystem.entity.posX + offset.x;
					this.posY = this.particleSystem.entity.posY + ent.getEyeHeight() + offset.y;
					this.posZ = this.particleSystem.entity.posZ + offset.z;
				}else {
				
					this.prevPosX = this.particleSystem.entity.prevPosX;
					this.prevPosY = this.particleSystem.entity.prevPosY;
					this.prevPosZ = this.particleSystem.entity.prevPosZ;
					this.posX = this.particleSystem.entity.posX;
					this.posY = this.particleSystem.entity.posY;
					this.posZ = this.particleSystem.entity.posZ;
				}
			}else {
				
				if (!this.particleSystem.isAlive()) {
					this.setExpired();
					return;
				}
				
				this.posX = this.particleSystem.posX();
				this.posY = this.particleSystem.posY();
				this.posZ = this.particleSystem.posZ();
			}	
			
		}else if (this.type.particlesMoveWithSystem) {		
			double dP = (this.particleSystem.rotationPitch - this.particleSystem.prevRotationPitch)*MathUtil.D2R;
			double dY = (this.particleSystem.rotationYaw - this.particleSystem.prevRotationYaw)*MathUtil.D2R;
			
			Vec3d pos = new Vec3d(this.posX,  this.posY, this.posZ);
			Vec3d sysPos = new Vec3d(this.particleSystem.posX(), this.particleSystem.posY(), this.particleSystem.posZ());
			
			Vec3d offset = sysPos.subtract(pos);
			offset = offset.rotateYaw((float)-dY);
			offset = offset.rotatePitch((float)-dP);
			
			Vec3d motion = new Vec3d (this.motionX, this.motionY, this.motionZ);
			motion = motion.rotateYaw((float)-dY);
			motion = motion.rotatePitch((float)-dP);
			
			this.posX = sysPos.x+offset.x;
			this.posY = sysPos.y+offset.y;
			this.posZ = sysPos.z+offset.z;
			
			this.motionX = motion.x;
			this.motionY = motion.y;
			this.motionZ = motion.z;
		
		}
		
		
		/* -------------
		 * MOTION
		 */

		this.motionX = velX;
		this.motionY = velY;
		this.motionZ = velZ;
		this.motionY -= type.gravity; //(0.05d * (double) type.gravity * (double) this.ticksExisted);		
		if (this.blockHitCooldownTicks > 0) {
			this.blockHitCooldownTicks--;
		}
		Vec3d start = new Vec3d(this.posX, this.posY, this.posZ);
		Vec3d next = start.add(this.motionX, this.motionY, this.motionZ);
		boolean handledBlockHit = false;
		if (type.blockHitAffect) {
			handledBlockHit = handleBlockCollision(start, next);
			if (this.isExpired) {
				return;
			}
			if (handledBlockHit) {
				next = this.getPos();
			}
		}
		this.setPosition(next.x, next.y, next.z);
		
		
		this.velX *= velocityDamping;
		this.velY *= velocityDamping;
		this.velZ *= velocityDamping;

		if (this.onGround || (handledBlockHit && this.surfaceNormal != null && this.surfaceNormal.y > 0.5)) {
			this.velX *= velocityDampingOnGround;
			this.velY *= velocityDampingOnGround; // ?
			this.velZ *= velocityDampingOnGround;
			if (type.removeOnGround)
				this.setExpired();
		}

		/* ------------
		 * SIZE
		 */
		size = Math.max(0.0f, size+sizeRate);
		sizeRate *= sizeRateDamping;
		
		/*
		 * ANGLE
		 */
		angle = (angle + angleRate) % 360.0f;
		angleRate *= angleRateDamping;
    }
    
    
	 /**
     * Renders the particle
     */
    public void renderParticle(BufferBuilder buffer, Entity playerIn, float partialTickTime, float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ)
    {
    	float progress = ((float)this.particleAge+partialTickTime) / (float)this.particleMaxAge;
    	
    	preRenderStep(progress);   	
    	
		/*-------------------
		 * ANIMATION
		 */	
		int currentFrame = 0;
        if (type.hasVariations) {
        	currentFrame = variationFrame;
        }else {
        	currentFrame = ((int)((float)type.frames*(progress * this.animationSpeed))) % type.frames;
        }
    	
    	/* -------------
         * RENDER PARTICLE
         */
        this.particleScale = sizePrev + (size-sizePrev)*partialTickTime;
//        Minecraft.getMinecraft().getTextureManager().bindTexture(type.texture);
        float fscale = 0.1F * this.particleScale;
        float fPosX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTickTime - (!this.itemAttached ? SAParticleManager.interpPosX :0));
        float fPosY = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTickTime - (!this.itemAttached ? SAParticleManager.interpPosY :0));
        float fPosZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTickTime - (!this.itemAttached ? SAParticleManager.interpPosZ :0));
        if (this.type.surfaceAligned && this.surfaceNormal != null) {
        	fPosX += (float)(this.surfaceNormal.x * SURFACE_RENDER_OFFSET);
        	fPosY += (float)(this.surfaceNormal.y * SURFACE_RENDER_OFFSET);
        	fPosZ += (float)(this.surfaceNormal.z * SURFACE_RENDER_OFFSET);
        }
		int col = currentFrame % type.columns;
		int row = (currentFrame / type.columns);
		float u = 1.f/type.columns;
		float v = 1.f/type.rows; 
		float U1 = col*u;
		float V1 = row*v;
		float U2 = (col+1)*u;
		float V2 = (row+1)*v;
		float ua, va, ub, vb, uc, vc, ud, vd;
		ua=U2; va=V2; ub = U2; vb= V1; uc = U1; vc = V1; ud=U1; vd = V2;
//		enableBlendMode();
//        buffer.begin(7, VERTEX_FORMAT);
        double a = (angle + (partialTickTime * angleRate)) * MathUtil.D2R;
		
        float aspect = (float)type.rows / (float)type.columns;
        float fscaleX = fscale;
        float fscaleY = fscale;
        if (aspect > 1.0f) {
            fscaleY = fscale / aspect;
        } else {
            fscaleX = fscale * aspect;
        }
        
		if (this.type.surfaceAligned && this.surfaceNormal != null && shouldUseSurfaceWallAlign(this.surfaceNormal, this.type.surfaceAlignMode)) {
			this.buildWallAlignedQuadIntoScratch(this.surfaceNormal, fscaleX, fscaleY, a);
		} else if (this.type.groundAligned || (this.type.surfaceAligned && this.surfaceNormal != null)) {
			float sx = fscaleX;
			float sz = fscaleY;
			this.putQuadVertex(0, -sx, 0, -sz);
			this.putQuadVertex(1, sx, 0, -sz);
			this.putQuadVertex(2, sx, 0, sz);
			this.putQuadVertex(3, -sx, 0, sz);
			if (a > 0.0001f) {
				double sinA = Math.sin(a);
				double cosA = Math.cos(a);
				for (int vi = 0; vi < 4; vi++) {
					this.rotateQuadVertexY(vi, sinA, cosA);
				}
			}
		} else {
	        this.putQuadVertex(0, -rotX * fscaleX - rotXY * fscaleY, -rotZ * fscaleY, -rotYZ * fscaleX - rotXZ * fscaleY);
	        this.putQuadVertex(1, -rotX * fscaleX + rotXY * fscaleY, rotZ * fscaleY, -rotYZ * fscaleX + rotXZ * fscaleY);
	        this.putQuadVertex(2, rotX * fscaleX + rotXY * fscaleY, rotZ * fscaleY, rotYZ * fscaleX + rotXZ * fscaleY);
	        this.putQuadVertex(3, rotX * fscaleX - rotXY * fscaleY, -rotZ * fscaleY, rotYZ * fscaleX - rotXZ * fscaleY);
	        if (a > 0.0001d) {
	        	this.computeBillboardRotationAxis();
				double cosa = Math.cos(a);
				double sina = Math.sin(a);
				double ax = axisScratch[0];
				double ay = axisScratch[1];
				double az = axisScratch[2];
		        for (int vi = 0; vi < 4; vi++) {
		        	this.applyRotAxisToVertex(vi, ax, ay, az, sina, cosa);
		        }
	        }
		}
		int packedLight = this.getBrightnessForRender(partialTickTime);
		int lmU = packedLight >> 16 & 65535;
		int lmV = packedLight & 65535;
		this.writeQuad(buffer, fPosX, fPosY, fPosZ, ua, va, ub, vb, uc, vc, ud, vd, lmU, lmV);
    }

	public boolean canUseInstancedRender() {
		if (this.type == null || this.itemAttached || this.type.streak) {
			return false;
		}
		return true;
	}

	public boolean packInstanced(FloatBuffer buffer, float partialTickTime) {
		if (!this.canUseInstancedRender()) {
			return false;
		}
		if (buffer.remaining() < SAInstancedParticleShader.INSTANCE_FLOATS) {
			return false;
		}
		float progress = ((float) this.particleAge + partialTickTime) / (float) this.particleMaxAge;
		this.preRenderStep(progress);
		int currentFrame;
		if (this.type.hasVariations) {
			currentFrame = this.variationFrame;
		} else {
			currentFrame = ((int) ((float) this.type.frames * (progress * this.animationSpeed))) % this.type.frames;
		}
		float scale = this.sizePrev + (this.size - this.sizePrev) * partialTickTime;
		float fscale = 0.1F * scale;
		float fPosX = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTickTime - SAParticleManager.interpPosX);
		float fPosY = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTickTime - SAParticleManager.interpPosY);
		float fPosZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTickTime - SAParticleManager.interpPosZ);
		float renderMode = 0.0f;
		float nx = 0.0f;
		float ny = 0.0f;
		float nz = 0.0f;
		if (this.type.surfaceAligned && this.surfaceNormal != null) {
			fPosX += (float) (this.surfaceNormal.x * SURFACE_RENDER_OFFSET);
			fPosY += (float) (this.surfaceNormal.y * SURFACE_RENDER_OFFSET);
			fPosZ += (float) (this.surfaceNormal.z * SURFACE_RENDER_OFFSET);
			if (this.shouldUseSurfaceWallAlign(this.surfaceNormal, this.type.surfaceAlignMode)) {
				renderMode = 2.0f;
				nx = (float) this.surfaceNormal.x;
				ny = (float) this.surfaceNormal.y;
				nz = (float) this.surfaceNormal.z;
			} else {
				renderMode = 1.0f;
			}
		} else if (this.type.groundAligned) {
			renderMode = 1.0f;
		}
		float aspect = (float) this.type.rows / (float) this.type.columns;
		float fscaleX = fscale;
		float fscaleY = fscale;
		if (aspect > 1.0f) {
			fscaleY = fscale / aspect;
		} else {
			fscaleX = fscale * aspect;
		}
		int col = currentFrame % this.type.columns;
		int row = currentFrame / this.type.columns;
		float cellU = 1.0f / this.type.columns;
		float cellV = 1.0f / this.type.rows;
		float u0 = col * cellU;
		float u1 = (col + 1) * cellU;
		float v0 = row * cellV;
		float v1 = (row + 1) * cellV;
		int packedLight = this.getBrightnessForRender(partialTickTime);
		float lmU = packedLight >> 16 & 65535;
		float lmV = packedLight & 65535;
		float angleRad = (float) ((this.angle + partialTickTime * this.angleRate) * MathUtil.D2R);
		buffer.put(fPosX).put(fPosY).put(fPosZ).put(renderMode);
		buffer.put(fscaleX).put(fscaleY).put(u0).put(u1);
		buffer.put(v0).put(v1).put(this.particleRed).put(this.particleGreen);
		buffer.put(this.particleBlue).put(this.particleAlpha).put(lmU).put(lmV);
		buffer.put(nx).put(ny).put(nz).put(angleRad);
		return true;
	}

	private void putQuadVertex(int vertex, double x, double y, double z) {
		int i = vertex * 3;
		this.quadLocal[i] = x;
		this.quadLocal[i + 1] = y;
		this.quadLocal[i + 2] = z;
	}

	private void rotateQuadVertexY(int vertex, double sinA, double cosA) {
		int i = vertex * 3;
		double x = this.quadLocal[i];
		double z = this.quadLocal[i + 2];
		this.quadLocal[i] = x * cosA - z * sinA;
		this.quadLocal[i + 2] = x * sinA + z * cosA;
	}

	private void computeBillboardRotationAxis() {
		double x0 = this.quadLocal[0];
		double y0 = this.quadLocal[1];
		double z0 = this.quadLocal[2];
		double x1 = this.quadLocal[3];
		double y1 = this.quadLocal[4];
		double z1 = this.quadLocal[5];
		double len0 = Math.sqrt(x0 * x0 + y0 * y0 + z0 * z0);
		double len1 = Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
		if (len0 > 1.0E-6D) {
			x0 /= len0;
			y0 /= len0;
			z0 /= len0;
		}
		if (len1 > 1.0E-6D) {
			x1 /= len1;
			y1 /= len1;
			z1 /= len1;
		}
		this.axisScratch[0] = y0 * z1 - z0 * y1;
		this.axisScratch[1] = z0 * x1 - x0 * z1;
		this.axisScratch[2] = x0 * y1 - y0 * x1;
	}

	private void applyRotAxisToVertex(int vertex, double ax, double ay, double az, double sina, double cosa) {
		int i = vertex * 3;
		double px = this.quadLocal[i];
		double py = this.quadLocal[i + 1];
		double pz = this.quadLocal[i + 2];
		double v1x = ay * pz - az * py;
		double v1y = az * px - ax * pz;
		double v1z = ax * py - ay * px;
		double d1 = ax * px + ay * py + az * pz;
		double oneMinusCosa = 1.0D - cosa;
		this.quadLocal[i] = px * cosa + v1x * sina + ax * d1 * oneMinusCosa;
		this.quadLocal[i + 1] = py * cosa + v1y * sina + ay * d1 * oneMinusCosa;
		this.quadLocal[i + 2] = pz * cosa + v1z * sina + az * d1 * oneMinusCosa;
	}

	private void writeQuad(BufferBuilder buffer, float fPosX, float fPosY, float fPosZ,
			float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd,
			int lmU, int lmV) {
		buffer.pos(this.quadLocal[0] + fPosX, this.quadLocal[1] + fPosY, this.quadLocal[2] + fPosZ)
				.tex(ua, va).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
				.lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
		buffer.pos(this.quadLocal[3] + fPosX, this.quadLocal[4] + fPosY, this.quadLocal[5] + fPosZ)
				.tex(ub, vb).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
				.lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
		buffer.pos(this.quadLocal[6] + fPosX, this.quadLocal[7] + fPosY, this.quadLocal[8] + fPosZ)
				.tex(uc, vc).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
				.lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
		buffer.pos(this.quadLocal[9] + fPosX, this.quadLocal[10] + fPosY, this.quadLocal[11] + fPosZ)
				.tex(ud, vd).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
				.lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
	}
    
    /**
     * interpolate colors and alpha values
     */
    protected void preRenderStep(float progress) {
    	
		/* ------------------------
		 * INTERPOLATE COLOR VALUES
		 */
    	
		ColorEntry c1 = null;
		ColorEntry c2 = null;
    	if (type.colorEntries.size()==0) {
    		c1 =new ColorEntry(1.0f,1.0f,1.0f,0);
    		c2 = c1;
    	}else if (type.colorEntries.size() == 1) {
    		c1 = type.colorEntries.get(0);
    		c2 = c1;
    	}else {
    		c1 = type.colorEntries.get(0);
    		for (int i = 1; i < type.colorEntries.size(); i++) {
    			c2 = type.colorEntries.get(i);
				if (progress < c2.time) {
					break;
				}else {
					c1 = c2;
				}
			}
    	}
		float p = (progress-c1.time) / (c2.time-c1.time);		
		if (c1 != c2) {
			
			//RGB to HSB
			float[] hsb1 = Color.RGBtoHSB((int)(c1.r*255), (int)(c1.g*255), (int)(c1.b*255), null);
			float[] hsb2 = Color.RGBtoHSB((int)(c2.r*255), (int)(c2.g*255), (int)(c2.b*255), null);	
			//HSB to RGB;
			Color color = new Color(Color.HSBtoRGB(hsb1[0]*(1f-p) + hsb2[0]*p, hsb1[1]*(1f-p) + hsb2[1]*p, hsb1[2]*(1f-p) + hsb2[2]*p));
			this.particleRed = (float)color.getRed() / 255.0f;
			this.particleGreen = (float)color.getGreen() / 255.0f;
			this.particleBlue = (float)color.getBlue() / 255.0f;
		}else {
			this.particleRed = (float)c1.r;
			this.particleGreen = (float)c1.g;
			this.particleBlue = (float)c1.b;
		}
		
//		if (p > 0.99f)
//			//System.out.println(String.format("R=%.3f, G=%.3f, B=%.3f", this.particleRed, this.particleGreen, this.particleBlue));
		
		/*-------------------------
		 * INTERPOLATE ALPHA VALUES
		 */
		AlphaEntry a1 = null;
		AlphaEntry a2 = null;
		if (type.alphaEntries.size() == 0) {
			this.particleAlpha = 1.0f;
		}else if (type.alphaEntries.size() == 1) {
			a1 = type.alphaEntries.get(0);
			this.particleAlpha = a1.alpha;
		}else {
			a1 = type.alphaEntries.get(0);
    		for (int i = 1; i < type.alphaEntries.size(); i++) {
    			a2 = type.alphaEntries.get(i);
				if (progress < a2.time) {
					break;
				}else {
					a1 = a2;
				}
			}
    		if (a1.time != a2.time) {
    			p = (progress-a1.time) / (a2.time-a1.time);		
    			//interpolate
    			this.particleAlpha = a1.alpha*(1f-p) + a2.alpha * p;
    		}else {
    			this.particleAlpha = a1.alpha;
    		}
		}
		
	
		
//		if (p > 0.99f)
//			//System.out.println(String.format("A=%.3f", this.particleAlpha));
	
        
    }
    
    @Override
    public int getBrightnessForRender(float partialTicks) {
    	if (this.type != null && this.type.renderType == RenderType.ALPHA_SHADED) {
    		if (SAConfig.cl_enableLightCache) {
    			double x = this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks;
    			double y = this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks;
    			double z = this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks;
    			return LightCache.getPackedLight(this.world, x, y, z);
    		}
    		return super.getBrightnessForRender(partialTicks);
    	}
    	// ALPHA / SOLID / NO_Z_TEST：顶点满亮无环境压暗；ADDITIVE / NO_Z_TEST_ADDITIVE 同顶点满亮 + 全局高亮（见 SARenderHelper）
    	return (240 << 16) | 240;
    }
	
	protected void enableBlendMode() {
		if (type.renderType != RenderType.SOLID) {
			GlStateManager.enableBlend();
			GlStateManager.depthMask(false);
		}
		if (type.renderType == RenderType.ALPHA || type.renderType == RenderType.ALPHA_SHADED || type.renderType == RenderType.NO_Z_TEST) {
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		} else if (type.renderType == RenderType.ADDITIVE || type.renderType == RenderType.NO_Z_TEST_ADDITIVE) {
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		}
		if (type.renderType == RenderType.NO_Z_TEST || type.renderType == RenderType.NO_Z_TEST_ADDITIVE) {
			GlStateManager.depthMask(false);
			GlStateManager.disableDepth();
		}
		if (type.renderType == RenderType.ADDITIVE || type.renderType == RenderType.NO_Z_TEST_ADDITIVE) {
			SARenderHelper.enableFXLighting();
		}
	}
	
	protected void disableBlendMode() {
		if (type.renderType == RenderType.ADDITIVE || type.renderType == RenderType.NO_Z_TEST_ADDITIVE) {
			SARenderHelper.disableFXLighting();
		}
		if (type.renderType != RenderType.SOLID) {
			GlStateManager.disableBlend();
			GlStateManager.depthMask(true);
		}
		if (type.renderType == RenderType.ALPHA || type.renderType == RenderType.ALPHA_SHADED || type.renderType == RenderType.NO_Z_TEST) {
			GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		} else if (type.renderType == RenderType.ADDITIVE || type.renderType == RenderType.NO_Z_TEST_ADDITIVE) {
			GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		}
		if (type.renderType == RenderType.NO_Z_TEST || type.renderType == RenderType.NO_Z_TEST_ADDITIVE) {
			GlStateManager.depthMask(true);
			GlStateManager.enableDepth();
		}
	}
    
    
    /**
	 * Retrieve what effect layer (what texture) the particle should be rendered
	 * with. 0 for the particle sprite sheet, 1 for the main Texture atlas, and 3
	 * for a custom texture
	 */
	public int getFXLayer() {
		return 3;
	}

// DON'T NEED THIS
//    @SideOnly(Side.CLIENT)
//    public static class Factory implements IParticleFactory
//    {
//        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, int... parameters)
//        {
//        	if (parameters.length <= 0) return null;
//        	SAParticleSystemType type = SAParticleList.getType(parameters[0]);
//            return new SAParticle(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, type);
//        }
//    }
	
	public double posX() {
		return posX;
	}
	
	public double posY() {
		return posY;
	}
	
	public double posZ() {
		return posZ;
	}

	
	protected Vec3d rotAxis(Vec3d p1, Vec3d axis, double sina, double cosa) {	
		  Vec3d v1 = axis.crossProduct(p1);
		  double d1 = axis.dotProduct(p1);
		  return p1.scale(cosa).add(v1.scale(sina)).add(axis.scale(d1*(1.0 - cosa)));			
		//  return p1.scale(cosa).add(axis.crossProduct(p1).scale(Math.sin(a))).add(axis.scale(axis.dotProduct(p1)*(1.0 - Math.cos(a))));			
	}

	private void buildWallAlignedQuadIntoScratch(Vec3d normal, float sx, float sy, double angleRad) {
		double nx = normal.x;
		double ny = normal.y;
		double nz = normal.z;
		double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (nLen > 1.0E-6D) {
			nx /= nLen;
			ny /= nLen;
			nz /= nLen;
		}
		double hx, hy, hz;
		if (Math.abs(ny) > 0.9D) {
			hx = 1.0D;
			hy = 0.0D;
			hz = 0.0D;
		} else {
			hx = 0.0D;
			hy = 1.0D;
			hz = 0.0D;
		}
		double tx = hy * nz - hz * ny;
		double ty = hz * nx - hx * nz;
		double tz = hx * ny - hy * nx;
		double tLen = Math.sqrt(tx * tx + ty * ty + tz * tz);
		if (tLen > 1.0E-6D) {
			tx /= tLen;
			ty /= tLen;
			tz /= tLen;
		}
		double bx = ny * tz - nz * ty;
		double by = nz * tx - nx * tz;
		double bz = nx * ty - ny * tx;
		double bLen = Math.sqrt(bx * bx + by * by + bz * bz);
		if (bLen > 1.0E-6D) {
			bx /= bLen;
			by /= bLen;
			bz /= bLen;
		}
		if (angleRad > 0.0001d) {
			double cos = Math.cos(angleRad);
			double sin = Math.sin(angleRad);
			double t2x = tx * cos + bx * sin;
			double t2y = ty * cos + by * sin;
			double t2z = tz * cos + bz * sin;
			double b2x = bx * cos - tx * sin;
			double b2y = by * cos - ty * sin;
			double b2z = bz * cos - tz * sin;
			tx = t2x;
			ty = t2y;
			tz = t2z;
			bx = b2x;
			by = b2y;
			bz = b2z;
		}
		this.putQuadVertex(0, tx * -sx + bx * -sy, ty * -sx + by * -sy, tz * -sx + bz * -sy);
		this.putQuadVertex(1, tx * sx + bx * -sy, ty * sx + by * -sy, tz * sx + bz * -sy);
		this.putQuadVertex(2, tx * sx + bx * sy, ty * sx + by * sy, tz * sx + bz * sy);
		this.putQuadVertex(3, tx * -sx + bx * sy, ty * -sx + by * sy, tz * -sx + bz * sy);
	}

	@Override
	public Vec3d getPos() {
		return new Vec3d(this.posX, this.posY, this.posZ);
	}

	@Override
	public double getPosX() {
		return this.posX;
	}

	@Override
	public double getPosY() {
		return this.posY;
	}

	@Override
	public double getPosZ() {
		return this.posZ;
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
	public void doRender(BufferBuilder buffer, Entity playerIn, float partialTickTime, float rotX, float rotZ,
			float rotYZ, float rotXY, float rotXZ) {
		this.renderParticle(buffer, playerIn, partialTickTime, rotX, rotZ, rotYZ, rotXY, rotXZ);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox(float partialTickTime, Entity viewEnt) {
	    //float fPosX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTickTime - interpPosX);
        //float fPosY = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTickTime - interpPosY);
        //float fPosZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTickTime - interpPosZ);
		double fPosX = (this.posX-viewEnt.posX);
		double fPosY = (this.posY-viewEnt.posY);
		double fPosZ = (this.posZ-viewEnt.posZ);
	    
		double s = size*0.5;
		return new AxisAlignedBB(fPosX-s, fPosY-s, fPosZ-s, fPosX+s, fPosY+s, fPosZ+s);
	}

	@Override
	public double getDepth() {
		return this.depth;
	}

	@Override
	public void setDepth(double depth) {
		this.depth=depth;
	}
	
	private Vec3d setupViewBobbing(float ptt)
    {
        if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer)
        {
            EntityPlayer entityplayer = (EntityPlayer)Minecraft.getMinecraft().getRenderViewEntity();
            float f1 = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
            float f2 = -(entityplayer.distanceWalkedModified + f1 * ptt);
            float f3 = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * ptt;
            float f4 = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * ptt;

            float F1 = 1.0f; // (float) Keybinds.X;
            float F2 = 1.0f; //(float) Keybinds.Y;
            
//            GlStateManager.translate(MathHelper.sin(f2 * (float)Math.PI) * f3 * 0.5F * F1, -Math.abs(MathHelper.cos(f2 * (float)Math.PI) * f3) * F2, 0.0F);
//            GlStateManager.rotate(MathHelper.sin(f2 * (float)Math.PI) * f3 * 3.0F, 0.0F, 0.0F, 1.0F);
//            GlStateManager.rotate(Math.abs(MathHelper.cos(f2 * (float)Math.PI - 0.2F) * f3) * 5.0F, 1.0F, 0.0F, 0.0F);
//            GlStateManager.rotate(f4, 1.0F, 0.0F, 0.0F);

            Vec3d vec = new Vec3d(MathHelper.sin(f2 * (float)Math.PI) * f3 * 0.5F * F1,  -Math.abs(MathHelper.cos(f2 * (float)Math.PI) * f3) * F2, 0.0F);
            vec = MathUtil.rotateVec3dAroundZ(vec, MathHelper.sin(f2 * (float)Math.PI) * f3 * 3.0F * (float)MathUtil.D2R);
            return vec.rotatePitch(Math.abs(MathHelper.cos(f2 * (float)Math.PI - 0.2F) * f3) * 5.0F * (float)MathUtil.D2R).rotatePitch(f4 * (float)MathUtil.D2R);
            		
            		
        }else {
        	return new Vec3d(0,0,0);
        }
    }

	@Override
	public void setItemAttached() {
		this.itemAttached=true;
	}

	@Override
	public void setExpired() {
		super.setExpired();
		this.particleSystem=null;
	}
	
	protected boolean handleBlockCollision(Vec3d start, Vec3d next) {
		RayTraceResult trace = this.world.rayTraceBlocks(start, next, false, true, false);
		if (trace == null || trace.typeOfHit != RayTraceResult.Type.BLOCK || trace.sideHit == null) {
			return false;
		}
		EnumFacing side = trace.sideHit;
		Vec3d normal = new Vec3d(side.getXOffset(), side.getYOffset(), side.getZOffset());
		if (normal.lengthSquared() > 1.0E-6) {
			this.surfaceNormal = normal.normalize();
		}
		this.onGround = side == EnumFacing.UP;
		this.setPosition(
				trace.hitVec.x + normal.x * SURFACE_RENDER_OFFSET,
				trace.hitVec.y + normal.y * SURFACE_RENDER_OFFSET,
				trace.hitVec.z + normal.z * SURFACE_RENDER_OFFSET);
		Vec3d v = new Vec3d(this.velX, this.velY, this.velZ);
		double vn = v.dotProduct(normal);
		this.velX = v.x - normal.x * vn;
		this.velY = v.y - normal.y * vn;
		this.velZ = v.z - normal.z * vn;
		this.motionX = this.velX;
		this.motionY = this.velY;
		this.motionZ = this.velZ;
		
		int maxSpawn = type.blockHitSpawnOnce ? 1 : Math.max(0, type.blockHitMaxSpawnCount);
		boolean canSpawn = maxSpawn > 0
				&& this.blockHitSpawnCount < maxSpawn
				&& this.blockHitCooldownTicks <= 0
				&& this.remainingBlockHitChainBudget > 0
				&& type.blockHitSpawnFx != null
				&& !type.blockHitSpawnFx.isEmpty();
		if (canSpawn) {
			spawnBlockHitFx(trace.hitVec, this.surfaceNormal);
			this.blockHitSpawnCount++;
			this.blockHitCooldownTicks = Math.max(0, type.blockHitSpawnCooldownTicks);
		}
		if (type.blockHitKillSelf) {
			this.setExpired();
		}
		return true;
	}
	
	protected void spawnBlockHitFx(Vec3d hitPos, Vec3d normal) {
		if (normal == null) {
			normal = new Vec3d(0, 1, 0);
		}
		double f = type.surfaceNormalVelocityFactor;
		Vec3d spawnPos = hitPos.add(normal.scale(SURFACE_RENDER_OFFSET));
		List<SAParticleSystem> systems = SAFX.createFX(this.world, type.blockHitSpawnFx,
				spawnPos.x, spawnPos.y, spawnPos.z, normal.x * f, normal.y * f, normal.z * f);
		if (systems == null) {
			return;
		}
		int childBudgetBase = Math.max(0, this.remainingBlockHitChainBudget - 1);
		for (SAParticleSystem s : systems) {
			s.scale = this.particleSystem != null ? this.particleSystem.scale : s.scale;
			s.setSurfaceNormal(normal);
			int childBudget = Math.min(childBudgetBase, Math.max(0, s.type.blockHitChainBudget));
			s.setInheritedBlockHitChainBudget(childBudget);
			ClientProxy.get().particleManager.addEffect(s);
		}
	}
	
	protected boolean shouldUseSurfaceWallAlign(Vec3d normal, String mode) {
		String alignMode = mode == null ? "AUTO" : mode;
		switch (alignMode.toUpperCase()) {
		case "WALL":
			return true;
		case "GROUND":
			return false;
		case "AUTO":
		default:
			return Math.abs(normal.y) < 0.5;
		}
	}
	
}
