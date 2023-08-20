package mchhui.hegltf;

import java.nio.FloatBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.util.vector.Quaternion;

import mchhui.hegltf.DataAnimation.Transform;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * @author Hueihuea
 */
public class GltfRenderModel {
    private static final HashSet<String> setObj = new HashSet<String>();
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final Comparator<DataMaterial> COMPARATOR_MATE = new Comparator() {

        @Override
        public int compare(Object o1, Object o2) {
            // TODO Auto-generated method stub
            return ((DataMaterial)o1).isTranslucent && !((DataMaterial)o2).isTranslucent ? 1 : -1;
        }

    };

    public HashMap<String, NodeState> nodeStates = new HashMap<String, NodeState>();

    public GltfDataModel geoModel;

    public GltfDataModel lastAniModel;
    public GltfDataModel aniModel;

    protected boolean initedNodeStates = false;
    protected int jointMatsBufferId = -1;

    public static class NodeState {
        public Matrix4f mat = new Matrix4f();
    }

    public GltfRenderModel(GltfDataModel geoModel) {
        this.geoModel = geoModel;
    }

    public void calculateAllNodePose(float time) {
        if (!initedNodeStates) {
            geoModel.nodes.keySet().forEach((name) -> {
                nodeStates.put(name, new NodeState());
            });
            initedNodeStates = true;
        }
        for (Entry<String, DataNode> entry : geoModel.rootNodes.entrySet()) {
            calculateNodeAndChildren(entry.getValue(), null, time);
        }
    }

    public void calculateNodeAndChildren(DataNode node, Matrix4f parent, float time) {
        Matrix4f matrix = new Matrix4f();
        DataAnimation animation = geoModel.animations.get(node.name);
        if (animation != null) {
            Transform trans = animation.findTransform(time, node.pos, node.size, node.rot);
            matrix.translate(trans.pos.x, trans.pos.y, trans.pos.z);
            matrix.scale(trans.size.x, trans.size.y, trans.size.z);
            matrix.rotate(trans.rot);
        } else {
            matrix.translate(node.pos);
            matrix.scale(node.size);
            matrix.rotate(node.rot);
        }
        if (parent != null) {
            matrix.mulLocal(parent);
        }
        nodeStates.get(node.name).mat = matrix;
        for (String name : node.childlist) {
            calculateNodeAndChildren(geoModel.nodes.get(name), matrix, time);
        }
    }

    public void uploadAllJointTransform() {
        if (geoModel.joints.size() == 0) {
            return;
        }
        if (jointMatsBufferId == -1) {
            jointMatsBufferId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointMatsBufferId);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, geoModel.joints.size() * 64, GL15.GL_DYNAMIC_DRAW);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointMatsBufferId);
        for (int i = 0; i < geoModel.joints.size(); i++) {
            Matrix4f inv = geoModel.inverseBindMatrices.get(i);
            Matrix4f pose = nodeStates.get(geoModel.joints.get(i)).mat;
            Matrix4f result = new Matrix4f(pose);
            result.mul(inv);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, i * 64, result.get(MATRIX_BUFFER));
        }
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void skinNodeAndChildren(DataNode node, HashSet<String> sun, HashSet<String> moon) {
        if (sun != null && !sun.isEmpty() && !sun.contains(node.name)) {
            return;
        }
        if (moon != null && !moon.isEmpty() && moon.contains(node.name)) {
            return;
        }
        if (geoModel.joints.size() == 0) {
            return;
        }
        if (jointMatsBufferId == -1) {
            return;
        }
        node.meshes.values().forEach((mesh) -> {
            mesh.callSkinning();
        });
        node.childlist.forEach((child) -> {
            skinNodeAndChildren(geoModel.nodes.get(child), sun, moon);
        });
    }

    public boolean updateAnimation(float time, boolean skin) {
        //System.out.println(time);
        if (!geoModel.loaded) {
            return false;
        }
        calculateAllNodePose(time);
        uploadAllJointTransform();
        if (skin) {
            if (geoModel.joints.size() > 0) {
                ShaderGltf.useShader();
                GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.JOINTMATSBUFFERBINDING,
                    jointMatsBufferId);

                GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
                for (Entry<String, DataNode> e : geoModel.rootNodes.entrySet()) {
                    skinNodeAndChildren(e.getValue(), null, null);
                }
                GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

                GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.JOINTMATSBUFFERBINDING, 0);
                GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.VERTEXBUFFERBINDING, 0);
                GL20.glUseProgram(0);
            }
        }
        return true;
    }

    // 阴阳！哈哈哈 下次试试aplle和pear XD
    public void render(HashSet<String> sun, HashSet<String> moon) {
        if (!geoModel.loaded) {
            return;
        }

        for (Entry<String, DataNode> e : geoModel.nodes.entrySet()) {
            if (sun != null && !sun.isEmpty() && !sun.contains(e.getKey())) {
                continue;
            }
            if (moon != null && !moon.isEmpty() && moon.contains(e.getKey())) {
                continue;
            }
            e.getValue().meshes.values().forEach((mesh) -> {
                System.out.println("a");
                GlStateManager.pushMatrix();
                if (!mesh.skin) {
                    GlStateManager.multMatrix(nodeStates.get(e.getValue().name).mat.get(MATRIX_BUFFER));
                }
                mesh.render();
                GlStateManager.popMatrix();
            });
        }
    }

    public void renderAll() {
        render(null, null);
    }

    @Deprecated
    public void renderPart(String part) {
        HashSet<String> set = setObj;
        setObj.clear();
        set.add(part);
        render(set, null);
    }

    @Deprecated
    public void renderOnly(String[] part) {
        HashSet<String> set = setObj;
        setObj.clear();
        for (int i = 0; i < part.length; i++) {
            set.add(part[i]);
        }
        renderOnly(set);
    }

    @Deprecated
    public void renderExcept(String[] part) {
        HashSet<String> set = setObj;
        setObj.clear();
        for (int i = 0; i < part.length; i++) {
            set.add(part[i]);
        }
        renderExcept(set);
    }

    public void renderOnly(HashSet<String> part) {
        render(part, null);
    }

    public void renderExcept(HashSet<String> part) {
        render(null, part);
    }
}
