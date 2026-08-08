package mchhui.hegltf;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;

public final class GltfCpuScheduler {
    private static ExecutorService executor;
    private static final AtomicInteger THREAD_SEQ = new AtomicInteger();
    private static final AtomicInteger QUEUED = new AtomicInteger();

    private GltfCpuScheduler() {
    }

    private static synchronized ExecutorService executor() {
        if (executor == null || executor.isShutdown()) {
            int n = 2;
            if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.gltf != null) {
                n = Math.max(1, Math.min(8, ModConfig.INSTANCE.gltf.cpuWorkers));
            }
            final int workers = n;
            executor = Executors.newFixedThreadPool(workers, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "MWF-GltfCPU-" + THREAD_SEQ.incrementAndGet());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                }
            });
            GltfModelManager.devLog("[GltfLazy] CPU scheduler started with {} workers", workers);
        }
        return executor;
    }

    public static void submit(Runnable task) {
        QUEUED.incrementAndGet();
        executor().execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                ModularWarfare.LOGGER.warn("[GltfLazy] CPU task failed", t);
            } finally {
                QUEUED.decrementAndGet();
            }
        });
    }

    public static int queuedCount() {
        return QUEUED.get();
    }

    public static synchronized void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        QUEUED.set(0);
    }
}
