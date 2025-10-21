package com.modularwarfare.raycast.obb;

import java.util.HashMap;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * HE实体OBB对象
 * 处理从HE引擎的GLTF模型中获取的upc_节点数据
 * 将其转换为OBB碰撞箱结构
 */
@SideOnly(Side.CLIENT)
public class HEEntityOBBObject extends OBBModelObject {
    
    /**
     * 实体引用
     */
    private EntityLivingBase entity;
    
    /**
     * 实体变换（位置和旋转）
     */
    private float entityX, entityY, entityZ;
    private float entityYaw, entityPitch;
    
    /**
     * UPC节点数据（从HE引擎获取）
     */
    private HashMap<String, UPCNodeData> upcNodeDataMap = new HashMap<>();
    
    /**
     * 节点名称到Bone的映射
     */
    private HashMap<String, OBBModelBone> nodeToBoneMap = new HashMap<>();
    
    /**
     * UPC节点数据结构
     */
    public static class UPCNodeData {
        public String nodeName;
        public Vector3f position = new Vector3f();
        public Quaternionf rotation = new Quaternionf();
        public Vector3f scale = new Vector3f();
        public Matrix4f worldMatrix = new Matrix4f();
    }
    
    public HEEntityOBBObject() {
        super();
        // 初始化场景
        this.scene = new OBBModelScene();
    }
    
    /**
     * 设置关联的实体
     */
    public void setEntity(EntityLivingBase entity) {
        this.entity = entity;
    }
    
    /**
     * 获取关联的实体
     */
    public EntityLivingBase getEntity() {
        return entity;
    }
    
    /**
     * 更新实体的变换（位置和旋转）
     */
    public void updateEntityTransform(float x, float y, float z, float yaw, float pitch) {
        this.entityX = x;
        this.entityY = y;
        this.entityZ = z;
        this.entityYaw = yaw;
        this.entityPitch = pitch;
    }
    
    // 首次更新标志，用于输出初始矩阵数据
    private boolean firstUpdate = true;
    
     /**
      * 更新UPC节点数据（从HE引擎）
      * @param nodeDataList UPC节点数据列表
      */
     public void updateUPCNodeData(List<UPCNodeData> nodeDataList) {
         upcNodeDataMap.clear();
         
         for (UPCNodeData data : nodeDataList) {
             upcNodeDataMap.put(data.nodeName, data);
             
             // 如果还没有创建对应的Bone，创建它
             if (!nodeToBoneMap.containsKey(data.nodeName)) {
                 createBoneForNode(data);
                 if (com.modularwarfare.ModConfig.INSTANCE.debug_hits_message) {
                     com.modularwarfare.ModularWarfare.LOGGER.info("[HEEntityOBBObject] 为节点 " + data.nodeName + " 创建OBB, 尺寸: " + data.scale);
                 }
             }
             
             // 首次更新时输出原始矩阵数据
             if (firstUpdate && com.modularwarfare.ModConfig.INSTANCE.debug_hits_message) {
                 com.modularwarfare.ModularWarfare.LOGGER.info("[HEEntityOBBObject] 节点 " + data.nodeName + 
                     " 原始位置: " + data.position + " 缩放: " + data.scale);
                 com.modularwarfare.ModularWarfare.LOGGER.info("[HEEntityOBBObject] 节点 " + data.nodeName + 
                     " 世界矩阵位置: (" + data.worldMatrix.m30() + ", " + data.worldMatrix.m31() + ", " + data.worldMatrix.m32() + ")");
             }
         }
         
         firstUpdate = false;
     }
    
    /**
     * 为UPC节点创建OBB骨骼和碰撞箱
     */
    private void createBoneForNode(UPCNodeData nodeData) {
        // 创建Bone
        OBBModelBone bone = new OBBModelBone();
        bone.name = nodeData.nodeName;
        bone.oirign = new com.modularwarfare.common.vector.Vector3f(0, 0, 0);
        
        // 添加到场景根骨骼
        scene.rootBones.add(bone);
        nodeToBoneMap.put(nodeData.nodeName, bone);
        
        // 创建Box（碰撞箱）
        // 从节点的缩放中提取碰撞箱尺寸
        OBBModelBox box = new OBBModelBox();
        box.name = nodeData.nodeName + "_box";
        
        // Blender中的scale是完整尺寸，需要除以2得到OBB半径
        // OBBModelBox.size 是半径（half-extent），渲染时会 × 2
        // 公式: 渲染尺寸 = size × 2 = (scale / 2) × 2 = scale ✅
        
        // OBBModelBox.size 是半径，渲染时会×2
        // 所以 size = scale × 0.5，渲染 = size × 2 = scale ✅
        // 玩家和非玩家使用相同的scaleFactor
        boolean isPlayer = entity instanceof net.minecraft.entity.player.EntityPlayer;
        float scaleFactor = 0.5f;  // 统一使用0.5
        
        if (com.modularwarfare.ModConfig.INSTANCE.debug_hits_message) {
            com.modularwarfare.ModularWarfare.LOGGER.info(String.format(
                "[HEEntityOBBObject] 节点 %s: 实体类型=%s, scaleFactor=%.1f, scale=(%.3f,%.3f,%.3f) -> size=(%.3f,%.3f,%.3f)",
                nodeData.nodeName,
                isPlayer ? "玩家" : "非玩家",
                scaleFactor,
                1, 1, 1,
                1 * scaleFactor, 1 * scaleFactor, 1 * scaleFactor
            ));
        }
        
        box.size = new com.modularwarfare.common.vector.Vector3f(
            1 * scaleFactor,
            1 * scaleFactor,
            1 * scaleFactor
        );
        
        // 中心点相对于bone原点
        box.center = new com.modularwarfare.common.vector.Vector3f(0, 0, 0);
        box.anchor = new com.modularwarfare.common.vector.Vector3f(0, 0, 0);
        box.rotation = new com.modularwarfare.common.vector.Vector3f(0, 0, 0);
        
        // 添加box到对象
        boxes.add(box);
        boneBinding.put(box, bone);
    }
    
    // 调试计数器，减少日志输出频率
    private int debugFrameCounter = 0;
    private static final int DEBUG_INTERVAL = 60; // 每60帧输出一次（约1秒）
    
     /**
      * 更新OBB姿态
      * HE引擎的nodeState.mat是相对于实体的局部坐标，需要结合实体世界变换
      */
     public void updatePose(float partialTicks) {
         int updatedBones = 0;
         
         // 【第一步】构建实体的世界变换矩阵
         com.modularwarfare.common.vector.Matrix4f entityWorldMatrix = new com.modularwarfare.common.vector.Matrix4f();
         entityWorldMatrix.setIdentity();
         entityWorldMatrix.translate(new com.modularwarfare.common.vector.Vector3f(entityX, entityY, entityZ));
         entityWorldMatrix.rotate((float)Math.toRadians(entityYaw), new com.modularwarfare.common.vector.Vector3f(0, -1, 0));
         
         // 【第二步】为每个骨骼计算世界变换并更新碰撞箱
         for (UPCNodeData nodeData : upcNodeDataMap.values()) {
             OBBModelBone bone = nodeToBoneMap.get(nodeData.nodeName);
             if (bone == null) {
                 continue;
             }
             
             // 将HE引擎的局部矩阵转换为MW矩阵
             com.modularwarfare.common.vector.Matrix4f localMatrix = convertJOMLToMW(nodeData.worldMatrix);
             
             // 【关键】手动计算世界矩阵 = 实体世界矩阵 * 局部矩阵
             com.modularwarfare.common.vector.Matrix4f worldMatrix = 
                 com.modularwarfare.common.vector.Matrix4f.mul(entityWorldMatrix, localMatrix, null);
             
             // 存储世界矩阵到bone
             bone.currentPose = worldMatrix;
             
             // 【第三步】直接用世界矩阵计算碰撞箱
             for (OBBModelBox box : boxes) {
                 if (boneBinding.get(box) == bone) {
                     box.compute(new com.modularwarfare.common.vector.Matrix4f(worldMatrix).translate(bone.oirign));
                 }
             }
             
             updatedBones++;
         }
         
        // 调试信息（节流：每60帧输出一次）
        debugFrameCounter++;
        if (com.modularwarfare.ModConfig.INSTANCE.debug_hits_message && updatedBones > 0 && debugFrameCounter >= DEBUG_INTERVAL) {
            debugFrameCounter = 0;
            
            com.modularwarfare.ModularWarfare.LOGGER.info("[HEEntityOBBObject] 实体 " + 
                (entity != null ? entity.getName() : "null") + 
                " (" + (entity != null ? entity.getEntityId() : "null") + ")" +
                " 更新了 " + updatedBones + " 个骨骼, 共 " + boxes.size() + " 个碰撞箱");
            
            // 输出所有碰撞箱的尺寸信息（用于调试尺寸问题）
            com.modularwarfare.ModularWarfare.LOGGER.info("[HEEntityOBBObject] ===== 碰撞箱尺寸详情 =====");
            for (int i = 0; i < boxes.size() && i < 10; i++) {  // 限制输出前10个
                OBBModelBox box = boxes.get(i);
                UPCNodeData nodeData = null;
                for (UPCNodeData data : upcNodeDataMap.values()) {
                    OBBModelBone bone = nodeToBoneMap.get(data.nodeName);
                    if (bone != null && boneBinding.get(box) == bone) {
                        nodeData = data;
                        break;
                    }
                }
                
                if (nodeData != null) {
                    // 计算渲染尺寸（size × 2）
                    float renderX = box.size.x * 2.0f;
                    float renderY = box.size.y * 2.0f;
                    float renderZ = box.size.z * 2.0f;
                    
                    com.modularwarfare.ModularWarfare.LOGGER.info(String.format(
                        "  [%d] %s: Blender(%.3f,%.3f,%.3f) -> size(%.3f,%.3f,%.3f) -> 渲染(%.3f,%.3f,%.3f) 中心(%.1f,%.1f,%.1f)",
                        i, box.name,
                        nodeData.scale.x, nodeData.scale.y, nodeData.scale.z,
                        box.size.x, box.size.y, box.size.z,
                        renderX, renderY, renderZ,
                        box.center.x, box.center.y, box.center.z
                    ));
                }
            }
            
            com.modularwarfare.ModularWarfare.LOGGER.info("[HEEntityOBBObject] 实体世界位置: (" + 
                entityX + ", " + entityY + ", " + entityZ + ") Yaw: " + entityYaw);
        }
     }
    
    /**
     * 将JOML的Matrix4f转换为ModularWarfare的Matrix4f
     */
    private com.modularwarfare.common.vector.Matrix4f convertJOMLToMW(Matrix4f jomlMatrix) {
        com.modularwarfare.common.vector.Matrix4f mwMatrix = new com.modularwarfare.common.vector.Matrix4f();
        
        // 复制矩阵元素
        mwMatrix.m00 = jomlMatrix.m00();
        mwMatrix.m01 = jomlMatrix.m01();
        mwMatrix.m02 = jomlMatrix.m02();
        mwMatrix.m03 = jomlMatrix.m03();
        
        mwMatrix.m10 = jomlMatrix.m10();
        mwMatrix.m11 = jomlMatrix.m11();
        mwMatrix.m12 = jomlMatrix.m12();
        mwMatrix.m13 = jomlMatrix.m13();
        
        mwMatrix.m20 = jomlMatrix.m20();
        mwMatrix.m21 = jomlMatrix.m21();
        mwMatrix.m22 = jomlMatrix.m22();
        mwMatrix.m23 = jomlMatrix.m23();
        
        mwMatrix.m30 = jomlMatrix.m30();
        mwMatrix.m31 = jomlMatrix.m31();
        mwMatrix.m32 = jomlMatrix.m32();
        mwMatrix.m33 = jomlMatrix.m33();
        
        return mwMatrix;
    }
    
    /**
     * 渲染调试线框（白色粗线）
     */
    public void renderDebugWireframe() {
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);
        org.lwjgl.opengl.GL11.glLineWidth(3.0f); // 粗线方便观察
        org.lwjgl.opengl.GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // 白色
        
        for (OBBModelBox box : boxes) {
            renderOBBWireframe(box);
        }
        
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_LIGHTING);
        org.lwjgl.opengl.GL11.glLineWidth(1.0f);
        org.lwjgl.opengl.GL11.glPopMatrix();
    }
    
    /**
     * 渲染单个OBB的线框
     */
    private void renderOBBWireframe(OBBModelBox box) {
        // OBB的8个顶点
        com.modularwarfare.common.vector.Vector3f center = box.center;
        com.modularwarfare.common.vector.Vector3f[] axes = {
            box.axis.x,
            box.axis.y,
            box.axis.z
        };
        
        // 计算8个顶点
        com.modularwarfare.common.vector.Vector3f[] vertices = new com.modularwarfare.common.vector.Vector3f[8];
        for (int i = 0; i < 8; i++) {
            float sx = (i & 1) == 0 ? -1 : 1;
            float sy = (i & 2) == 0 ? -1 : 1;
            float sz = (i & 4) == 0 ? -1 : 1;
            
            vertices[i] = new com.modularwarfare.common.vector.Vector3f(
                center.x + axes[0].x * sx + axes[1].x * sy + axes[2].x * sz,
                center.y + axes[0].y * sx + axes[1].y * sy + axes[2].y * sz,
                center.z + axes[0].z * sx + axes[1].z * sy + axes[2].z * sz
            );
        }
        
        // 渲染12条边
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINES);
        
        // 底面4条边 (z=0)
        drawLine(vertices[0], vertices[1]);
        drawLine(vertices[1], vertices[3]);
        drawLine(vertices[3], vertices[2]);
        drawLine(vertices[2], vertices[0]);
        
        // 顶面4条边 (z=1)
        drawLine(vertices[4], vertices[5]);
        drawLine(vertices[5], vertices[7]);
        drawLine(vertices[7], vertices[6]);
        drawLine(vertices[6], vertices[4]);
        
        // 连接底面和顶面的4条边
        drawLine(vertices[0], vertices[4]);
        drawLine(vertices[1], vertices[5]);
        drawLine(vertices[2], vertices[6]);
        drawLine(vertices[3], vertices[7]);
        
        org.lwjgl.opengl.GL11.glEnd();
    }
    
    /**
     * 绘制一条线
     */
    private void drawLine(com.modularwarfare.common.vector.Vector3f start, com.modularwarfare.common.vector.Vector3f end) {
        org.lwjgl.opengl.GL11.glVertex3f(start.x, start.y, start.z);
        org.lwjgl.opengl.GL11.glVertex3f(end.x, end.y, end.z);
    }
    
    /**
     * 清理资源
     */
    public void dispose() {
        upcNodeDataMap.clear();
        nodeToBoneMap.clear();
        boxes.clear();
        boneBinding.clear();
        scene.rootBones.clear();
    }
}

