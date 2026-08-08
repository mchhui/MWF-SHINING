package mchhui.hegltf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;

import net.minecraft.util.ResourceLocation;

public final class GltfModelManager {
    private static final GltfModelManager INSTANCE = new GltfModelManager();

    private final ConcurrentHashMap<ResourceLocation, GltfModelHandle> handles = new ConcurrentHashMap<>();

    private GltfModelManager() {
    }

    public static GltfModelManager get() {
        return INSTANCE;
    }

    public static boolean isDevLog() {
        return ModConfig.INSTANCE != null && ModConfig.INSTANCE.dev_mode;
    }

    public static void devLog(String msg, Object... args) {
        if (isDevLog()) {
            ModularWarfare.LOGGER.info(msg, args);
        }
    }

    public GltfModelHandle getHandle(ResourceLocation loc) {
        return handles.get(loc);
    }

    public GltfModelHandle request(ResourceLocation loc, GltfLoadPriority priority) {
        if (loc == null) {
            return null;
        }
        GltfModelHandle handle = handles.computeIfAbsent(loc, GltfModelHandle::new);
        handle.bumpPriority(priority != null ? priority : GltfLoadPriority.NORMAL);
        handle.touch();

        if (!isLazyEnabled()) {
            if (handle.getDataModel() == null || handle.getPhase() == GltfLoadPhase.EMPTY
                || handle.getPhase() == GltfLoadPhase.FAILED) {
                loadSync(handle);
            }
            return handle;
        }

        if (needsLoad(handle)) {
            startAsyncLoad(handle);
        }
        return handle;
    }

    private static boolean needsLoad(GltfModelHandle handle) {
        GltfLoadPhase phase = handle.getPhase();
        if (phase == GltfLoadPhase.EMPTY || phase == GltfLoadPhase.FAILED) {
            return true;
        }
        if (phase == GltfLoadPhase.ANIM_READY) {
            GltfDataModel model = handle.getDataModel();
            if (model == null) {
                return true;
            }
            for (DataNode node : model.nodes.values()) {
                if (node.meshes != null && !node.meshes.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void startAsyncLoad(GltfModelHandle handle) {
        synchronized (handle) {
            if (handle.isLoadQueued()) {
                return;
            }
            if (!needsLoad(handle) && handle.getPhase() != GltfLoadPhase.EMPTY
                && handle.getPhase() != GltfLoadPhase.FAILED) {
                return;
            }
            handle.setLoadQueued(true);
            handle.setPhase(GltfLoadPhase.ANIM_LOADING);
        }
        final boolean high = handle.isPriorityHigh();
        GltfCpuScheduler.submit(() -> GltfDataModel.loadAsync(handle, high));
    }

    private void loadSync(GltfModelHandle handle) {
        synchronized (handle) {
            if (handle.getDataModel() != null && handle.isFullReady()) {
                return;
            }
            handle.setPhase(GltfLoadPhase.ANIM_LOADING);
        }
        GltfDataModel model = GltfDataModel.loadSync(handle.location);
        handle.setDataModel(model);
        if (model != null && model.loaded) {
            handle.setPhase(GltfLoadPhase.FULL_READY);
            model.phase = GltfLoadPhase.FULL_READY;
            model.queueAllMeshUploads(handle.isPriorityHigh());
        } else {
            handle.setPhase(GltfLoadPhase.FAILED);
        }
        handle.setLoadQueued(false);
    }

    public void pin(ResourceLocation loc) {
        GltfModelHandle h = handles.get(loc);
        if (h != null) {
            h.pin();
        }
    }

    public void unpin(ResourceLocation loc) {
        GltfModelHandle h = handles.get(loc);
        if (h != null) {
            h.unpin();
        }
    }

    public void softPin(ResourceLocation loc, long ttlMs) {
        GltfModelHandle h = request(loc, GltfLoadPriority.LOW);
        if (h != null) {
            h.softPinUntil(System.currentTimeMillis() + ttlMs);
        }
    }

    public void tickIdleUnload() {
        if (!isLazyEnabled()) {
            return;
        }
        int idleSec = 90;
        if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.gltf != null) {
            idleSec = ModConfig.INSTANCE.gltf.idleUnloadSeconds;
        }
        if (idleSec <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long timeout = idleSec * 1000L;
        ArrayList<GltfModelHandle> toUnload = new ArrayList<>();
        for (GltfModelHandle h : handles.values()) {
            if (h.getPinCount() > 0 || h.isSoftPinned(now)) {
                continue;
            }
            if (!h.isAnimReady() && h.getPhase() != GltfLoadPhase.MESH_LOADING) {
                continue;
            }
            long idleSince = h.getIdleSinceMs();
            if (idleSince < 0) {
                idleSince = h.getLastUsedMs();
            }
            if (now - idleSince >= timeout) {
                toUnload.add(h);
            }
        }
        for (GltfModelHandle h : toUnload) {
            unload(h, false);
        }
    }

    public void unload(GltfModelHandle handle, boolean force) {
        if (handle == null) {
            return;
        }
        synchronized (handle) {
            if (!force && (handle.getPinCount() > 0 || handle.isSoftPinned(System.currentTimeMillis()))) {
                return;
            }
            GltfDataModel model = handle.getDataModel();
            boolean keepAnim = ModConfig.INSTANCE != null && ModConfig.INSTANCE.gltf != null
                && ModConfig.INSTANCE.gltf.keepAnimAfterUnload;
            if (model != null) {
                final GltfDataModel toDelete = model;
                if (keepAnim && handle.isAnimReady()) {
                    GltfGpuUploadScheduler.add("unloadMesh:" + handle.location, 8, true, toDelete::deleteGpu);
                    toDelete.dropMeshData();
                    handle.setPhase(GltfLoadPhase.ANIM_READY);
                    model.phase = GltfLoadPhase.ANIM_READY;
                    devLog("[GltfLazy] Unloaded mesh, kept anim: {}", handle.location);
                } else {
                    ResourceLocation loc = handle.location;
                    handle.setDataModel(null);
                    handle.setPhase(GltfLoadPhase.EMPTY);
                    handle.setLoadQueued(false);
                    handles.remove(loc, handle);
                    GltfGpuUploadScheduler.add("unload:" + loc, 8, true, toDelete::delete);
                    devLog("[GltfLazy] Unloaded model: {}", loc);
                }
            } else {
                handle.setPhase(GltfLoadPhase.EMPTY);
                handle.setLoadQueued(false);
                handles.remove(handle.location, handle);
            }
            handle.bumpPriority(GltfLoadPriority.NORMAL);
        }
    }

    public void clearAll() {
        GltfGpuUploadScheduler.clear();
        Iterator<Map.Entry<ResourceLocation, GltfModelHandle>> it = handles.entrySet().iterator();
        while (it.hasNext()) {
            GltfModelHandle h = it.next().getValue();
            GltfDataModel model = h.getDataModel();
            if (model != null) {
                model.delete();
            }
            h.setDataModel(null);
            h.setPhase(GltfLoadPhase.EMPTY);
            h.setLoadQueued(false);
        }
        handles.clear();
        devLog("[GltfLazy] Cleared all cached GLTF models");
    }

    public void clearAllCpuOnly() {
        GltfGpuUploadScheduler.clear();
        Iterator<Map.Entry<ResourceLocation, GltfModelHandle>> it = handles.entrySet().iterator();
        while (it.hasNext()) {
            GltfModelHandle h = it.next().getValue();
            GltfDataModel model = h.getDataModel();
            if (model != null) {
                model.dropMeshData();
                model.loaded = false;
                model.phase = GltfLoadPhase.EMPTY;
            }
            h.setDataModel(null);
            h.setPhase(GltfLoadPhase.EMPTY);
            h.setLoadQueued(false);
        }
        handles.clear();
    }

    public int cachedCount() {
        int n = 0;
        for (GltfModelHandle h : handles.values()) {
            if (h.getDataModel() != null) {
                n++;
            }
        }
        return n;
    }

    public int readyCount() {
        int n = 0;
        for (GltfModelHandle h : handles.values()) {
            if (h.getDataModel() != null && h.isAnimReady()) {
                n++;
            }
        }
        return n;
    }

    public static boolean isLazyEnabled() {
        return ModConfig.INSTANCE == null || ModConfig.INSTANCE.gltf == null
            || ModConfig.INSTANCE.gltf.lazyLoad;
    }

    public static boolean isProxyEnabled() {
        return ModConfig.INSTANCE == null || ModConfig.INSTANCE.gltf == null
            || ModConfig.INSTANCE.gltf.proxyEnabled;
    }
}
