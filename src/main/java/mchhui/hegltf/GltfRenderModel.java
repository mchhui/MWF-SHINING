package mchhui.hegltf;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

import com.modularwarfare.client.compat.AtomicShaderCompat;
import com.modularwarfare.utility.OptifineHelper;

import mchhui.hegltf.DataAnimation.Transform;
import net.minecraft.client.renderer.GlStateManager;

/**
 * @author Hueihuea
 */
public class GltfRenderModel {
    private static final HashSet<String> setObj = new HashSet<String>();
    /** Scratch for joint matrix = pose * inverseBind (no per-joint alloc). */
    private static final Matrix4f JOINT_SCRATCH = new Matrix4f();
    private static final Comparator<DataMesh> COMPARE_DRAW_VAO = new Comparator<DataMesh>() {
        @Override
        public int compare(DataMesh a, DataMesh b) {
            return Integer.compare(a.getDrawVao(), b.getDrawVao());
        }
    };
    private static final Comparator<DataMaterial> COMPARATOR_MATE = new Comparator() {

        @Override
        public int compare(Object o1, Object o2) {
            // TODO Auto-generated method stub
            return ((DataMaterial)o1).isTranslucent && !((DataMaterial)o2).isTranslucent ? 1 : -1;
        }

    };

    public HashMap<String, NodeState> nodeStates = new HashMap<String, NodeState>();
    public NodeAnimationBlender animationCalBlender;
    public NodeAnimationMapper animationLoadMapper;

    public GltfDataModel geoModel;

    public GltfDataModel lastAniModel;
    public GltfDataModel aniModel;

    protected boolean initedNodeStates = false;
    protected int jointMatsBufferId = -1;
    /** Packed joint matrices for one glBufferSubData upload. */
    private FloatBuffer jointMatsUpload;
    /** Nodes that own at least one mesh (bones-only skipped at draw). */
    private List<DataNode> meshNodes;
    private boolean meshNodesBuilt = false;
    /** Scratch lists reused every render (no per-frame alloc). */
    private final ArrayList<DataMesh> skinDrawList = new ArrayList<DataMesh>();
    private final ArrayList<DataNode> rigidNodeList = new ArrayList<DataNode>();
    private final ArrayList<DataMesh> rigidMeshScratch = new ArrayList<DataMesh>();
    /** Per-model MV buffers (must not be static — nested models would clobber). */
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer baseMvBuffer = BufferUtils.createFloatBuffer(16);
    private final Matrix4f baseMv = new Matrix4f();
    private final Matrix4f composedMv = new Matrix4f();
    private int drawScopeDepth = 0;
    private boolean baseMvValid = false;

    public static class NodeState {
        public Matrix4f mat = new Matrix4f();
    }

    public static class NodeAnimationBlender {
        public String name;

        public NodeAnimationBlender(String name) {
            this.name = name;
        }

        public void handle(DataNode node, Matrix4f mat) {

        }
    }
    
    public static class NodeAnimationMapper {
        public String name;

        public NodeAnimationMapper(String name) {
            this.name = name;
        }

        public void handle(GltfRenderModel model,GltfRenderModel other,String target) {

        }
    }

    public void setNodeAnimationCalBlender(NodeAnimationBlender blender) {
        animationCalBlender=blender;
    }
    
    public void setNodeAnimationLoadMapper(NodeAnimationMapper mapper) {
        animationLoadMapper=mapper;
    }

    public GltfRenderModel(GltfDataModel geoModel) {
        this.geoModel = geoModel;
    }

    public void bindGeoModel(GltfDataModel geoModel) {
        if (this.geoModel == geoModel) {
            return;
        }
        this.geoModel = geoModel;
        this.initedNodeStates = false;
        this.nodeStates.clear();
        this.meshNodesBuilt = false;
        this.meshNodes = null;
        if (this.jointMatsBufferId != -1) {
            GL15.glDeleteBuffers(this.jointMatsBufferId);
            this.jointMatsBufferId = -1;
        }
        this.jointMatsUpload = null;
        clearThirdIdleSkinMark();
    }

    /** Last successful frozen-idle {@link #skinFromPose} (skip re-skin when time unchanged). */
    private float thirdIdleSkinTime = Float.NaN;

    public boolean isThirdIdleSkinReady(float time) {
        return jointMatsBufferId != -1 && Float.compare(thirdIdleSkinTime, time) == 0;
    }

    public void markThirdIdleSkin(float time) {
        thirdIdleSkinTime = time;
    }

    public void clearThirdIdleSkinMark() {
        thirdIdleSkinTime = Float.NaN;
    }

    /**
     * Skin SSBO targets ready for draw: every compiled skin mesh has been skinned at least once.
     * Returns false while meshes are still uploading ({@code !compiled}) so callers must not cache-skip.
     */
    public boolean hasInitializedSkinBuffers() {
        if (geoModel == null || geoModel.nodes == null) {
            return false;
        }
        if (geoModel.joints == null || geoModel.joints.isEmpty()) {
            return true;
        }
        for (DataNode node : geoModel.nodes.values()) {
            if (node == null || node.meshes == null) {
                continue;
            }
            for (DataMesh mesh : node.meshes.values()) {
                if (mesh == null || !mesh.skin) {
                    continue;
                }
                if (!mesh.isCompiled() || !mesh.isSkinInitialized()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void invalidateMeshNodes() {
        meshNodesBuilt = false;
        meshNodes = null;
    }

    private void ensureMeshNodes() {
        if (meshNodesBuilt) {
            return;
        }
        meshNodes = new ArrayList<>();
        if (geoModel == null || geoModel.nodes == null) {
            meshNodesBuilt = true;
            return;
        }
        for (DataNode node : geoModel.nodes.values()) {
            if (node.meshes != null && !node.meshes.isEmpty()) {
                meshNodes.add(node);
            }
        }
        meshNodesBuilt = true;
    }

    public void calculateAllNodePose(float time) {
        if (geoModel == null) {
            return;
        }
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
        Matrix4f matrix = nodeStates.get(node.name).mat;
        matrix.identity();
        DataAnimation animation = geoModel.animations.get(node.name);
        if (animation != null) {
            Transform trans = animation.findTransform(time, node.pos, node.size, node.rot);
            matrix.translate(trans.pos.x, trans.pos.y, trans.pos.z);
            matrix.rotate(trans.rot);
            matrix.scale(trans.size.x, trans.size.y, trans.size.z);
        } else {
            matrix.translate(node.pos);
            matrix.rotate(node.rot);
            matrix.scale(node.size);
        }
        
        if(animationCalBlender!=null) {
            animationCalBlender.handle(node, matrix);
        }
        
        if (parent != null) {
            matrix.mulLocal(parent);
        }
        
        for (String name : node.childlist) {
            calculateNodeAndChildren(geoModel.nodes.get(name), matrix, time);
        }
    }

    public void uploadAllJointTransform() {
        if (geoModel.joints.size() == 0) {
            return;
        }
        int jointCount = geoModel.joints.size();
        if (jointMatsBufferId == -1) {
            jointMatsBufferId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointMatsBufferId);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, jointCount * 64L, GL15.GL_DYNAMIC_DRAW);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }

        int floats = jointCount * 16;
        if (jointMatsUpload == null || jointMatsUpload.capacity() < floats) {
            jointMatsUpload = BufferUtils.createFloatBuffer(floats);
        }
        jointMatsUpload.clear();
        for (int i = 0; i < jointCount; i++) {
            Matrix4f inv = geoModel.inverseBindMatrices.get(i);
            Matrix4f pose = nodeStates.get(geoModel.joints.get(i)).mat;
            JOINT_SCRATCH.set(pose);
            JOINT_SCRATCH.mul(inv);
            JOINT_SCRATCH.get(i * 16, jointMatsUpload);
        }
        jointMatsUpload.limit(floats);
        jointMatsUpload.position(0);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointMatsBufferId);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, jointMatsUpload);
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

    /** Pose only (nodeStates). Blender must already be set if needed. */
    public boolean updatePose(float time) {
        if (geoModel == null || !geoModel.isAnimReady()) {
            return false;
        }
        calculateAllNodePose(time);
        return true;
    }

    /** Upload joints + GPU skin from current nodeStates. */
    public void skinFromPose() {
        if (geoModel == null || !geoModel.isAnimReady()) {
            return;
        }
        if (geoModel.joints.size() == 0) {
            if (AtomicShaderCompat.isGBufferFillActive() || AtomicShaderCompat.isShadowDepthActive()) {
                restoreProgramAfterSkinCompute();
            }
            return;
        }
        uploadAllJointTransform();
        runSkinCompute();
    }

    private void runSkinCompute() {
        if (jointMatsBufferId == -1) {
            return;
        }
        ShaderGltf.useShader();
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.JOINTMATSBUFFERBINDING,
            jointMatsBufferId);
        GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
        try {
            for (Entry<String, DataNode> e : geoModel.rootNodes.entrySet()) {
                skinNodeAndChildren(e.getValue(), null, null);
            }
        } finally {
            GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.JOINTMATSBUFFERBINDING, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.VERTEXBUFFERBINDING, 0);
            restoreProgramAfterSkinCompute();
        }
    }
    
    public boolean loadAnimation(GltfRenderModel other,boolean skin) {
        if (other == null || geoModel == null || other.geoModel == null) {
            return false;
        }
        if(!other.initedNodeStates) {
            return false;
        }
        if (!initedNodeStates) {
            geoModel.nodes.keySet().forEach((name) -> {
                nodeStates.put(name, new NodeState());
            });
            initedNodeStates = true;
        }
        nodeStates.forEach((k,v)->{
            NodeState s=other.nodeStates.get(k);
            if(s!=null) {
                v.mat.set(s.mat);
            }
            if(animationLoadMapper!=null) {
                animationLoadMapper.handle(this,other, k);
            }
        });
        if (skin) {
            skinFromPose();
        }
        return true;
    }

    public boolean updateAnimation(float time, boolean skin) {
        if (!updatePose(time)) {
            return false;
        }
        if (skin) {
            skinFromPose();
        }
        return true;
    }

    /** After skin SSBO compute: restore Atomic fill/shadow or OptiFine program (do not leave program 0). */
    private static void restoreProgramAfterSkinCompute() {
        if (AtomicShaderCompat.isShadowDepthActive()) {
            AtomicShaderCompat.rebindAtomicCaptureIfActive();
            return;
        }
        if (AtomicShaderCompat.isGBufferFillActive()) {
            AtomicShaderCompat.markFillCaptureDirty();
            AtomicShaderCompat.rebindFillIfActive();
            return;
        }
        if (OptifineHelper.isShadersEnabled()) {
            GL20.glUseProgram(OptifineHelper.getProgram());
        } else {
            GL20.glUseProgram(0);
        }
    }

    private void loadModelView(Matrix4f mat) {
        matrixBuffer.clear();
        mat.get(matrixBuffer);
        matrixBuffer.rewind();
        GL11.glLoadMatrix(matrixBuffer);
    }

    private void readBaseModelView() {
        baseMvBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, baseMvBuffer);
        baseMvBuffer.rewind();
        baseMv.set(baseMvBuffer);
        baseMvValid = true;
    }

    public void beginDrawScope() {
        if (!GltfFeatureFlags.skinAnimOpt()) {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            AtomicShaderCompat.rebindFillAndGunPbr();
            return;
        }
        if (drawScopeDepth++ > 0) {
            return;
        }
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        AtomicShaderCompat.rebindFillAndGunPbr();
        DataMesh.beginBatch();
        readBaseModelView();
    }

    public void endDrawScope() {
        if (!GltfFeatureFlags.skinAnimOpt()) {
            return;
        }
        if (drawScopeDepth <= 0) {
            return;
        }
        if (--drawScopeDepth > 0) {
            return;
        }
        if (baseMvValid) {
            loadModelView(baseMv);
        }
        DataMesh.endBatch();
        baseMvValid = false;
    }

    /** Caller changed GL modelview (push/mult/pop); force re-read on next rigid pass. */
    public void invalidateDrawScopeBase() {
        baseMvValid = false;
    }

    private boolean isExcluded(HashSet<String> moon, String name) {
        if (moon == null || moon.isEmpty()) {
            return false;
        }
        return moon.contains(name);
    }

    private void collectDrawLists(HashSet<String> sun, HashSet<String> moon) {
        skinDrawList.clear();
        rigidNodeList.clear();
        if (sun != null && !sun.isEmpty()) {
            for (String name : sun) {
                if (isExcluded(moon, name)) {
                    continue;
                }
                DataNode node = geoModel.nodes.get(name);
                if (node == null || node.meshes == null || node.meshes.isEmpty()) {
                    continue;
                }
                collectNodeMeshes(node);
            }
        } else {
            ensureMeshNodes();
            for (int i = 0, n = meshNodes.size(); i < n; i++) {
                DataNode node = meshNodes.get(i);
                if (isExcluded(moon, node.name)) {
                    continue;
                }
                collectNodeMeshes(node);
            }
        }
    }

    private void collectNodeMeshes(DataNode node) {
        boolean anyRigid = false;
        for (DataMesh mesh : node.meshes.values()) {
            if (mesh.skin) {
                skinDrawList.add(mesh);
            } else {
                anyRigid = true;
            }
        }
        if (anyRigid) {
            rigidNodeList.add(node);
        }
    }

    private void drawSkinPass() {
        if (skinDrawList.isEmpty()) {
            return;
        }
        // Skin needs the scope base MV (not last rigid node matrix).
        if (baseMvValid) {
            loadModelView(baseMv);
        }
        if (skinDrawList.size() > 1) {
            skinDrawList.sort(COMPARE_DRAW_VAO);
        }
        for (int i = 0, n = skinDrawList.size(); i < n; i++) {
            skinDrawList.get(i).render();
        }
    }

    private void drawRigidPass() {
        if (rigidNodeList.isEmpty()) {
            return;
        }
        if (!baseMvValid) {
            readBaseModelView();
        }
        for (int i = 0, n = rigidNodeList.size(); i < n; i++) {
            DataNode node = rigidNodeList.get(i);
            NodeState state = nodeStates.get(node.name);
            if (state != null) {
                composedMv.set(baseMv);
                composedMv.mul(state.mat);
                loadModelView(composedMv);
            } else {
                loadModelView(baseMv);
            }
            rigidMeshScratch.clear();
            for (DataMesh mesh : node.meshes.values()) {
                if (!mesh.skin) {
                    rigidMeshScratch.add(mesh);
                }
            }
            if (rigidMeshScratch.size() > 1) {
                rigidMeshScratch.sort(COMPARE_DRAW_VAO);
            }
            for (int j = 0, m = rigidMeshScratch.size(); j < m; j++) {
                rigidMeshScratch.get(j).render();
            }
        }
        // Restore base so following skin draws / caller see correct MV.
        loadModelView(baseMv);
    }

    // 阴阳！哈哈哈 下次试试aplle和pear XD
    public void render(HashSet<String> sun, HashSet<String> moon) {
        if (geoModel == null || !geoModel.isAnimReady()) {
            return;
        }
        if (!GltfFeatureFlags.skinAnimOpt()) {
            renderLegacy(sun, moon);
            return;
        }
        boolean ownedScope = false;
        if (drawScopeDepth == 0) {
            beginDrawScope();
            ownedScope = true;
        } else if (!baseMvValid) {
            readBaseModelView();
        }
        try {
            collectDrawLists(sun, moon);
            drawSkinPass();
            drawRigidPass();
        } finally {
            if (ownedScope) {
                endDrawScope();
            }
        }
    }

    private void renderLegacy(HashSet<String> sun, HashSet<String> moon) {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        AtomicShaderCompat.rebindFillAndGunPbr();
        for (Entry<String, DataNode> e : geoModel.nodes.entrySet()) {
            if (sun != null && !sun.isEmpty() && !sun.contains(e.getKey())) {
                continue;
            }
            if (moon != null && !moon.isEmpty() && moon.contains(e.getKey())) {
                continue;
            }
            DataNode node = e.getValue();
            if (node == null || node.meshes == null || node.meshes.isEmpty()) {
                continue;
            }
            NodeState state = nodeStates.get(node.name);
            for (DataMesh mesh : node.meshes.values()) {
                if (mesh == null) {
                    continue;
                }
                GlStateManager.pushMatrix();
                if (!mesh.skin && state != null && state.mat != null) {
                    matrixBuffer.clear();
                    state.mat.get(matrixBuffer);
                    matrixBuffer.rewind();
                    GlStateManager.multMatrix(matrixBuffer);
                }
                mesh.render();
                GlStateManager.popMatrix();
            }
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
