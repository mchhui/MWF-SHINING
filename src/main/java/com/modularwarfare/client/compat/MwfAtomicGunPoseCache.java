package com.modularwarfare.client.compat;

import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.guns.GunType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches third-person / item-frame gun casters for Atomic shadow mid-fill.
 * <p>
 * Color/fill {@code drawThirdGun} stores a <b>world-space</b> gun-root matrix (camera MV
 * converted like flashlight TP pose). External shadow then draws
 * {@code T(-origin) * worldMatrix} depth-only — same contract as HE WorldObject casters.
 */
@SideOnly(Side.CLIENT)
public final class MwfAtomicGunPoseCache {

    public static final class Entry {
        public final String key;
        public final float x, y, z;
        public final long frame;
        /** Column-major 4×4 gun-local → world; may be null if conversion failed. */
        @Nullable
        public final float[] worldMatrix;
        public final ItemStack stack;
        public final String renderType;
        public final boolean sneak;
        /** Living entity id when known; -1 for item-frame / loot. */
        public final int entityId;

        Entry(String key, float x, float y, float z, long frame, @Nullable float[] worldMatrix,
                ItemStack stack, String renderType, boolean sneak, int entityId) {
            this.key = key;
            this.x = x;
            this.y = y;
            this.z = z;
            this.frame = frame;
            this.worldMatrix = worldMatrix;
            this.stack = stack;
            this.renderType = renderType;
            this.sneak = sneak;
            this.entityId = entityId;
        }

        public boolean isWorldProp() {
            return "itemframe".equals(renderType) || "itemloot".equals(renderType);
        }
    }

    private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static final Vector3f TMP = new Vector3f();
    private static long frameCounter;

    private MwfAtomicGunPoseCache() {
    }

    public static void onRenderTickStart() {
        frameCounter++;
        pruneStaleLivingGuns();
        if ((frameCounter & 63L) == 0L) {
            long min = frameCounter - 4L;
            Iterator<Map.Entry<String, Entry>> it = ENTRIES.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue().frame < min) {
                    it.remove();
                }
            }
        }
    }

    /** Drop living casters when the entity no longer holds that Enhance gun (stops phantom shadows). */
    public static void pruneStaleLivingGuns() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        Iterator<Map.Entry<String, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next().getValue();
            if (e.entityId < 0 || e.isWorldProp()) {
                continue;
            }
            Entity ent = mc.world.getEntityByID(e.entityId);
            if (!(ent instanceof EntityLivingBase)) {
                it.remove();
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) ent;
            if (!livingHoldsEnhanceGun(living, e)) {
                it.remove();
            }
        }
    }

    private static boolean livingHoldsEnhanceGun(EntityLivingBase living, Entry e) {
        if ("player_offhand".equals(e.renderType)) {
            return isEnhanceGunStack(living.getHeldItemOffhand());
        }
        return isEnhanceGunStack(living.getHeldItemMainhand());
    }

    private static boolean isEnhanceGunStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemGun)) {
            return false;
        }
        GunType type = ((ItemGun) stack.getItem()).type;
        return type != null && type.animationType == WeaponAnimationType.ENHANCED;
    }

    /** Drop cached world matrix for a living entity (force rebuild — e.g. local player in FP). */
    public static void clearWorldMatrixForEntity(int entityId) {
        if (entityId < 0) {
            return;
        }
        for (Map.Entry<String, Entry> en : ENTRIES.entrySet()) {
            Entry e = en.getValue();
            if (e.entityId != entityId || e.worldMatrix == null) {
                continue;
            }
            ENTRIES.put(en.getKey(), new Entry(e.key, e.x, e.y, e.z, e.frame, null, e.stack, e.renderType,
                    e.sneak, e.entityId));
        }
    }

    public static void put(String key, float worldX, float worldY, float worldZ,
            @Nullable float[] worldMatrix, ItemStack stack, String renderType, boolean sneak,
            int entityId) {
        if (key == null || stack == null || stack.isEmpty() || renderType == null) {
            return;
        }
        float[] matCopy = null;
        if (worldMatrix != null && worldMatrix.length >= 16) {
            matCopy = new float[16];
            System.arraycopy(worldMatrix, 0, matCopy, 0, 16);
        }
        ENTRIES.put(key, new Entry(key, worldX, worldY, worldZ, frameCounter, matCopy, stack.copy(),
                renderType, sneak, entityId));
    }

    /**
     * Shadow in-layer: refresh anchor/stack/frame without wiping a color-frame world matrix.
     * If no entry exists yet, insert matrix-less so entity rebuild can still run (FP first frames).
     */
    public static void touchForShadowPass(String key, float worldX, float worldY, float worldZ,
            ItemStack stack, String renderType, boolean sneak, int entityId) {
        if (key == null || stack == null || stack.isEmpty() || renderType == null) {
            return;
        }
        Entry old = ENTRIES.get(key);
        float[] keep = old != null ? old.worldMatrix : null;
        put(key, worldX, worldY, worldZ, keep, stack, renderType, sneak, entityId);
    }

    public static Iterable<Entry> entries() {
        return ENTRIES.values();
    }

    public static List<Entry> entriesInCull(float ox, float oy, float oz, float radius) {
        float r2 = radius * radius;
        List<Entry> out = new ArrayList<>();
        for (Entry e : ENTRIES.values()) {
            if (e.stack == null || e.stack.isEmpty() || e.renderType == null || e.renderType.isEmpty()) {
                continue;
            }
            float dx = e.x - ox;
            float dy = e.y - oy;
            float dz = e.z - oz;
            if (dx * dx + dy * dy + dz * dz <= r2) {
                out.add(e);
            }
        }
        return out;
    }

    public static boolean anyInCull(float ox, float oy, float oz, float radius) {
        return !entriesInCull(ox, oy, oz, radius).isEmpty();
    }

    /**
     * Convert camera-relative MODELVIEW (gun root) to world-space column-major 4×4.
     * Uses a yaw-stable camera basis when looking nearly straight up/down (avoids orbiting shadows).
     */
    @Nullable
    public static float[] camRelativeMvToWorldMatrix(Matrix4f camMv) {
        if (camMv == null) {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null) {
            return null;
        }
        float pt = mc.getRenderPartialTicks();
        Vec3d camPos = ActiveRenderInfo.projectViewFromEntity(view, pt);
        Vec3d camForward = view.getLook(pt);
        if (mc.gameSettings.thirdPersonView == 2) {
            camForward = camForward.scale(-1.0D);
        }
        if (camForward.lengthSquared() < 1.0E-12D) {
            camForward = new Vec3d(0, 0, 1);
        } else {
            camForward = camForward.normalize();
        }

        Vec3d worldUp = new Vec3d(0.0D, 1.0D, 0.0D);
        Vec3d camRight;
        // |forward·up|≈1 → cross(forward, up) vanishes; build right from yaw (MC: yaw0 = +Z).
        if (Math.abs(camForward.y) > 0.999D) {
            float yaw = view.rotationYaw * 0.017453292F;
            Vec3d horizFwd = new Vec3d(-MathHelper.sin(yaw), 0.0D, MathHelper.cos(yaw));
            camRight = horizFwd.crossProduct(worldUp);
            if (camRight.lengthSquared() < 1.0E-12D) {
                camRight = new Vec3d(1.0D, 0.0D, 0.0D);
            } else {
                camRight = camRight.normalize();
            }
        } else {
            camRight = camForward.crossProduct(worldUp);
            if (camRight.lengthSquared() < 1.0E-12D) {
                camRight = new Vec3d(1.0D, 0.0D, 0.0D);
            } else {
                camRight = camRight.normalize();
            }
        }
        Vec3d camUp = camRight.crossProduct(camForward);
        if (camUp.lengthSquared() < 1.0E-12D) {
            camUp = worldUp;
        } else {
            camUp = camUp.normalize();
        }

        Vec3d tx = camDirToWorld(camMv, 1f, 0f, 0f, camRight, camUp, camForward);
        Vec3d ty = camDirToWorld(camMv, 0f, 1f, 0f, camRight, camUp, camForward);
        Vec3d tz = camDirToWorld(camMv, 0f, 0f, 1f, camRight, camUp, camForward);
        camMv.transformPosition(0f, 0f, 0f, TMP);
        Vec3d tw = camPos.add(camRight.scale(TMP.x)).add(camUp.scale(TMP.y)).add(camForward.scale(-TMP.z));

        return new float[] {
                (float) tx.x, (float) tx.y, (float) tx.z, 0f,
                (float) ty.x, (float) ty.y, (float) ty.z, 0f,
                (float) tz.x, (float) tz.y, (float) tz.z, 0f,
                (float) tw.x, (float) tw.y, (float) tw.z, 1f
        };
    }

    private static Vec3d camDirToWorld(Matrix4f camMv, float dx, float dy, float dz,
            Vec3d camRight, Vec3d camUp, Vec3d camForward) {
        camMv.transformDirection(dx, dy, dz, TMP);
        return camRight.scale(TMP.x).add(camUp.scale(TMP.y)).add(camForward.scale(-TMP.z));
    }
}
