package mchhui.hegltf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.modularwarfare.ModularWarfare;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import mchhui.hegltf.DataAnimation.DataKeyframe;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class GltfDataModel {
    private static final GltfModelReader READER = new GltfModelReader();

    private static final Comparator<DataKeyframe> COMPARATOR_ANI = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            return ((DataKeyframe) o1).time > ((DataKeyframe) o2).time ? 1 : -1;
        }
    };

    private String lastPos = "unkown";

    public HashMap<String, DataAnimation> animations = new HashMap<String, DataAnimation>();
    public HashMap<String, DataMaterial> materials = new HashMap<String, DataMaterial>();
    public HashMap<String, DataNode> nodes = new HashMap<String, DataNode>();
    public HashMap<String, DataNode> rootNodes = new HashMap<String, DataNode>();
    public ArrayList<String> joints = new ArrayList<String>();
    public ArrayList<Matrix4f> inverseBindMatrices = new ArrayList<Matrix4f>();
    public String skeleton = "";

    public boolean loaded = false;
    public volatile GltfLoadPhase phase = GltfLoadPhase.EMPTY;
    public ResourceLocation sourceLocation;
    public static int count = 0;

    private final ArrayList<PendingMesh> pendingFullMeshes = new ArrayList<>();
    private final AtomicInteger meshesPendingUpload = new AtomicInteger();
    private volatile boolean deleted;

    private static final class PendingMesh {
        final String nodeName;
        final String mateName;
        final DataMesh mesh;

        PendingMesh(String nodeName, String mateName, DataMesh mesh) {
            this.nodeName = nodeName;
            this.mateName = mateName;
            this.mesh = mesh;
        }
    }

    public boolean isAnimReady() {
        return loaded && (phase == GltfLoadPhase.ANIM_READY || phase == GltfLoadPhase.MESH_LOADING
            || phase == GltfLoadPhase.PROXY_READY || phase == GltfLoadPhase.FULL_READY);
    }

    public boolean isMeshReady() {
        return phase == GltfLoadPhase.PROXY_READY || phase == GltfLoadPhase.FULL_READY;
    }

    public boolean isFullReady() {
        return phase == GltfLoadPhase.FULL_READY;
    }

    public static GltfDataModel load(ResourceLocation loc) {
        return loadSync(loc);
    }

    public static GltfDataModel loadSync(ResourceLocation loc) {
        count++;
        GltfDataModel gltfDataModel = new GltfDataModel();
        gltfDataModel.sourceLocation = loc;
        try {
            byte[] bytes = readResourceBytes(loc);
            GltfModel model = READER.readWithoutReferences(new ByteArrayInputStream(bytes));
            gltfDataModel.parsePhaseA(model);
            gltfDataModel.loaded = true;
            gltfDataModel.phase = GltfLoadPhase.ANIM_READY;
            gltfDataModel.parsePhaseB(model, false);
            gltfDataModel.applyPendingFullMeshes(false);
            gltfDataModel.phase = GltfLoadPhase.FULL_READY;
        } catch (Throwable e) {
            ModularWarfare.LOGGER.warn("Something is wrong when loading:" + loc + " " + gltfDataModel.lastPos);
            e.printStackTrace();
            gltfDataModel.phase = GltfLoadPhase.FAILED;
        }
        return gltfDataModel;
    }

    public static void loadAsync(GltfModelHandle handle, boolean highPriority) {
        ResourceLocation loc = handle.location;
        count++;
        GltfDataModel gltfDataModel = new GltfDataModel();
        gltfDataModel.sourceLocation = loc;
        long t0 = System.currentTimeMillis();
        try {
            byte[] bytes = readResourceBytes(loc);
            GltfModel model = READER.readWithoutReferences(new ByteArrayInputStream(bytes));

            gltfDataModel.parsePhaseA(model);
            gltfDataModel.loaded = true;
            gltfDataModel.phase = GltfLoadPhase.ANIM_READY;
            handle.setDataModel(gltfDataModel);
            handle.setPhase(GltfLoadPhase.ANIM_READY);
            GltfModelManager.devLog("[GltfLazy] AnimReady {} ({}ms)", loc, System.currentTimeMillis() - t0);

            boolean proxy = GltfModelManager.isProxyEnabled();
            gltfDataModel.phase = GltfLoadPhase.MESH_LOADING;
            handle.setPhase(GltfLoadPhase.MESH_LOADING);

            if (proxy && !gltfDataModel.deleted) {
                gltfDataModel.buildProxiesOnly(model);
                gltfDataModel.phase = GltfLoadPhase.PROXY_READY;
                handle.setPhase(GltfLoadPhase.PROXY_READY);
                handle.bumpGeneration();
                gltfDataModel.queueProxyMeshUploads(highPriority);
                GltfModelManager.devLog("[GltfLazy] ProxyReady {}", loc);

                final GltfModel gltfModel = model;
                final boolean hi = highPriority;
                final long started = t0;
                GltfCpuScheduler.submit(() -> finishFullMeshPhase(handle, gltfDataModel, gltfModel, hi, started));
                return;
            }

            gltfDataModel.parsePhaseB(model, false);

            if (!gltfDataModel.deleted) {
                gltfDataModel.queueAllMeshUploads(highPriority);
                if (gltfDataModel.meshesPendingUpload.get() <= 0) {
                    gltfDataModel.phase = GltfLoadPhase.FULL_READY;
                    handle.setPhase(GltfLoadPhase.FULL_READY);
                }
            }
            GltfModelManager.devLog("[GltfLazy] MeshCPU done {} ({}ms total)", loc, System.currentTimeMillis() - t0);
        } catch (Throwable e) {
            ModularWarfare.LOGGER.warn("[GltfLazy] Failed loading:" + loc + " " + gltfDataModel.lastPos);
            e.printStackTrace();
            handle.setDataModel(gltfDataModel);
            handle.setPhase(GltfLoadPhase.FAILED);
            gltfDataModel.phase = GltfLoadPhase.FAILED;
        } finally {
            handle.setLoadQueued(false);
        }
    }

    private static void finishFullMeshPhase(GltfModelHandle handle, GltfDataModel gltfDataModel, GltfModel model,
            boolean highPriority, long t0) {
        try {
            try {
                Thread.sleep(32);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            if (gltfDataModel.deleted || handle.getDataModel() != gltfDataModel) {
                return;
            }
            gltfDataModel.parsePhaseB(model, true);
            if (!gltfDataModel.deleted && handle.getDataModel() == gltfDataModel) {
                gltfDataModel.applyPendingFullMeshes(true);
                handle.bumpGeneration();
                if (gltfDataModel.meshesPendingUpload.get() <= 0) {
                    gltfDataModel.phase = GltfLoadPhase.FULL_READY;
                    handle.setPhase(GltfLoadPhase.FULL_READY);
                }
            }
            GltfModelManager.devLog("[GltfLazy] MeshCPU done {} ({}ms total)", handle.location,
                System.currentTimeMillis() - t0);
        } catch (Throwable e) {
            ModularWarfare.LOGGER.warn("[GltfLazy] Failed mesh phase:" + handle.location + " " + gltfDataModel.lastPos);
            e.printStackTrace();
            if (handle.getDataModel() == gltfDataModel) {
                handle.setPhase(GltfLoadPhase.FAILED);
                gltfDataModel.phase = GltfLoadPhase.FAILED;
            }
        }
    }

    private static byte[] readResourceBytes(ResourceLocation loc) throws Exception {
        InputStream inputStream = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream();
        if (inputStream == null) {
            throw new RuntimeException("File not found:" + loc);
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(8192, inputStream.available()));
            byte[] buf = new byte[8192];
            int n;
            while ((n = inputStream.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            try {
                inputStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void parsePhaseA(GltfModel model) {
        lastPos = "materials";
        model.getMaterialModels().forEach((materialModel) -> {
            DataMaterial mate = new DataMaterial();
            if (materials.containsKey(materialModel.getName())) {
                throw new RuntimeException("the same material name");
            }
            materials.put(materialModel.getName(), mate);
            mate.name = materialModel.getName();
            Map map = (Map) materialModel.getExtras();
            if (map != null) {
                if (map.containsKey("isGlow")) {
                    mate.isGlow = (Boolean) map.get("isGlow");
                }
                if (map.containsKey("isTranslucent")) {
                    mate.isTranslucent = (Boolean) map.get("isTranslucent");
                }
            }
        });

        lastPos = "animations";
        model.getAnimationModels().forEach((animationModel) -> {
            animationModel.getChannels().forEach((channel) -> {
                DataAnimation animation;
                String node = channel.getNodeModel().getName();
                if (!animations.containsKey(node)) {
                    DataAnimation newAni = new DataAnimation();
                    newAni.nodeName = node;
                    animations.put(node, newAni);
                }
                animation = animations.get(node);
                ArrayList<DataKeyframe> aniChannel;
                if (channel.getPath().equals("translation")) {
                    aniChannel = animation.posChannel;
                } else if (channel.getPath().equals("rotation")) {
                    aniChannel = animation.rotChannel;
                } else if (channel.getPath().equals("scale")) {
                    aniChannel = animation.sizeChannel;
                } else {
                    throw new RuntimeException("Undefined animation channel");
                }
                ByteBuffer input = channel.getSampler().getInput().getBufferViewModel().getBufferViewData();
                ByteBuffer output = channel.getSampler().getOutput().getBufferViewModel().getBufferViewData();
                if (channel.getSampler().getInput().getCount() != channel.getSampler().getOutput().getCount()) {
                    throw new RuntimeException("Animation format wrong");
                }
                for (int i = 0; i < channel.getSampler().getInput().getCount(); i++) {
                    float time = input.getFloat();
                    DataKeyframe aniKeyframe;
                    if (channel.getPath().equals("rotation")) {
                        aniKeyframe = new DataKeyframe(time, new Vector4f(output.getFloat(), output.getFloat(),
                            output.getFloat(), output.getFloat()));
                    } else {
                        aniKeyframe = new DataKeyframe(time,
                            new Vector4f(output.getFloat(), output.getFloat(), output.getFloat(), 0));
                    }
                    aniChannel.add(aniKeyframe);
                }
            });
        });
        animations.values().forEach((ani) -> {
            ani.posChannel.sort(COMPARATOR_ANI);
            ani.rotChannel.sort(COMPARATOR_ANI);
            ani.sizeChannel.sort(COMPARATOR_ANI);
        });

        lastPos = "skin";
        if (model.getSkinModels().size() > 1) {
            throw new RuntimeException("Skin model is more than one");
        }
        model.getSkinModels().forEach((skinModel) -> {
            if (skinModel.getSkeleton() != null) {
                skeleton = skinModel.getSkeleton().getName();
            } else {
                skeleton = skinModel.getName();
            }
            skinModel.getJoints().forEach((joint) -> {
                joints.add(joint.getName());
            });
            ByteBuffer invMatsBuffer = skinModel.getInverseBindMatrices().getBufferViewModel().getBufferViewData();
            while (invMatsBuffer.hasRemaining()) {
                inverseBindMatrices.add(new Matrix4f(invMatsBuffer.getFloat(), invMatsBuffer.getFloat(),
                    invMatsBuffer.getFloat(), invMatsBuffer.getFloat(), invMatsBuffer.getFloat(),
                    invMatsBuffer.getFloat(), invMatsBuffer.getFloat(), invMatsBuffer.getFloat(),
                    invMatsBuffer.getFloat(), invMatsBuffer.getFloat(), invMatsBuffer.getFloat(),
                    invMatsBuffer.getFloat(), invMatsBuffer.getFloat(), invMatsBuffer.getFloat(),
                    invMatsBuffer.getFloat(), invMatsBuffer.getFloat()));
            }
        });

        model.getNodeModels().forEach((nodeModel) -> {
            lastPos = "nodes";
            DataNode node = new DataNode();
            node.unsafeNode = nodeModel;
            if (nodes.containsKey(nodeModel.getName())) {
                throw new RuntimeException("The same node name: \"" + nodeModel.getName() + "\"");
            }
            nodes.put(nodeModel.getName(), node);
            if (nodeModel.getParent() == null) {
                rootNodes.put(nodeModel.getName(), node);
            }
            node.name = nodeModel.getName();
            if (nodeModel.getParent() != null) {
                node.parent = nodeModel.getParent().getName();
            }
            if (nodeModel.getTranslation() != null) {
                node.pos = new Vector3f(nodeModel.getTranslation());
            }
            if (nodeModel.getRotation() != null) {
                node.rot = new Quaternionf(nodeModel.getRotation()[0], nodeModel.getRotation()[1],
                    nodeModel.getRotation()[2], nodeModel.getRotation()[3]);
            }
            if (nodeModel.getScale() != null) {
                node.size = new Vector3f(nodeModel.getScale());
            }
            nodeModel.getChildren().forEach((child) -> {
                node.childlist.add(child.getName());
            });
        });
    }

    private void buildProxiesOnly(GltfModel model) {
        for (NodeModel nodeModel : model.getNodeModels()) {
            DataNode node = nodes.get(nodeModel.getName());
            if (node == null) {
                continue;
            }
            lastPos = "proxy - " + node.name;
            nodeModel.getMeshModels().forEach((meshGModel) -> {
                meshGModel.getMeshPrimitiveModels().forEach((meshModel) -> {
                    String mateName = meshModel.getMaterialModel() == null ? "###DEFAULT###"
                        : meshModel.getMaterialModel().getName();
                    ArrayList<Vector3f> posList = new ArrayList<>();
                    readAccessorToList(
                        meshModel.getAttributes().get("POSITION").getBufferViewModel().getBufferViewData(), posList,
                        3);
                    boolean isSkining = meshModel.getAttributes().get("JOINTS_0") != null;
                    ArrayList<Vector4i> jointList = new ArrayList<>();
                    ArrayList<Vector4f> weightList = new ArrayList<>();
                    if (isSkining) {
                        readAccessorToList(
                            meshModel.getAttributes().get("JOINTS_0").getBufferViewModel().getBufferViewData(),
                            jointList, 4, meshModel.getAttributes().get("JOINTS_0").getComponentType());
                        readAccessorToList(
                            meshModel.getAttributes().get("WEIGHTS_0").getBufferViewModel().getBufferViewData(),
                            weightList, 4);
                    }
                    int joint = GltfProxyMeshBuilder.dominantJoint(jointList, weightList);
                    node.meshes.put(mateName, GltfProxyMeshBuilder.buildBox(posList, isSkining, joint));
                });
            });
        }
    }

    private void parsePhaseB(GltfModel model, boolean deferPut) {
        pendingFullMeshes.clear();
        for (NodeModel nodeModel : model.getNodeModels()) {
            DataNode node = nodes.get(nodeModel.getName());
            if (node == null) {
                continue;
            }
            lastPos = "nodes(meshes) - " + node.name;
            nodeModel.getMeshModels().forEach((meshGModel) -> {
                meshGModel.getMeshPrimitiveModels().forEach((meshModel) -> {
                    buildOneMesh(node, meshModel, deferPut);
                });
            });
        }
    }

    private void buildOneMesh(DataNode node, MeshPrimitiveModel meshModel, boolean deferPut) {
        if (meshModel.getMode() != 4) {
            throw new RuntimeException("Some meshes are not triangles");
        }
        if (meshModel.getAttributes().get("POSITION").getCount() >= Integer.MAX_VALUE) {
            throw new RuntimeException("Too many points in one mesh");
        }
        String mateName = meshModel.getMaterialModel() == null ? "###DEFAULT###"
            : meshModel.getMaterialModel().getName();

        ArrayList<Vector3f> posList = new ArrayList<>();
        ArrayList<Vector3f> normalList = new ArrayList<>();
        ArrayList<Vector2f> texList = new ArrayList<>();
        ArrayList<Vector4i> jointList = new ArrayList<>();
        ArrayList<Vector4f> weightList = new ArrayList<>();
        readAccessorToList(meshModel.getAttributes().get("POSITION").getBufferViewModel().getBufferViewData(),
            posList, 3);
        readAccessorToList(meshModel.getAttributes().get("NORMAL").getBufferViewModel().getBufferViewData(),
            normalList, 3);
        readAccessorToList(meshModel.getAttributes().get("TEXCOORD_0").getBufferViewModel().getBufferViewData(),
            texList, 2);

        boolean isSkining = false;
        if (meshModel.getAttributes().get("JOINTS_0") != null) {
            isSkining = true;
            readAccessorToList(meshModel.getAttributes().get("JOINTS_0").getBufferViewModel().getBufferViewData(),
                jointList, 4, meshModel.getAttributes().get("JOINTS_0").getComponentType());
            readAccessorToList(meshModel.getAttributes().get("WEIGHTS_0").getBufferViewModel().getBufferViewData(),
                weightList, 4);
        }

        DataMesh dataMesh = new DataMesh();
        ByteBuffer buffer = meshModel.getIndices().getBufferViewModel().getBufferViewData();
        int indicesType = meshModel.getIndices().getComponentType();
        if (isSkining) {
            dataMesh.unit = 5;
            dataMesh.geoCount = posList.size();
            dataMesh.geoBuffer = BufferUtils
                .createByteBuffer(posList.size() * (3 * 4 + 2 * 4 + 3 * 4 + 4 * 4 + 4 * 4 + 1 * 4));
            for (int i = 0; i < posList.size(); i++) {
                int point = i;
                dataMesh.geoBuffer.putFloat(posList.get(point).x);
                dataMesh.geoBuffer.putFloat(posList.get(point).y);
                dataMesh.geoBuffer.putFloat(posList.get(point).z);
                dataMesh.geoBuffer.putFloat(texList.get(point).x);
                dataMesh.geoBuffer.putFloat(texList.get(point).y);
                dataMesh.geoBuffer.putFloat(normalList.get(point).x);
                dataMesh.geoBuffer.putFloat(normalList.get(point).y);
                dataMesh.geoBuffer.putFloat(normalList.get(point).z);

                dataMesh.geoBuffer.putInt(jointList.get(point).x);
                dataMesh.geoBuffer.putInt(jointList.get(point).y);
                dataMesh.geoBuffer.putInt(jointList.get(point).z);
                dataMesh.geoBuffer.putInt(jointList.get(point).w);
                dataMesh.geoBuffer.putFloat(weightList.get(point).x);
                dataMesh.geoBuffer.putFloat(weightList.get(point).y);
                dataMesh.geoBuffer.putFloat(weightList.get(point).z);
                dataMesh.geoBuffer.putFloat(weightList.get(point).w);

                dataMesh.geoBuffer.putInt(i);
            }
            dataMesh.elementBuffer = BufferUtils.createIntBuffer(meshModel.getIndices().getCount());
            dataMesh.elementCount = meshModel.getIndices().getCount();
            while (buffer.hasRemaining()) {
                int point = getIndice(buffer, indicesType);
                dataMesh.elementBuffer.put(point);

                dataMesh.geoList.add(posList.get(point).x);
                dataMesh.geoList.add(posList.get(point).y);
                dataMesh.geoList.add(posList.get(point).z);
                dataMesh.geoList.add(texList.get(point).x);
                dataMesh.geoList.add(texList.get(point).y);
                dataMesh.geoList.add(normalList.get(point).x);
                dataMesh.geoList.add(normalList.get(point).y);
                dataMesh.geoList.add(normalList.get(point).z);
            }
        } else {
            dataMesh.unit = 3;
            while (buffer.hasRemaining()) {
                int point = getIndice(buffer, indicesType);
                dataMesh.geoList.add(posList.get(point).x);
                dataMesh.geoList.add(posList.get(point).y);
                dataMesh.geoList.add(posList.get(point).z);
                dataMesh.geoList.add(texList.get(point).x);
                dataMesh.geoList.add(texList.get(point).y);
                dataMesh.geoList.add(normalList.get(point).x);
                dataMesh.geoList.add(normalList.get(point).y);
                dataMesh.geoList.add(normalList.get(point).z);
            }
        }

        if (deferPut) {
            pendingFullMeshes.add(new PendingMesh(node.name, mateName, dataMesh));
        } else {
            node.meshes.put(mateName, dataMesh);
        }
    }

    private void applyPendingFullMeshes(boolean queueUpload) {
        if (pendingFullMeshes.isEmpty()) {
            return;
        }
        ArrayList<PendingMesh> copy = new ArrayList<>(pendingFullMeshes);
        pendingFullMeshes.clear();
        for (PendingMesh pm : copy) {
            DataNode node = nodes.get(pm.nodeName);
            if (node == null) {
                continue;
            }
            DataMesh old = node.meshes.put(pm.mateName, pm.mesh);
            if (old != null && old.proxy) {
                final DataMesh proxy = old;
                GltfGpuUploadScheduler.add("delProxy:" + pm.nodeName, 2, true, proxy::delete);
            }
            if (queueUpload) {
                meshesPendingUpload.incrementAndGet();
                pm.mesh.requestUpload(sourceLocation != null ? sourceLocation.toString() : "gltf", true,
                    this::onMeshUploadDone);
            }
        }
    }

    private void onMeshUploadDone() {
        int left = meshesPendingUpload.decrementAndGet();
        if (left <= 0 && !deleted) {
            phase = GltfLoadPhase.FULL_READY;
            if (sourceLocation != null) {
                GltfModelHandle h = GltfModelManager.get().getHandle(sourceLocation);
                if (h != null && h.getDataModel() == this) {
                    h.setPhase(GltfLoadPhase.FULL_READY);
                    h.bumpGeneration();
                    GltfModelManager.devLog("[GltfLazy] FullReady {}", sourceLocation);
                }
            }
        }
    }

    public void queueProxyMeshUploads(boolean highPriority) {
        String name = sourceLocation != null ? sourceLocation.toString() : "gltf";
        nodes.forEach((k, v) -> {
            v.meshes.forEach((n, m) -> {
                if (m.proxy) {
                    m.requestUpload(name + ":proxy", highPriority, null);
                }
            });
        });
    }

    public void queueAllMeshUploads(boolean highPriority) {
        String name = sourceLocation != null ? sourceLocation.toString() : "gltf";
        meshesPendingUpload.set(0);
        nodes.forEach((k, v) -> {
            v.meshes.forEach((n, m) -> {
                meshesPendingUpload.incrementAndGet();
                m.requestUpload(name, highPriority, this::onMeshUploadDone);
            });
        });
        if (meshesPendingUpload.get() <= 0) {
            phase = GltfLoadPhase.FULL_READY;
        }
    }

    public void dropMeshData() {
        nodes.forEach((k, v) -> {
            v.meshes.forEach((n, m) -> m.dropCpuData());
            v.meshes.clear();
        });
        pendingFullMeshes.clear();
        meshesPendingUpload.set(0);
    }

    public void deleteGpu() {
        if (nodes == null) {
            return;
        }
        nodes.forEach((k, v) -> {
            v.meshes.forEach((n, m) -> m.delete());
        });
    }

    public static int getIndice(ByteBuffer buf, int type) {
        if (type == GL11.GL_UNSIGNED_BYTE) {
            return buf.get() & 0xff;
        } else if (type == GL11.GL_UNSIGNED_SHORT) {
            return buf.getShort() & 0xffff;
        } else {
            return buf.getInt();
        }
    }

    public static void readAccessorToList(ByteBuffer buf, List list, int type) {
        readAccessorToList(buf, list, type, GL11.GL_FLOAT);
    }

    public static void readAccessorToList(ByteBuffer buf, List list, int type, int mode) {
        while (buf.hasRemaining()) {
            if (type == 2) {
                list.add(new Vector2f(buf.getFloat(), buf.getFloat()));
            } else if (type == 3) {
                list.add(new Vector3f(buf.getFloat(), buf.getFloat(), buf.getFloat()));
            } else if (type == 4) {
                if (mode == GL11.GL_UNSIGNED_BYTE || mode == GL11.GL_BYTE) {
                    list.add(new Vector4i(buf.get() & 0xff, buf.get() & 0xff, buf.get() & 0xff, buf.get() & 0xff));
                } else if (mode == GL11.GL_UNSIGNED_SHORT || mode == GL11.GL_SHORT) {
                    list.add(new Vector4i(buf.getShort() & 0xffff, buf.getShort() & 0xffff, buf.getShort() & 0xffff,
                        buf.getShort() & 0xffff));
                } else if (mode == GL11.GL_UNSIGNED_INT || mode == GL11.GL_INT) {
                    list.add(new Vector4i(buf.getInt(), buf.getInt(), buf.getInt(), buf.getInt()));
                } else if (mode == GL11.GL_FLOAT) {
                    list.add(new Vector4f(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat()));
                } else {
                    throw new Error("意料之外的type:" + mode);
                }
            } else {
                throw new Error("意料之外的unit");
            }
        }
    }

    public void delete() {
        deleted = true;
        deleteGpu();
        dropMeshData();
        loaded = false;
        phase = GltfLoadPhase.EMPTY;
        animations.clear();
        materials.clear();
        nodes.clear();
        rootNodes.clear();
        joints.clear();
        inverseBindMatrices.clear();
    }
}
