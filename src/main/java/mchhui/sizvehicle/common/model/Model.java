package mchhui.sizvehicle.common.model;

import mchhui.hegltf.GltfRenderModel;
import mchhui.hegltf.GltfRenderModel.NodeState;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import mchhui.hegltf.GltfRenderModel.NodeAnimationMapper;
import mchhui.hegltf.DataAnimation;
import mchhui.hegltf.DataAnimation.Transform;
import mchhui.hegltf.DataNode;
import de.javagl.jgltf.model.NodeModel;
import org.joml.Matrix4f;
import java.util.HashSet;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import net.minecraft.client.renderer.GlStateManager;

public class Model {
     
     public final GltfRenderModel model;
     public boolean initCal = false;
     
     public Model(GltfRenderModel model) {
         this.model = model;
     }
     
     public void loadAnimation(Model other, boolean skin) {
         if(model == null || other == null || other.model == null) {
             return;
         }
         model.loadAnimation(other.model, skin);
     }
     
     public void updateAnimation(float time, boolean skin) {
         initCal = model.updateAnimation(time, skin);
     }
     
     public Transform findLocalTransform(String name, float time) {
         if(model == null) {
             return null;
         }
         DataNode node = model.geoModel.nodes.get(name);
         if(node == null) {
             return null;
         }
         DataAnimation ani = model.geoModel.animations.get(name);
         if(ani == null) {
             return null;
         }
         return model.geoModel.animations.get(name).findTransform(time, node.pos, node.size, node.rot);
     }
     
     public void setAnimationCalBlender(NodeAnimationBlender blender) {
         model.setNodeAnimationCalBlender(blender);
     }
     
     public void setAnimationLoadMapper(NodeAnimationMapper mapper) {
         model.setNodeAnimationLoadMapper(mapper);
     }
     
     @Deprecated
     public void updateAnimation(float time) {
         updateAnimation(time, true);
     }
     
     public boolean existPart(String part) {
         return model.geoModel.nodes.containsKey(part);
     }
     
     @Deprecated
     public NodeModel getPart(String part) {
         DataNode node = model.geoModel.nodes.get(part);
         if(node == null) {
             return null;
         }
         return node.unsafeNode;
     }
     
     public void renderPart(String part, float scale) {
         if (!initCal) {
             return;
         }
         model.renderPart(part);
     }
     
     public void renderPart(String part) {
         if (!initCal) {
             return;
         }
         model.renderPart(part);
     }
     
     public void renderPartExcept(HashSet<String> set) {
         if (!initCal) {
             return;
         }
         model.renderExcept(set);
     }
     
     public void renderPart(String[] only) {
         if (!initCal) {
             return;
         }
         model.renderOnly(only);
     }
     
     public Matrix4f getGlobalTransform(String name) {
         if (!initCal) {
             return new Matrix4f();
         }
         NodeState state = model.nodeStates.get(name);
         if(state == null) {
             return new Matrix4f();
         }
         return state.mat;
     }
}
