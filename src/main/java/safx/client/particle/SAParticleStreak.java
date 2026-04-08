package safx.client.particle;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import safx.client.ClientProxy;

public class SAParticleStreak extends SAParticle{

	protected SAParticleStreak prev;
	protected SAParticleStreak next;
	
	protected Vec3d pos1; //This streak segment's vertices
	protected Vec3d pos2; 
	
	public boolean enableSmoothing = false;
	public int smoothingSubdivisions = 3;
	
	public SAParticleStreak(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn,
			double ySpeedIn, double zSpeedIn, SAParticleSystem particleSystem) {
		super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, particleSystem);
	}

	
	
	 /**
     * Renders the particle
     */
	@Override
    public void renderParticle(BufferBuilder buffer, Entity playerIn, float partialTickTime, float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ)
    {
    	float progress = ((float)this.particleAge+partialTickTime) / (float)this.particleMaxAge;
    	
//    	System.out.printf("renderParticle: rotX = %.4f,  rotZ = %.4f,  rotYZ = %.4f,  rotXY = %.4f,  rotXZ = %.4f\n", rotX, rotZ, rotYZ, rotXY, rotXZ);
//    	Vec3d View = ClientProxy.get().getPlayerClient().getLook(partialTickTime);
//    	System.out.printf("PlayerView: X = %.4f,  Y = %.4f,  Z = %.4f\n---\n", View.x, View.y, View.z);
    	
    	preRenderStep(progress);

    	int packedLight = this.getBrightnessForRender(partialTickTime);
    	final int lmU = packedLight >> 16 & 65535;
    	final int lmV = packedLight & 65535;
    	
    	if (this.next == null) {
    		this.particleAlpha = 0.0f;
    	}
    	
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

        float fPosX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTickTime - SAParticleManager.interpPosX);
        float fPosY = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTickTime - SAParticleManager.interpPosY);
        float fPosZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTickTime - SAParticleManager.interpPosZ);
        
		int col = currentFrame % type.columns;
		int row = (currentFrame / type.columns);
		
		float u = 1.f/type.columns;
		float v = 1.f/type.columns;
		
		float U1 = col*u;
		float V1 = row*v;
		float U2 = (col+1)*u;
		float V2 = (row+1)*v;
		
		float ua, va, ub, vb, uc, vc, ud, vd;
		ua=U2; va=V2; ub = U2; vb= V1; uc = U1; vc = V1; ud=U1; vd = V2;
		
//		enableBlendMode();
//		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//        buffer.begin(7, VERTEX_FORMAT);
		Vec3d p1, p2, p3, p4;
		
		//System.out.println("streak");
		
		//RotX = yaw
		//RotZ = pitch

//		double x1, x2, x3, x4, z1, z2, z3, z4;
//		x1 = (double)(- rotX * fscale - rotXY * fscale);
//		x2 = (double)(- rotX * fscale + rotXY * fscale);
//		x3 = (double)( rotX * fscale + rotXY * fscale);
//		x4 = (double)( rotX * fscale - rotXY * fscale);
//		z1 = (double)(- rotYZ * fscale - rotXZ * fscale);
//		z2 = (double)( - rotYZ * fscale + rotXZ * fscale);
//		z3 = (double)( + rotYZ * fscale + rotXZ * fscale);
//		z4 =  (double)( + rotYZ * fscale - rotXZ * fscale);
		
        //p1 = new Vec3d((x1+x2)*0.5, (double)(- rotZ * fscale), (z1+z2)*0.5);
        //p2 = new Vec3d((x3+x4)*0.5, (double)( + rotZ * fscale), (z3+x4)*0.5);
        
//		p1 = new Vec3d((x1+x4)*0.5, (double)(- rotZ * fscale), (z1+z4)*0.5);
//        p2 = new Vec3d((x2+x3)*0.5, (double)( + rotZ * fscale), (z2+x3)*0.5);
		
        if (prev == null) {
            this.pos1 = null;
            this.pos2 = null;
        }else {        	
    		Vec3d v_view = ClientProxy.get().getPlayerClient().getLook(partialTickTime);
    		Vec3d v_prev = new Vec3d(prev.posX, prev.posY, prev.posZ).subtract(ClientProxy.get().getPlayerClient().getPositionVector());
    		
    		Vec3d v_dir = v_prev.subtract(fPosX, fPosY, fPosZ).normalize();
            
    		Vec3d v_cross = v_view.crossProduct(v_dir).normalize();
    		
            p1 = new Vec3d(v_cross.x*fscale + fPosX, v_cross.y*fscale  + fPosY, v_cross.z*fscale  + fPosZ);
            p2 = new Vec3d(v_cross.x* -fscale + fPosX, v_cross.y* -fscale  + fPosY, v_cross.z* -fscale  + fPosZ);
        	
            this.pos1 = p1;
            this.pos2 = p2;
            
            float fscaleP = prev.particleScale *0.1f;
            
            if (prev.pos1 != null && prev.pos2 != null) {
            	p3 = prev.pos2;
            	p4 = prev.pos1;
            }else {
            	p4 = new Vec3d(v_cross.x*fscaleP + v_prev.x, v_cross.y*fscaleP  + v_prev.y, v_cross.z*fscaleP  + v_prev.z);
                p3 = new Vec3d(v_cross.x* -fscaleP + v_prev.x, v_cross.y* -fscaleP  + v_prev.y, v_cross.z* -fscaleP  + v_prev.z);
                
                prev.pos1 = p4;
                prev.pos2 = p3;
            }
            
            if (enableSmoothing && prev.prev != null) {
            	renderSmoothedQuad(buffer, p1, p2, p3, p4, v_prev, new Vec3d(fPosX, fPosY, fPosZ), 
            	                   v_view, fscaleP, fscale, ua, va, ub, vb, uc, vc, ud, vd, lmU, lmV);
            } else {
            	buffer.pos(p1.x, p1.y, p1.z).tex((double)ua, (double)va).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
    			buffer.pos(p2.x, p2.y, p2.z).tex((double)ub, (double)vb).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
    			buffer.pos(p3.x, p3.y, p3.z).tex((double)uc, (double)vc).color(prev.particleRed, prev.particleGreen, prev.particleBlue, prev.particleAlpha).lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
    			buffer.pos(p4.x, p4.y, p4.z).tex((double)ud, (double)vd).color(prev.particleRed, prev.particleGreen, prev.particleBlue, prev.particleAlpha).lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
            }
        }
    }
	
	private void renderSmoothedQuad(BufferBuilder buffer, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4,
	                                Vec3d posPrev, Vec3d posCurrent, Vec3d v_view,
	                                float scalePrev, float scaleCurrent,
	                                float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd,
	                                int lmU, int lmV) {
		Vec3d posPrevPrev = prev.prev != null ? 
			new Vec3d(prev.prev.posX, prev.prev.posY, prev.prev.posZ).subtract(ClientProxy.get().getPlayerClient().getPositionVector()) : 
			null;
		
		Vec3d lastQ1 = null;
		Vec3d lastQ2 = null;
		
		int subdivisions = Math.max(1, Math.min(smoothingSubdivisions, 10));
		
		for (int i = 0; i < subdivisions; i++) {
			float t0 = (float)i / subdivisions;
			float t1 = (float)(i + 1) / subdivisions;
			
			Vec3d c0, c1;
			if (posPrevPrev != null) {
				c0 = catmullRom(posPrevPrev, posPrev, posCurrent, t0);
				c1 = catmullRom(posPrevPrev, posPrev, posCurrent, t1);
			} else {
				c0 = posPrev.scale(1.0 - t0).add(posCurrent.scale(t0));
				c1 = posPrev.scale(1.0 - t1).add(posCurrent.scale(t1));
			}
			
			Vec3d dir = c1.subtract(c0).normalize();
			Vec3d cross = v_view.crossProduct(dir).normalize();
			
			float s0 = scalePrev + (scaleCurrent - scalePrev) * t0;
			float s1 = scalePrev + (scaleCurrent - scalePrev) * t1;
			
			Vec3d q1 = new Vec3d(cross.x*s1 + c1.x, cross.y*s1 + c1.y, cross.z*s1 + c1.z);
			Vec3d q2 = new Vec3d(-cross.x*s1 + c1.x, -cross.y*s1 + c1.y, -cross.z*s1 + c1.z);
			Vec3d q3, q4;
			
			if (i == 0) {
				q4 = p4;
				q3 = p3;
			} else {
				q4 = lastQ1;
				q3 = lastQ2;
			}
			
			if (i == subdivisions - 1) {
				this.pos1 = q1;
				this.pos2 = q2;
			}
			
			float u0 = uc + (ua - uc) * t0;
			float u1 = uc + (ua - uc) * t1;
			
			float r0 = prev.particleRed + (this.particleRed - prev.particleRed) * t0;
			float g0 = prev.particleGreen + (this.particleGreen - prev.particleGreen) * t0;
			float b0 = prev.particleBlue + (this.particleBlue - prev.particleBlue) * t0;
			float a0 = prev.particleAlpha + (this.particleAlpha - prev.particleAlpha) * t0;
			
			float r1 = prev.particleRed + (this.particleRed - prev.particleRed) * t1;
			float g1 = prev.particleGreen + (this.particleGreen - prev.particleGreen) * t1;
			float b1 = prev.particleBlue + (this.particleBlue - prev.particleBlue) * t1;
			float a1 = prev.particleAlpha + (this.particleAlpha - prev.particleAlpha) * t1;
			
			buffer.pos(q1.x, q1.y, q1.z).tex(u1, va).color(r1, g1, b1, a1).lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
			buffer.pos(q2.x, q2.y, q2.z).tex(u1, vc).color(r1, g1, b1, a1).lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
			buffer.pos(q3.x, q3.y, q3.z).tex(u0, vc).color(r0, g0, b0, a0).lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
			buffer.pos(q4.x, q4.y, q4.z).tex(u0, va).color(r0, g0, b0, a0).lightmap(lmU, lmV).normal(0.0f, 1.0f, 0.0f).endVertex();
			
			lastQ1 = q1;
			lastQ2 = q2;
		}
	}
	
	private Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, float t) {
		Vec3d p3 = p2.add(p2.subtract(p1).scale(0.5));
		
		float t2 = t * t;
		float t3 = t2 * t;
		
		double x = 0.5 * ((2 * p1.x) + 
				(-p0.x + p2.x) * t + 
				(2*p0.x - 5*p1.x + 4*p2.x - p3.x) * t2 + 
				(-p0.x + 3*p1.x - 3*p2.x + p3.x) * t3);
		
		double y = 0.5 * ((2 * p1.y) + 
				(-p0.y + p2.y) * t + 
				(2*p0.y - 5*p1.y + 4*p2.y - p3.y) * t2 + 
				(-p0.y + 3*p1.y - 3*p2.y + p3.y) * t3);
		
		double z = 0.5 * ((2 * p1.z) + 
				(-p0.z + p2.z) * t + 
				(2*p0.z - 5*p1.z + 4*p2.z - p3.z) * t2 + 
				(-p0.z + 3*p1.z - 3*p2.z + p3.z) * t3);
		
		return new Vec3d(x, y, z);
	}

	public SAParticleStreak getPrev() {
		return prev;
	}

	public void setPrev(SAParticleStreak prev) {
		this.prev = prev;
	}

	public SAParticleStreak getNext() {
		return next;
	}

	public void setNext(SAParticleStreak next) {
		this.next = next;
	}

	@Override
	public boolean doNotSort() {
		return true;
	}
	
}
