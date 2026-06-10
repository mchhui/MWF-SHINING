package safx.client.particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import safx.client.particle.list.ParticleList;
import safx.client.particle.list.ParticleList.ParticleListIterator;
import safx.client.render.particle.SAInstancedParticleRenderer;
import safx.client.render.SARenderHelper;
import safx.client.render.SARenderHelper.RenderType;

public class SAParticleManager {
	
    public static double interpPosX;
    public static double interpPosY;
    public static double interpPosZ;

	protected ParticleList<SAParticleSystem> list_systems = new ParticleList<>();
	protected ParticleList<ISAParticle> list = new ParticleList<>();
	protected ParticleList<ISAParticle> list_nosort = new ParticleList<>();
	protected ComparatorParticleDepth compare = new ComparatorParticleDepth();

	private final HashMap<Long, RenderBucket> bucketMap = new HashMap<>(32);
	private final ArrayList<RenderBucket> bucketPool = new ArrayList<>(32);
	private final ArrayList<RenderBucket> bucketRenderOrder = new ArrayList<>(32);
	private int nextBucketIndex = 0;
	private final ArrayList<SAParticle> instancedScratch = new ArrayList<>(1024);
	private final SAInstancedParticleRenderer instancedRenderer = SAInstancedParticleRenderer.get();
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
			}
		}
	}
	
	public void tickParticles() {
		if(Minecraft.getMinecraft().isGamePaused()) return;
		
		Entity viewEnt = Minecraft.getMinecraft().getRenderViewEntity();
		
		Iterator<SAParticleSystem> sysit = list_systems.iterator();
		while(sysit.hasNext()) {
			SAParticleSystem p = sysit.next();
			p.updateTick();
			if(p.shouldRemove()) {
				sysit.remove();
			}
		}
		
		ParticleListIterator<ISAParticle> it = list.iterator();
		while(it.hasNext()) {
			ISAParticle p = it.next();
			p.updateTick();
			if(p.shouldRemove()) {
				it.remove();
			} else if(viewEnt != null) {
				p.setDepth(this.distanceToPlane(viewEnt, p.getPosX(), p.getPosY(), p.getPosZ()));
			}
		}
		
		Iterator<ISAParticle> it2 = list_nosort.iterator();
		while(it2.hasNext()) {
			ISAParticle p = it2.next();
			p.updateTick();
			if(p.shouldRemove()) {
				it2.remove();
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

        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.entityRenderer.enableLightmap();

        this.useInstancedRendering = this.instancedRenderer.isAvailable();
        this.beginBucketPass();
        this.collectParticles(this.list, true);
        this.collectParticles(this.list_nosort, false);
        this.renderBuckets(tessellator, bufferbuilder, mc, playerIn, partialTicks, f1, f5, f2, f3, f4);

        mc.entityRenderer.disableLightmap();
        GlStateManager.depthMask(true);
    }

	private void beginBucketPass() {
		for (RenderBucket bucket : bucketPool) {
			bucket.clear();
		}
		bucketMap.clear();
		bucketRenderOrder.clear();
		nextBucketIndex = 0;
	}

	private void collectParticles(ParticleList<ISAParticle> particles, boolean sortByDepth) {
		ParticleListIterator<ISAParticle> it = particles.iterator();
		while (it.hasNext()) {
			ISAParticle particle = it.next();
			if (!(particle instanceof SAParticle)) {
				continue;
			}
			SAParticle sp = (SAParticle) particle;
			SAParticleSystemType type = sp.type;
			if (type == null) {
				continue;
			}
			RenderBucket bucket = this.acquireBucket(type.texture, type.renderType);
			if (sortByDepth) {
				bucket.sortable.add(particle);
			} else {
				bucket.fixedOrder.add(particle);
			}
		}
	}

	private RenderBucket acquireBucket(ResourceLocation texture, RenderType renderType) {
		long key = bucketKey(texture, renderType);
		RenderBucket bucket = bucketMap.get(key);
		if (bucket != null) {
			return bucket;
		}
		if (nextBucketIndex < bucketPool.size()) {
			bucket = bucketPool.get(nextBucketIndex++);
		} else {
			bucket = new RenderBucket(texture, renderType);
			bucketPool.add(bucket);
			nextBucketIndex++;
		}
		bucket.texture = texture;
		bucket.renderType = renderType;
		bucketMap.put(key, bucket);
		bucketRenderOrder.add(bucket);
		return bucket;
	}

	private static long bucketKey(ResourceLocation texture, RenderType renderType) {
		return ((long) System.identityHashCode(texture) << 32) | (renderType.ordinal() & 0xFFFFFFFFL);
	}

	private void renderBuckets(Tessellator tessellator, BufferBuilder bufferbuilder, Minecraft mc,
			Entity playerIn, float partialTicks, float f1, float f5, float f2, float f3, float f4) {
		for (int i = 0; i < bucketRenderOrder.size(); i++) {
			RenderBucket bucket = bucketRenderOrder.get(i);
			if (bucket.isEmpty()) {
				continue;
			}
			if (!bucket.sortable.isEmpty()) {
				Collections.sort(bucket.sortable, compare);
			}
			if (this.useInstancedRendering && this.canInstancEntireBucket(bucket)) {
				this.instancedScratch.clear();
				this.collectInstancedParticles(bucket.sortable);
				this.collectInstancedParticles(bucket.fixedOrder);
				this.instancedRenderer.renderInstanced(this.instancedScratch, bucket.texture, bucket.renderType,
						partialTicks, f1, f5, f2, f3, f4);
			} else {
				mc.getTextureManager().bindTexture(bucket.texture);
				SARenderHelper.enableBlendMode(bucket.renderType);
				bufferbuilder.begin(GL11.GL_QUADS, SAParticle.VERTEX_FORMAT);
				this.renderParticleList(bucket.sortable, bufferbuilder, playerIn, partialTicks, f1, f5, f2, f3, f4);
				this.renderParticleList(bucket.fixedOrder, bufferbuilder, playerIn, partialTicks, f1, f5, f2, f3, f4);
				tessellator.draw();
				SARenderHelper.disableBlendMode(bucket.renderType);
			}
		}
	}

	private boolean canInstancEntireBucket(RenderBucket bucket) {
		if (!this.listAllInstancable(bucket.sortable)) {
			return false;
		}
		if (!this.listAllInstancable(bucket.fixedOrder)) {
			return false;
		}
		return !bucket.isEmpty();
	}

	private boolean listAllInstancable(List<ISAParticle> particles) {
		for (int i = 0, size = particles.size(); i < size; i++) {
			ISAParticle particle = particles.get(i);
			if (!(particle instanceof SAParticle) || !((SAParticle) particle).canUseInstancedRender()) {
				return false;
			}
		}
		return true;
	}

	private void collectInstancedParticles(List<ISAParticle> source) {
		for (int i = 0, size = source.size(); i < size; i++) {
			this.instancedScratch.add((SAParticle) source.get(i));
		}
	}

	private void renderParticleList(List<ISAParticle> particles, BufferBuilder bufferbuilder, Entity playerIn,
			float partialTicks, float f1, float f5, float f2, float f3, float f4) {
		for (int i = 0, size = particles.size(); i < size; i++) {
			particles.get(i).doRender(bufferbuilder, playerIn, partialTicks, f1, f5, f2, f3, f4);
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

	private static final class RenderBucket {
		ResourceLocation texture;
		RenderType renderType;
		final ArrayList<ISAParticle> sortable = new ArrayList<>();
		final ArrayList<ISAParticle> fixedOrder = new ArrayList<>();

		RenderBucket(ResourceLocation texture, RenderType renderType) {
			this.texture = texture;
			this.renderType = renderType;
		}

		void clear() {
			sortable.clear();
			fixedOrder.clear();
		}

		boolean isEmpty() {
			return sortable.isEmpty() && fixedOrder.isEmpty();
		}
	}
	
	public static class ComparatorParticleDepth implements java.util.Comparator<ISAParticle> {

		@Override
		public int compare(ISAParticle p1, ISAParticle p2) {
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
