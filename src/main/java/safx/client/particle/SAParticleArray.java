package safx.client.particle;

import java.util.Arrays;
import java.util.Comparator;

public final class SAParticleArray {

	private static final int INITIAL_CAP = 64;

	private SAParticle[] data = new SAParticle[INITIAL_CAP];
	private int size;

	public int size() {
		return this.size;
	}

	public boolean isEmpty() {
		return this.size == 0;
	}

	public SAParticle[] data() {
		return this.data;
	}

	SAParticle get(int index) {
		return this.data[index];
	}

	void add(SAParticle particle) {
		if (this.size == this.data.length) {
			this.data = Arrays.copyOf(this.data, this.data.length << 1);
		}
		particle.bucketSlot = this.size;
		this.data[this.size++] = particle;
	}

	void swapRemove(int slot) {
		int last = this.size - 1;
		SAParticle moved = this.data[last];
		this.data[last] = null;
		this.size = last;
		if (slot != last) {
			this.data[slot] = moved;
			moved.bucketSlot = slot;
		}
	}

	void sort(Comparator<SAParticle> comparator) {
		if (this.size < 2) {
			return;
		}
		Arrays.sort(this.data, 0, this.size, comparator);
		for (int i = 0; i < this.size; i++) {
			this.data[i].bucketSlot = i;
		}
	}
}
