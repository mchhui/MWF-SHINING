package mchhui.hegltf;

import java.util.LinkedList;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID, value = Side.CLIENT)
public final class GltfGpuUploadScheduler {
    public static final class GpuTask {
        public final String name;
        public final int weight;
        public final boolean priority;
        public final Runnable run;

        public GpuTask(String name, int weight, boolean priority, Runnable run) {
            this.name = name;
            this.weight = Math.max(1, weight);
            this.priority = priority;
            this.run = run;
        }
    }

    private static final LinkedList<GpuTask> PRIORITY = new LinkedList<>();
    private static final LinkedList<GpuTask> NORMAL = new LinkedList<>();
    private static int lastFrameWeight;
    private static int lastFrameTasks;

    private GltfGpuUploadScheduler() {
    }

    public static synchronized void add(String name, int weight, boolean priority, Runnable run) {
        GpuTask task = new GpuTask(name, weight, priority, run);
        if (priority) {
            PRIORITY.addLast(task);
        } else {
            NORMAL.addLast(task);
        }
    }

    public static synchronized void clear() {
        PRIORITY.clear();
        NORMAL.clear();
    }

    public static synchronized int queuedCount() {
        return PRIORITY.size() + NORMAL.size();
    }

    public static int lastFrameWeight() {
        return lastFrameWeight;
    }

    public static int lastFrameTasks() {
        return lastFrameTasks;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        drainFrame();
    }

    private static void drainFrame() {
        int budget = 512;
        if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.gltf != null) {
            budget = Math.max(1, ModConfig.INSTANCE.gltf.uploadFrameBudget);
        }
        int used = 0;
        int tasks = 0;
        while (used < budget) {
            GpuTask task;
            synchronized (GltfGpuUploadScheduler.class) {
                task = PRIORITY.pollFirst();
                if (task == null) {
                    task = NORMAL.pollFirst();
                }
            }
            if (task == null) {
                break;
            }
            try {
                task.run.run();
            } catch (Throwable t) {
                ModularWarfare.LOGGER.warn("[GltfLazy] GPU task failed: " + task.name, t);
            }
            used += task.weight;
            tasks++;
        }
        lastFrameWeight = used;
        lastFrameTasks = tasks;
    }

    public static int estimateWeight(int bytes) {
        int partSize = 65536;
        int partWeight = 32;
        if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.gltf != null) {
            partSize = Math.max(1024, ModConfig.INSTANCE.gltf.uploadPartSize);
            partWeight = Math.max(1, ModConfig.INSTANCE.gltf.uploadPartWeight);
        }
        long w = (long) bytes / partSize * partWeight;
        return (int) Math.max(1, w);
    }
}
