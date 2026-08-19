package safx.client.particle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import safx.SAConfig;
import safx.client.particle.list.ParticleList;
import safx.client.render.particle.SAInstancedParticleRenderer;
import safx.client.render.SAFrustumCache;
import safx.client.render.SARenderHelper;
import safx.client.render.SARenderHelper.RenderType;
import safx.util.LightCache;

public class SAParticleManager {
	
    public static double interpPosX;
    public static double interpPosY;
    public static double interpPosZ;

	protected ParticleList<SAParticleSystem> list_systems = new ParticleList<>();
	protected ParticleList<ISAParticle> list = new ParticleList<>();
	protected ParticleList<ISAParticle> list_nosort = new ParticleList<>();
	protected ComparatorParticleDepth compare = new ComparatorParticleDepth();

	private final HashMap<Long, RenderBucket> bucketMap = new HashMap<>(32);
	private final ArrayList<RenderBucket> bucketRenderOrder = new ArrayList<>(32);
	private final SAInstancedParticleRenderer instancedRenderer = SAInstancedParticleRenderer.get();
	private final ArrayDeque<SAParticle> collisionActive = new ArrayDeque<SAParticle>();
	private boolean useInstancedRendering;
	
	public void addEffect(ISAParticle effect) {
		if (effect == null) return;
		synchronized(this) {
			if(effect instanceof SAParticleSystem) {
				list_systems.add((SAParticleSystem) effect);
			} else {
				if (effect.doNotSort()) {
					list_nosort.add(effect);
				} else {
					list.add(effect);
				}
				if (effect instanceof SAParticle) {
					SAParticle particle = (SAParticle) effect;
					this.insertIntoBucket(particle, effect.doNotSort());
					this.registerCollisionParticle(particle);
				}
			}
		}
	}

	public void detachParticle(ISAParticle effect) {
		if (effect instanceof SAParticle) {
			SAParticle particle = (SAParticle) effect;
			this.unregisterCollisionParticle(particle);
			particle.detachFromBucket();
		}
	}
	
	public void tickParticles() {
		if(Minecraft.getMinecraft().isGamePaused()) return;
		
		Entity viewEnt = Minecraft.getMinecraft().getRenderViewEntity();
		LightCache.beginFrame();
		this.trimCollisionParticles();
		
		Iterator<SAParticleSystem> sysit = list_systems.iterator();
		while(sysit.hasNext()) {
			SAParticleSystem p = sysit.next();
			p.updateTick();
			if(p.shouldRemove()) {
				sysit.remove();
			}
		}

		boolean writeDepth = SAConfig.cl_particleSortMode != SAConfig.SORT_NONE;
		SAParticleParallelTick.tickList(this.list, viewEnt, writeDepth, this);
		SAParticleParallelTick.tickList(this.list_nosort, viewEnt, false, this);
	}

	private void registerCollisionParticle(SAParticle particle) {
		if (particle == null || particle.type == null || !particle.type.blockHitAffect) {
			return;
		}
		particle.blockHitActive = true;
		this.collisionActive.addLast(particle);
		this.trimCollisionParticles();
	}

	private void unregisterCollisionParticle(SAParticle particle) {
		if (particle == null) {
			return;
		}
		this.collisionActive.remove(particle);
	}

	private void trimCollisionParticles() {
		int limit = SAConfig.cl_collisionParticleLimit;
		if (limit <= 0) {
			return;
		}
		while (this.collisionActive.size() > limit) {
			SAParticle oldest = this.collisionActive.pollFirst();
			if (oldest != null && !oldest.shouldRemove()) {
				oldest.disableBlockHit();
			}
		}
	}

	public void renderParticles(Entity playerIn, float partialTicks) {
        float f1 = MathHelper.cos(playerIn.rotationYaw * 0.017453292F);
        float f2 = MathHelper.sin(playerIn.rotationYaw * 0.017453292F);
        float f3 = -f2 * MathHelper.sin(playerIn.rotationPitch * 0.017453292F);
        float f4 = f1 * MathHelper.sin(playerIn.rotationPitch * 0.017453292F);
        float f5 = MathHelper.cos(playerIn.rotationPitch * 0.017453292F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        Minecraft mc = Minecraft.getMinecraft();

        interpPosX = playerIn.lastTickPosX + (playerIn.posX - playerIn.lastTickPosX) * (double)partialTicks;
        interpPosY = playerIn.lastTickPosY + (playerIn.posY - playerIn.lastTickPosY) * (double)partialTicks;
        interpPosZ = playerIn.lastTickPosZ + (playerIn.posZ - playerIn.lastTickPosZ) * (double)partialTicks;

        SAFrustumCache.ensureUpdated(partialTicks);
        this.useInstancedRendering = this.instancedRenderer.isAvailable();
        if (this.useInstancedRendering) {
        	this.instancedRenderer.beginFrame();
        }
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.entityRenderer.enableLightmap();

        this.renderBuckets(tessellator, bufferbuilder, mc, playerIn, partialTicks, f1, f5, f2, f3, f4);
        if (this.useInstancedRendering) {
        	this.instancedRenderer.endFrame();
        }

        mc.entityRenderer.disableLightmap();
        GlStateManager.depthMask(true);
    }

	private void insertIntoBucket(SAParticle particle, boolean fixedOrder) {
		SAParticleSystemType type = particle.type;
		if (type == null || type.texture == null) {
			return;
		}
		RenderBucket bucket = this.acquireBucket(type.texture, type.renderType);
		if (!particle.canUseInstancedRender()) {
			bucket.instancedEligible = false;
		}
		if (type.surfaceAligned) {
			bucket.surfaceDecal = true;
		}
		if (fixedOrder) {
			particle.attachToBucket(bucket.fixedOrder, bucket);
		} else {
			particle.attachToBucket(bucket.sortable, bucket);
		}
		particle.refreshPackedLightIfMoved();
	}

	private RenderBucket acquireBucket(ResourceLocation texture, RenderType renderType) {
		long key = bucketKey(texture, renderType);
		RenderBucket bucket = bucketMap.get(key);
		if (bucket != null) {
			return bucket;
		}
		bucket = new RenderBucket(texture, renderType);
		bucketMap.put(key, bucket);
		bucketRenderOrder.add(bucket);
		return bucket;
	}

	private static long bucketKey(ResourceLocation texture, RenderType renderType) {
		return ((long) System.identityHashCode(texture) << 32) | (renderType.ordinal() & 0xFFFFFFFFL);
	}

	private boolean shouldSortBucket(RenderBucket bucket) {
		RenderType type = bucket.renderType;
		if (type == RenderType.ADDITIVE || type == RenderType.NO_Z_TEST_ADDITIVE) {
			return false;
		}
		int mode = SAConfig.cl_particleSortMode;
		if (mode == SAConfig.SORT_NONE) {
			return false;
		}
		int n = bucket.sortable.size();
		if (n < 2) {
			return false;
		}
		if (mode == SAConfig.SORT_PARTIAL && n > SAConfig.cl_particleSortLimit) {
			return false;
		}
		return true;
	}

	private void renderBuckets(Tessellator tessellator, BufferBuilder bufferbuilder, Minecraft mc,
			Entity playerIn, float partialTicks, float f1, float f5, float f2, float f3, float f4) {
		for (int pass = 0; pass < 2; pass++) {
			boolean surfacePass = pass == 0;
			for (int i = 0; i < bucketRenderOrder.size(); i++) {
				RenderBucket bucket = bucketRenderOrder.get(i);
				if (bucket.surfaceDecal != surfacePass || bucket.isEmpty()) {
					continue;
				}
				this.renderBucket(tessellator, bufferbuilder, mc, playerIn, partialTicks, f1, f5, f2, f3, f4, bucket);
			}
		}
	}

	private void renderBucket(Tessellator tessellator, BufferBuilder bufferbuilder, Minecraft mc, Entity playerIn,
			float partialTicks, float f1, float f5, float f2, float f3, float f4, RenderBucket bucket) {
		if (this.shouldSortBucket(bucket)) {
			bucket.sortable.sort(compare);
		}
		if (this.useInstancedRendering && bucket.instancedEligible) {
			if (!bucket.sortable.isEmpty()) {
				this.instancedRenderer.renderInstanced(bucket.sortable, bucket.texture, bucket.renderType,
						partialTicks, f1, f5, f2, f3, f4);
			}
			if (!bucket.fixedOrder.isEmpty()) {
				this.instancedRenderer.renderInstanced(bucket.fixedOrder, bucket.texture, bucket.renderType,
						partialTicks, f1, f5, f2, f3, f4);
			}
		} else {
			mc.getTextureManager().bindTexture(bucket.texture);
			SARenderHelper.enableBlendMode(bucket.renderType);
			bufferbuilder.begin(GL11.GL_QUADS, SAParticle.VERTEX_FORMAT);
			this.renderParticleArray(bucket.sortable, bufferbuilder, playerIn, partialTicks, f1, f5, f2, f3, f4);
			this.renderParticleArray(bucket.fixedOrder, bufferbuilder, playerIn, partialTicks, f1, f5, f2, f3, f4);
			tessellator.draw();
			SARenderHelper.disableBlendMode(bucket.renderType);
		}
	}

	private void renderParticleArray(SAParticleArray particles, BufferBuilder bufferbuilder, Entity playerIn,
			float partialTicks, float f1, float f5, float f2, float f3, float f4) {
		SAParticle[] data = particles.data();
		for (int i = 0, size = particles.size(); i < size; i++) {
			SAParticle particle = data[i];
			if (SAConfig.cl_enableParticleFrustumCull
					&& !particle.isInCameraFrustum(partialTicks)) {
				continue;
			}
			data[i].doRender(bufferbuilder, playerIn, partialTicks, f1, f5, f2, f3, f4);
		}
	}

	public double distanceToPlane(Entity viewEntity, double px, double py, double pz) {
		Vec3d n = viewEntity.getLookVec();
		double vx = viewEntity.posX;
		double vy = viewEntity.posY;
		double vz = viewEntity.posZ;
		double dot1 = -n.x * (px - vx) - n.y * (py - vy) - n.z * (pz - vz);
		double dot2 = n.x * n.x + n.y * n.y + n.z * n.z;
		double f = dot1 / dot2;
		double qx = px + n.x * f;
		double qy = py + n.y * f;
		double qz = pz + n.z * f;
		double dx = px - qx;
		double dy = py - qy;
		double dz = pz - qz;
		return dx * dx + dy * dy + dz * dz;
	}

	static final class RenderBucket {
		ResourceLocation texture;
		RenderType renderType;
		final SAParticleArray sortable = new SAParticleArray();
		final SAParticleArray fixedOrder = new SAParticleArray();
		boolean instancedEligible = true;
		boolean surfaceDecal = false;

		RenderBucket(ResourceLocation texture, RenderType renderType) {
			this.texture = texture;
			this.renderType = renderType;
		}

		boolean isEmpty() {
			return sortable.isEmpty() && fixedOrder.isEmpty();
		}
	}
	
	public static class ComparatorParticleDepth implements java.util.Comparator<SAParticle> {

		@Override
		public int compare(SAParticle p1, SAParticle p2) {
			if(p1.doNotSort() && p2.doNotSort()) {
				return 0;
			}
			double dist1 = p1.getDepth();
			double dist2 = p2.getDepth();
			if(dist1 < dist2) {
				return 1;
			} else if(dist1 > dist2) {
				return -1;
			}
			return 0;
		}
	}
}
