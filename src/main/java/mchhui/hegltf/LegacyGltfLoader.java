package mchhui.hegltf;

import com.modularwarfare.ModularWarfare;
import de.javagl.jgltf.model.GltfModel;
import moe.komi.mwprotect.IGltfLoader;
import org.joml.*;
import org.lwjgl.BufferUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

public class LegacyGltfLoader implements IGltfLoader {
    @Override
    public void loadGltf(GltfDataModel gltfDataModel, InputStream inputStream, String path) {
        try {
            if (inputStream == null) {
                throw new RuntimeException("File not found:" + path);
            }
            GltfModel model = GltfDataModel.READER.readWithoutReferences(inputStream);
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
                    ArrayList<DataAnimation.DataKeyframe> aniChannel;
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
                    DataAnimation.DataKeyframe aniKeyframe;
                    float time = 0;
                    for (int i = 0; i < channel.getSampler().getInput().getCount(); i++) {
                        time = input.getFloat();
                        if (channel.getPath().equals("rotation")) {
                            aniKeyframe = new DataAnimation.DataKeyframe(time, new Vector4f(output.getFloat(), output.getFloat(),
                                    output.getFloat(), output.getFloat()));
                        } else {
                            aniKeyframe = new DataAnimation.DataKeyframe(time,
                                    new Vector4f(output.getFloat(), output.getFloat(), output.getFloat(), 0));
                        }
                        aniChannel.add(aniKeyframe);
                    }
                });
            });
            gltfDataModel.animations.values().forEach((ani) -> {
                ani.posChannel.sort(GltfDataModel.COMPARATOR_ANI);
                ani.rotChannel.sort(GltfDataModel.COMPARATOR_ANI);
                ani.sizeChannel.sort(GltfDataModel.COMPARATOR_ANI);
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
                        GltfDataModel.readAccessorToList(
                                meshModel.getAttributes().get("POSITION").getBufferViewModel().getBufferViewData(), posList,
                                3);
                        GltfDataModel.readAccessorToList(
                                meshModel.getAttributes().get("NORMAL").getBufferViewModel().getBufferViewData(),
                                normalList, 3);
                        GltfDataModel.readAccessorToList(
                                meshModel.getAttributes().get("TEXCOORD_0").getBufferViewModel().getBufferViewData(),
                                texList, 2);

                        boolean isSkining = false;

                        if (meshModel.getAttributes().get("JOINTS_0") != null) {
                            isSkining = true;
                            GltfDataModel.readAccessorToList(
                                    meshModel.getAttributes().get("JOINTS_0").getBufferViewModel().getBufferViewData(),
                                    jointList, 4, meshModel.getAttributes().get("JOINTS_0").getComponentType());
                            GltfDataModel.readAccessorToList(
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
                                int point = GltfDataModel.getIndice(buffer, indicesType);
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
                                int point = GltfDataModel.getIndice(buffer, indicesType);
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
            ModularWarfare.LOGGER.warn("Something is wrong when loading:" + path);
            e.printStackTrace();
        }
    }
}
