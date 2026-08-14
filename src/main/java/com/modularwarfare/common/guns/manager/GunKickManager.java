package com.modularwarfare.common.guns.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.utility.RayUtil;
import com.modularwarfare.utility.raycast.hits.BulletHit;
import com.modularwarfare.utility.raycast.hits.OBBHit;
import com.modularwarfare.utility.raycast.obb.OBBModelBox;
import com.modularwarfare.utility.raycast.obb.OBBPlayerManager;
import com.modularwarfare.utility.raycast.obb.OBBPlayerManager.OBBDebugObject;
import com.modularwarfare.utility.vector.Matrix4f;
import com.modularwarfare.utility.vector.Vector3f;
import com.teamderpy.shouldersurfing.client.ShoulderInstance;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;

public class GunKickManager {
    private static Map<UUID, AimingData> playerAimingData = new HashMap<>();

    public static GunKickManager.AimingData getAimingData(EntityPlayer player) {
        UUID id = player.getUniqueID();
        if (!GunKickManager.playerAimingData.containsKey(id)) {
            GunKickManager.playerAimingData.put(id, new GunKickManager.AimingData());
        }
        return GunKickManager.playerAimingData.get(id);
    }
    
    public static class AimingData {
        public float pitch;
        public float yaw;
        public List<BulletHit> rayTraceList = new ArrayList<>();
        public long lastUpdateTime;
        private static boolean showBulletTrajectory = true; // 是否显示弹道-调试用
    
        public void update(EntityPlayer player, ItemGun itemGun) {
            // 添加服务端检查，如果是服务端则直接返回
            if (player.world != null && !player.world.isRemote) {
                return;
            }
    
            if (System.currentTimeMillis() - lastUpdateTime < 50) {
                if (OBBPlayerManager.debug && showBulletTrajectory && player.world.isRemote) {
                    renderDebugLine(player, itemGun);
                }
                return;
            }
            updateForced(player, itemGun);
        }
    
        private void renderDebugLine(EntityPlayer player, ItemGun itemGun) {
            if (!player.world.isRemote)
                return;
    
            if (!showBulletTrajectory)
                return;
    
            List<OBBDebugObject> lines = new ArrayList<>();
            Vec3d origin = player.getPositionEyes(ClientProxy.renderHooks.partialTicks);
    
            // 渲染所有命中点的射线
            for (BulletHit hit : rayTraceList) {
                if (hit.rayTraceResult != null && hit.rayTraceResult.hitVec != null) {
                    addRayRender(lines, origin, hit.rayTraceResult.hitVec, (float)hit.distance);
    
                    // 添加命中点标记
                    lines.add(new OBBDebugObject(new Vector3f(hit.rayTraceResult.hitVec)));
    
                    // 如果是OBB命中,显示命中的OBB
                    if (hit instanceof OBBHit) {
                        lines.add(new OBBDebugObject(((OBBHit)hit).box));
                    }
                }
            }
    
            if (rayTraceList.isEmpty()) {
                Vec3d forward = RayUtil.getGunAccuracy(pitch, yaw, 0, player.world.rand, player);
                Vec3d endVec = origin.add(forward.scale(itemGun.type.weaponMaxRange));
                addRayRender(lines, origin, endVec, (float)itemGun.type.weaponMaxRange);
            }
    
            // 更新渲染列表
            OBBPlayerManager.lines.clear();
            OBBPlayerManager.lines.addAll(lines);
        }
    
        private void addRayRender(List<OBBDebugObject> lines, Vec3d origin, Vec3d end, float distance) {
            Vector3f rayVec = new Vector3f((float)(end.x - origin.x), (float)(end.y - origin.y), (float)(end.z - origin.z));
            Vector3f normaliseVec = rayVec.normalise(null);
    
            OBBModelBox ray = new OBBModelBox();
            float pitchRad = (float)Math.asin(normaliseVec.y);
            normaliseVec.y = 0;
            normaliseVec = normaliseVec.normalise(null);
            float yawRad = (float)Math.asin(normaliseVec.x);
            if (normaliseVec.z < 0) {
                yawRad = (float)(Math.PI - yawRad);
            }
    
            Matrix4f matrix = new Matrix4f();
            matrix.rotate(yawRad, new Vector3f(0, 1, 0));
            matrix.rotate(pitchRad, new Vector3f(-1, 0, 0));
    
            ray.center = new Vector3f((float)(origin.x + end.x) * 0.5f, (float)(origin.y + end.y) * 0.5f, (float)(origin.z + end.z) * 0.5f);
            ray.axis.x = new Vector3f(0, 0, 0);
            ray.axis.y = new Vector3f(0, 0, 0);
            ray.axis.z = Matrix4f.transform(matrix, new Vector3f(0, 0, distance / 2), null);
            ray.axisNormal.x = Matrix4f.transform(matrix, new Vector3f(1, 0, 0), null);
            ray.axisNormal.y = Matrix4f.transform(matrix, new Vector3f(0, 1, 0), null);
            ray.axisNormal.z = Matrix4f.transform(matrix, new Vector3f(0, 0, 1), null);
    
            lines.add(new OBBDebugObject(ray));
            lines.add(new OBBDebugObject(new Vector3f((float)origin.x, (float)origin.y, (float)origin.z), new Vector3f((float)end.x, (float)end.y, (float)end.z)));
        }
    
        public void updateForced(EntityPlayer player, ItemGun itemGun) {
            lastUpdateTime = System.currentTimeMillis();
            if (player.world.isRemote) {
    
                ItemStack heldItem = player.getHeldItemMainhand();
                if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemGun)) {
                    rayTraceList.clear();
                    return;
                }
    
                Minecraft mc = Minecraft.getMinecraft();
                Entity entity = mc.getRenderViewEntity();
                pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * ClientProxy.renderHooks.partialTicks;
                yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * ClientProxy.renderHooks.partialTicks;
    
                if (ClientProxy.shoulderSurfingLoaded) {
                    if (ShoulderInstance.getInstance().doShoulderSurfing()) {
                        RayTraceResult r = getMouseOver(ClientProxy.renderHooks.partialTicks);
                        if (r != null && r.hitVec != null) {
                            Vec3d eye = entity.getPositionEyes(ClientProxy.renderHooks.partialTicks);
                            double posX = r.hitVec.x - eye.x;
                            double posY = r.hitVec.y - eye.y;
                            double posZ = r.hitVec.z - eye.z;
                            double horiz = Math.sqrt(posX * posX + posZ * posZ);
                            pitch = (float) -Math.toDegrees(Math.atan2(posY, Math.max(horiz, 1.0E-6D)));
                            yaw = (float) Math.toDegrees(Math.atan2(-posX, posZ));
                        }
                    }
                }
    
                int numBullets = itemGun.type.numBullets;
                ItemBullet bulletItem = ItemGun.getUsedBullet(heldItem, itemGun.type);
                boolean isSlug = false;
                if (bulletItem != null && bulletItem.type.isSlug) {
                    isSlug = true;
                }
    
                rayTraceList.clear();
                if (isSlug || numBullets <= 1) {
                    List<BulletHit> rayTrace = RayUtil.standardEntityRayTrace(Side.CLIENT, player.world, pitch, yaw, player, itemGun.type.weaponMaxRange, itemGun);
                    if (rayTrace != null) {
                        rayTraceList.addAll(rayTrace);
                    }
                } else {
                    for (int i = 0; i < numBullets; i++) {
                        List<BulletHit> rayTrace = RayUtil.standardEntityRayTrace(Side.CLIENT, player.world, pitch, yaw, player, itemGun.type.weaponMaxRange, itemGun);
                        if (rayTrace != null) {
                            rayTraceList.addAll(rayTrace);
                        }
                    }
                }
    
                // 添加调试渲染
                if (OBBPlayerManager.debug) {
                    renderDebugLine(player, itemGun);
                }
            }
        }
    }
    
    private static RayTraceResult getMouseOver(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity entity = mc.getRenderViewEntity();
        if (entity == null || mc.world == null) {
            return null;
        }
        return RayUtil.shoulderCrosshairPick(entity, partialTicks, 128.0D);
    }

}
