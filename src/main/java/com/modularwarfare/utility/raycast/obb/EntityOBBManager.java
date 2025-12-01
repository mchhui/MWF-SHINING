package com.modularwarfare.utility.raycast.obb;

import java.util.HashMap;
import java.util.UUID;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 实体OBB管理器
 * 管理所有实体的OBB碰撞箱（包括玩家、怪物等）
 * 提供统一的API供外部模块使用
 */
@SideOnly(Side.CLIENT)
public class EntityOBBManager {
    
    /**
     * 存储所有实体的OBB对象
     * Key: 实体UUID
     * Value: OBB对象
     */
    private static HashMap<UUID, OBBModelObject> entityOBBMap = new HashMap<>();
    
    /**
     * 注册实体的OBB对象
     * @param entityUUID 实体UUID
     * @param obbObject OBB对象
     */
    public static void registerEntityOBB(UUID entityUUID, OBBModelObject obbObject) {
        entityOBBMap.put(entityUUID, obbObject);
    }
    
    /**
     * 获取实体的OBB对象
     * @param entityUUID 实体UUID
     * @return OBB对象，如果不存在则返回null
     */
    public static OBBModelObject getEntityOBB(UUID entityUUID) {
        return entityOBBMap.get(entityUUID);
    }
    
    /**
     * 移除实体的OBB对象
     * @param entityUUID 实体UUID
     */
    public static void removeEntityOBB(UUID entityUUID) {
        entityOBBMap.remove(entityUUID);
    }
    
    /**
     * 检查实体是否已注册OBB
     * @param entityUUID 实体UUID
     * @return 如果已注册返回true
     */
    public static boolean hasEntityOBB(UUID entityUUID) {
        return entityOBBMap.containsKey(entityUUID);
    }
    
    /**
     * 清空所有OBB对象（通常在退出世界时调用）
     */
    public static void clearAll() {
        entityOBBMap.clear();
    }
    
    /**
     * 获取所有已注册的实体UUID
     */
    public static java.util.Set<UUID> getAllEntityUUIDs() {
        return entityOBBMap.keySet();
    }
    
    /**
     * 渲染所有实体的OBB调试框（在世界坐标系中，调用者已应用相机变换）
     */
    public static void renderAllDebugBoxes() {
        if (entityOBBMap.isEmpty()) {
            return;
        }
        
        int heCount = 0;
        int otherCount = 0;
        
        for (OBBModelObject obbObject : entityOBBMap.values()) {
            // 直接渲染，不应用相机偏移（调用者已经应用了）
            if (obbObject instanceof HEEntityOBBObject) {
                ((HEEntityOBBObject) obbObject).renderDebugWireframe();
                heCount++;
            } else {
                obbObject.renderDebugBoxes();
                obbObject.renderDebugAixs();
                otherCount++;
            }
        }
        
        // 定期输出统计信息（约1%的帧）
        if (com.modularwarfare.ModConfig.INSTANCE.debug_hits_message && Math.random() < 0.01) {
            com.modularwarfare.ModularWarfare.LOGGER.info("[EntityOBBManager] 渲染OBB: HE实体=" + heCount + ", 其他实体=" + otherCount);
        }
    }
}

