package mchhui.hegltf;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;
import org.joml.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.modularwarfare.ModularWarfare;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import io.netty.buffer.ByteBuf;
import mchhui.hegltf.DataAnimation.DataKeyframe;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class GltfDataModel {
    private static final GltfModelReader READER = new GltfModelReader();

    private static final Comparator<DataKeyframe> COMPARATOR_ANI = new Comparator() {

        @Override
        public int compare(Object o1, Object o2) {
            // TODO Auto-generated method stub
            return ((DataKeyframe)o1).time > ((DataKeyframe)o2).time ? 1 : -1;
        }

    };

    private String lastPos = "unkown";

    // node名对动画
    public HashMap<String, DataAnimation> animations = new HashMap<String, DataAnimation>();

    // 其名对其对象
    public HashMap<String, DataMaterial> materials = new HashMap<String, DataMaterial>();
    public HashMap<String, DataNode> nodes = new HashMap<String, DataNode>();
    public HashMap<String, DataNode> rootNodes = new HashMap<String, DataNode>();
    public ArrayList<String> joints = new ArrayList<String>();
    public ArrayList<Matrix4f> inverseBindMatrices = new ArrayList<Matrix4f>();
    public String skeleton = "";

    public boolean loaded = false;
    
    public static int count=0;

    public static GltfDataModel load(ResourceLocation loc) {
        count++;
        System.out.println("test a:"+count+" - "+loc);
        GltfDataModel gltfDataModel = new GltfDataModel();
        try {
            InputStream inputStream = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream();
            if (inputStream == null) {
                throw new RuntimeException("File not found:" + loc);
            }
            GltfModel model = READER.readWithoutReferences(inputStream);
            try {
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            /**
             * Materials Begining
             */
            gltfDataModel.lastPos = "materials";
            model.getMaterialModels().forEach((materialModel) -> {
                DataMaterial mate = new DataMaterial();
                if (gltfDataModel.materials.containsKey(materialModel.getName())) {
                    throw new RuntimeException("the same material name");
                }
                gltfDataModel.materials.put(materialModel.getName(), mate);
                mate.name = materialModel.getName();
                Map map = (Map)materialModel.getExtras();
                if (map != null) {
                    if (map.containsKey("isGlow")) {
                        mate.isGlow = (Boolean)map.get("isGlow");
                    }
                    if (map.containsKey("isTranslucent")) {
                        mate.isTranslucent = (Boolean)map.get("isTranslucent");
                    }
                }
            });

            /**
             * Animations begining
             */
            gltfDataModel.lastPos = "animations";
            model.getAnimationModels().forEach((animationModel) -> {
                animationModel.getChannels().forEach((channel) -> {
                    DataAnimation animation;
                    String node = channel.getNodeModel().getName();
                    if (!gltfDataModel.animations.containsKey(node)) {
                        gltfDataModel.animations.put(node, new DataAnimation());
                    }
                    animation = gltfDataModel.animations.get(node);
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
                    DataKeyframe aniKeyframe;
                    float time = 0;
                    for (int i = 0; i < channel.getSampler().getInput().getCount(); i++) {
                        time = input.getFloat();
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
            gltfDataModel.animations.values().forEach((ani) -> {
                ani.posChannel.sort(COMPARATOR_ANI);
                ani.rotChannel.sort(COMPARATOR_ANI);
                ani.sizeChannel.sort(COMPARATOR_ANI);
            });

            /**
             * Skining begining
             */
            gltfDataModel.lastPos = "skin";
            if (model.getSkinModels().size() > 1) {
                throw new RuntimeException("Skin model is more than one");
            }
            model.getSkinModels().forEach((skinModel) -> {
                if (skinModel.getSkeleton() != null) {
                    gltfDataModel.skeleton = skinModel.getSkeleton().getName();
                } else {
                    gltfDataModel.skeleton = skinModel.getName();
                }
                skinModel.getJoints().forEach((joint) -> {
                    gltfDataModel.joints.add(joint.getName());
                });
                ByteBuffer invMatsBuffer = skinModel.getInverseBindMatrices().getBufferViewModel().getBufferViewData();
                while (invMatsBuffer.hasRemaining()) {
                    gltfDataModel.inverseBindMatrices.add(new Matrix4f(invMatsBuffer.getFloat(),
                        invMatsBuffer.getFloat(), invMatsBuffer.getFloat(), invMatsBuffer.getFloat(),
                        invMatsBuffer.getFloat(), invMatsBuffer.getFloat(), invMatsBuffer.getFloat(),
                        invMatsBuffer.getFloat(), invMatsBuffer.getFloat(), invMatsBuffer.getFloat(),
                        invMatsBuffer.getFloat(), invMatsBuffer.getFloat(), invMatsBuffer.getFloat(),
                        invMatsBuffer.getFloat(), invMatsBuffer.getFloat(), invMatsBuffer.getFloat()));
                }
            });

            /**
             * Nodes begining
             */
            model.getNodeModels().forEach((nodeModel) -> {
                gltfDataModel.lastPos = "nodes";
                DataNode node = new DataNode();
                node.unsafeNode = nodeModel;
                if (gltfDataModel.nodes.containsKey(nodeModel.getName())) {
                    throw new RuntimeException("The same node name");
                }
                gltfDataModel.nodes.put(nodeModel.getName(), node);
                if (nodeModel.getParent() == null) {
                    gltfDataModel.rootNodes.put(nodeModel.getName(), node);
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

                gltfDataModel.lastPos = "nodes(meshes)";
                nodeModel.getMeshModels().forEach((meshGModel) -> {
                    meshGModel.getMeshPrimitiveModels().forEach((meshModel) -> {
                        DataMesh dataMesh = new DataMesh();
                        if (meshModel.getMode() != 4) {
                            throw new RuntimeException("Some meshes are not triangles");
                        }
                        if (meshModel.getAttributes().get("POSITION").getCount() >= Integer.MAX_VALUE) {
                            throw new RuntimeException("Too many points in one mesh");
                        }
                        if (meshModel.getMaterialModel() == null) {
                            node.meshes.put("###DEFAULT###", dataMesh);
                        } else {
                            node.meshes.put(meshModel.getMaterialModel().getName(), dataMesh);
                        }
                        ArrayList<Vector3f> posList = new ArrayList<>();
                        ArrayList<Vector3f> normalList = new ArrayList<>();
                        ArrayList<Vector2f> texList = new ArrayList<>();
                        ArrayList<Vector4i> jointList = new ArrayList<>();
                        ArrayList<Vector4f> weightList = new ArrayList<>();
                        readAccessorToList(
                            meshModel.getAttributes().get("POSITION").getBufferViewModel().getBufferViewData(), posList,
                            3);
                        readAccessorToList(
                            meshModel.getAttributes().get("NORMAL").getBufferViewModel().getBufferViewData(),
                            normalList, 3);
                        readAccessorToList(
                            meshModel.getAttributes().get("TEXCOORD_0").getBufferViewModel().getBufferViewData(),
                            texList, 2);

                        boolean isSkining = false;

                        if (meshModel.getAttributes().get("JOINTS_0") != null) {
                            isSkining = true;
                            readAccessorToList(
                                meshModel.getAttributes().get("JOINTS_0").getBufferViewModel().getBufferViewData(),
                                jointList, 4, meshModel.getAttributes().get("JOINTS_0").getComponentType());
                            readAccessorToList(
                                meshModel.getAttributes().get("WEIGHTS_0").getBufferViewModel().getBufferViewData(),
                                weightList, 4);
                        }

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
                    });
                });

            });
            gltfDataModel.loaded = true;
        } catch (Throwable e) {
            ModularWarfare.LOGGER.warn("Something is wrong when loading:" + loc);
            e.printStackTrace();
        }

        return gltfDataModel;
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
                    // 按理讲只需用0xff(1111 1111)但用多也无妨 哈哈哈
                    list.add(new Vector4i(buf.get() & 0xff, buf.get() & 0xff, buf.get() & 0xff, buf.get() & 0xff));
                } else if (mode == GL11.GL_UNSIGNED_SHORT || mode == GL11.GL_SHORT) {
                    list.add(new Vector4i(buf.getShort() & 0xffff, buf.getShort() & 0xffff, buf.getShort() & 0xffff,
                        buf.getShort() & 0xffff));
                } else if (mode == GL11.GL_UNSIGNED_INT || mode == GL11.GL_INT) {
                    // 其实没啥用 根本存不了 哈哈哈
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
        if(this.loaded) {
            this.nodes.forEach((k,v)->{
                v.meshes.forEach((n,m)->{
                    m.delete();
                });
            });
        }
    }
}
