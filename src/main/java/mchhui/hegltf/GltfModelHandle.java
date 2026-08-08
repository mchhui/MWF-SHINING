package mchhui.hegltf;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.util.ResourceLocation;

public class GltfModelHandle {
    public final ResourceLocation location;
    private final AtomicInteger pinCount = new AtomicInteger();
    private final AtomicInteger generation = new AtomicInteger();

    private volatile GltfLoadPhase phase = GltfLoadPhase.EMPTY;
    private volatile GltfDataModel dataModel;
    private volatile GltfLoadPriority priority = GltfLoadPriority.NORMAL;
    private volatile long lastUsedMs = System.currentTimeMillis();
    private volatile long idleSinceMs = -1;
    private volatile boolean loadQueued;
    private volatile long softPinUntilMs;

    public GltfModelHandle(ResourceLocation location) {
        this.location = location;
    }

    public GltfLoadPhase getPhase() {
        return phase;
    }

    public void setPhase(GltfLoadPhase phase) {
        this.phase = phase;
    }

    public GltfDataModel getDataModel() {
        return dataModel;
    }

    public void setDataModel(GltfDataModel dataModel) {
        this.dataModel = dataModel;
        generation.incrementAndGet();
    }

    public void bumpGeneration() {
        generation.incrementAndGet();
    }

    public int getGeneration() {
        return generation.get();
    }

    public GltfLoadPriority getPriority() {
        return priority;
    }

    public void bumpPriority(GltfLoadPriority p) {
        if (p != null) {
            priority = priority.max(p);
        }
    }

    public boolean isPriorityHigh() {
        return priority.rank >= GltfLoadPriority.HIGH.rank;
    }

    public void touch() {
        lastUsedMs = System.currentTimeMillis();
        if (pinCount.get() > 0) {
            idleSinceMs = -1;
        }
    }

    public long getLastUsedMs() {
        return lastUsedMs;
    }

    public void pin() {
        pinCount.incrementAndGet();
        idleSinceMs = -1;
        touch();
    }

    public void unpin() {
        int v = pinCount.decrementAndGet();
        if (v <= 0) {
            pinCount.set(0);
            idleSinceMs = System.currentTimeMillis();
        }
    }

    public int getPinCount() {
        return pinCount.get();
    }

    public long getIdleSinceMs() {
        return idleSinceMs;
    }

    public boolean isLoadQueued() {
        return loadQueued;
    }

    public void setLoadQueued(boolean loadQueued) {
        this.loadQueued = loadQueued;
    }

    public void softPinUntil(long untilMs) {
        softPinUntilMs = Math.max(softPinUntilMs, untilMs);
        touch();
    }

    public boolean isSoftPinned(long now) {
        return now < softPinUntilMs;
    }

    public boolean isAnimReady() {
        GltfLoadPhase p = phase;
        return p == GltfLoadPhase.ANIM_READY || p == GltfLoadPhase.MESH_LOADING
            || p == GltfLoadPhase.PROXY_READY || p == GltfLoadPhase.FULL_READY;
    }

    public boolean isMeshReady() {
        return phase == GltfLoadPhase.FULL_READY || phase == GltfLoadPhase.PROXY_READY;
    }

    public boolean isFullReady() {
        return phase == GltfLoadPhase.FULL_READY;
    }
}
