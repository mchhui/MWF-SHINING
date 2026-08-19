package safx.client.particle;

import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.RecursiveAction;

import net.minecraft.entity.Entity;
import safx.SAConfig;
import safx.client.particle.list.ParticleList;
import safx.client.particle.list.ParticleList.ParticleListIterator;
import safx.client.render.particle.SAInstancedParticleShader;

/**
 * MadParticle-style split: independent motion ticks on a daemon ForkJoin pool;
 * systems, stick/collision/streak stay on the client thread.
 */
public final class SAParticleParallelTick {

	private static final int GRAIN = 64;
	private static final int PACK_GRAIN = 512;
	private static final ArrayList<ISAParticle> ALL = new ArrayList<ISAParticle>(1024);
	private static final ArrayList<ISAParticle> ASYNC = new ArrayList<ISAParticle>(1024);
	private static final ArrayList<ISAParticle> SYNC = new ArrayList<ISAParticle>(256);

	private static final ForkJoinPool POOL = new ForkJoinPool(
			Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 1)),
			new ForkJoinPool.ForkJoinWorkerThreadFactory() {
				@Override
				public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
					ForkJoinWorkerThread t = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
					t.setName("safx-particle-tick-" + t.getPoolIndex());
					t.setDaemon(true);
					return t;
				}
			},
			null,
			true);

	private SAParticleParallelTick() {
	}

	public static void tickList(ParticleList<ISAParticle> particles, Entity viewEnt, boolean writeDepth,
			SAParticleManager manager) {
		if (particles.getSize() <= 0) {
			return;
		}
		if (!SAConfig.cl_asyncParticleTick
				|| particles.getSize() < SAConfig.cl_asyncParticleTickMinCount) {
			tickSequential(particles, viewEnt, writeDepth, manager);
			return;
		}
		ALL.clear();
		ASYNC.clear();
		SYNC.clear();
		particles.collectInto(ALL);
		for (int i = 0, n = ALL.size(); i < n; i++) {
			ISAParticle p = ALL.get(i);
			if (p == null) {
				continue;
			}
			if (p.canAsyncTick()) {
				ASYNC.add(p);
			} else {
				SYNC.add(p);
			}
		}
		for (int i = 0, n = SYNC.size(); i < n; i++) {
			tickOne(SYNC.get(i));
		}
		if (!ASYNC.isEmpty()) {
			POOL.invoke(new TickRange(ASYNC, 0, ASYNC.size()));
		}
		sweep(particles, viewEnt, writeDepth, manager);
		ALL.clear();
		ASYNC.clear();
		SYNC.clear();
	}

	private static void tickSequential(ParticleList<ISAParticle> particles, Entity viewEnt, boolean writeDepth,
			SAParticleManager manager) {
		ParticleListIterator<ISAParticle> it = particles.iterator();
		while (it.hasNext()) {
			ISAParticle p = it.next();
			p.updateTick();
			if (p.shouldRemove()) {
				manager.detachParticle(p);
				it.remove();
			} else {
				if (p instanceof SAParticle) {
					((SAParticle) p).refreshPackedLightIfMoved();
				}
				if (writeDepth && viewEnt != null) {
					p.setDepth(manager.distanceToPlane(viewEnt, p.getPosX(), p.getPosY(), p.getPosZ()));
				}
			}
		}
	}

	private static void sweep(ParticleList<ISAParticle> particles, Entity viewEnt, boolean writeDepth,
			SAParticleManager manager) {
		ParticleListIterator<ISAParticle> it = particles.iterator();
		while (it.hasNext()) {
			ISAParticle p = it.next();
			if (p.shouldRemove()) {
				manager.detachParticle(p);
				it.remove();
			} else {
				if (p instanceof SAParticle) {
					((SAParticle) p).refreshPackedLightIfMoved();
				}
				if (writeDepth && viewEnt != null) {
					p.setDepth(manager.distanceToPlane(viewEnt, p.getPosX(), p.getPosY(), p.getPosZ()));
				}
			}
		}
	}

	public static void packInstanced(SAParticle[] data, int count, float[] dest, float partialTicks) {
		packInstanced(data, 0, count, dest, partialTicks);
	}

	public static void packInstanced(SAParticle[] data, int from, int count, float[] dest, float partialTicks) {
		if (count <= 0 || data == null || dest == null) {
			return;
		}
		if (!SAConfig.cl_asyncParticlePack || count < SAConfig.cl_asyncParticlePackMinCount) {
			packRange(data, from, from + count, dest, 0, partialTicks);
			return;
		}
		POOL.invoke(new PackRange(data, from, from + count, dest, 0, partialTicks));
	}

	private static void packRange(SAParticle[] data, int from, int to, float[] dest, int destBaseParticle,
			float partialTicks) {
		int floats = SAInstancedParticleShader.INSTANCE_FLOATS;
		int out = destBaseParticle;
		for (int i = from; i < to; i++) {
			SAParticle particle = data[i];
			if (particle != null) {
				particle.packInstanced(dest, out * floats, partialTicks);
			}
			out++;
		}
	}

	private static void tickOne(ISAParticle p) {
		try {
			p.updateTick();
		} catch (Throwable ignored) {
		}
	}

	private static final class TickRange extends RecursiveAction {
		private static final long serialVersionUID = 1L;
		private final ArrayList<ISAParticle> particles;
		private final int from;
		private final int to;

		TickRange(ArrayList<ISAParticle> particles, int from, int to) {
			this.particles = particles;
			this.from = from;
			this.to = to;
		}

		@Override
		protected void compute() {
			int n = this.to - this.from;
			if (n <= GRAIN) {
				for (int i = this.from; i < this.to; i++) {
					tickOne(this.particles.get(i));
				}
				return;
			}
			int mid = this.from + (n >> 1);
			invokeAll(new TickRange(this.particles, this.from, mid), new TickRange(this.particles, mid, this.to));
		}
	}

	private static final class PackRange extends RecursiveAction {
		private static final long serialVersionUID = 1L;
		private final SAParticle[] data;
		private final int from;
		private final int to;
		private final float[] dest;
		private final int destBaseParticle;
		private final float partialTicks;

		PackRange(SAParticle[] data, int from, int to, float[] dest, int destBaseParticle, float partialTicks) {
			this.data = data;
			this.from = from;
			this.to = to;
			this.dest = dest;
			this.destBaseParticle = destBaseParticle;
			this.partialTicks = partialTicks;
		}

		@Override
		protected void compute() {
			int n = this.to - this.from;
			if (n <= PACK_GRAIN) {
				packRange(this.data, this.from, this.to, this.dest, this.destBaseParticle, this.partialTicks);
				return;
			}
			int mid = this.from + (n >> 1);
			int leftCount = mid - this.from;
			invokeAll(
					new PackRange(this.data, this.from, mid, this.dest, this.destBaseParticle, this.partialTicks),
					new PackRange(this.data, mid, this.to, this.dest, this.destBaseParticle + leftCount,
							this.partialTicks));
		}
	}
}
